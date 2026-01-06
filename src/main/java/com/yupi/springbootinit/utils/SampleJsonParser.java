package com.yupi.springbootinit.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.SampleJsonEnvelope;
import com.yupi.springbootinit.common.SampleParsedResult;
import com.yupi.springbootinit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class SampleJsonParser {

    private final ObjectMapper objectMapper;

    /**
     * 支持两种粘贴：
     * 1) 直接粘贴 JSON 对象：{ ... }
     * 2) 粘贴“带转义的 JSON 字符串”：" {\"id\":...} "
     *
     * 返回：规范化后的 JSON 字符串 + 自动抽取的冗余列字段
     */
    public SampleParsedResult parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleJson 不能为空");
        }

        JsonNode root = readJsonTolerant(raw);
        SampleJsonEnvelope env = treeToEnvelope(root);

        // 抽取：优先走通用壳 content.*
        String title = firstNonBlank(
                env.getContent() != null ? trimToNull(env.getContent().getTitle()) : null,
                trimToNull(env.getTitle()) // 兼容少数源直接把 title 放根节点
        );

        String sourceUrl = firstNonBlank(
                env.getContent() != null ? trimToNull(env.getContent().getSourceUrl()) : null,
                trimToNull(env.getSourceUrl())
        );

        String language = firstNonBlank(
                trimToNull(env.getLanguage()),
                env.getContent() != null ? trimToNull(env.getContent().getLanguage()) : null
        );

        String summary = firstNonBlank(
                env.getContent() != null ? trimToNull(env.getContent().getSummary()) : null,
                trimToNull(env.getSummary())
        );

        String publishedAtStr = firstNonBlank(
                env.getContent() != null ? trimToNull(env.getContent().getPublishedAt()) : null,
                trimToNull(env.getPublishedAt())
        );
        LocalDateTime publishedAt = parseDateTimeLenient(publishedAtStr);

        // 必填校验：保证列表页稳定展示
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleJson 缺少标题字段：content.title");
        }
        if (!StringUtils.hasText(sourceUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleJson 缺少来源链接字段：content.sourceUrl");
        }
        if (!StringUtils.hasText(language)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleJson 缺少语言字段：language（或 content.language）");
        }

        // URL 统一规范化
        String normSourceUrl = UrlUtils.normalizeUrl(sourceUrl);

        // 规范化 JSON：统一存为合法 JSON 对象字符串（去掉外层转义）
        String normalizedJson = writeJson(root);

        String envSiteUrl = trimToNull(env.getSiteUrl());
        if (!StringUtils.hasText(envSiteUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleJson 缺少站点字段：siteUrl");
        }
        return SampleParsedResult.builder()
                .normalizedJson(normalizedJson)
                .siteUrl(envSiteUrl)
                .title(title.trim())
                .sourceUrl(normSourceUrl)
                .language(language.trim().toLowerCase())
                .publishedAt(publishedAt)
                .summary(summary)
                // 下面两项不落库也行，但前端结构化展示会用到（可留作将来扩展）
                .section(env.getContent() != null ? trimToNull(env.getContent().getSection()) : null)
                .author(env.getContent() != null ? trimToNull(env.getContent().getAuthor()) : null)
                .build();
    }

    private JsonNode readJsonTolerant(String raw) {
        String s = raw.trim();
        try {
            JsonNode node = objectMapper.readTree(s);

            // 如果粘贴的是 JSON 字符串："...."（内部才是真正对象），再 parse 一次
            if (node != null && node.isTextual()) {
                String inner = node.asText();
                if (StringUtils.hasText(inner)) {
                    node = objectMapper.readTree(inner);
                }
            }

            if (node == null || !node.isObject()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleJson 必须是 JSON 对象（非数组/非纯文本）");
            }
            return node;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleJson 不是合法 JSON");
        }
    }

    private SampleJsonEnvelope treeToEnvelope(JsonNode root) {
        try {
            return objectMapper.treeToValue(root, SampleJsonEnvelope.class);
        } catch (Exception e) {
            // treeToValue 理论上很少失败，失败也说明 schema 过于异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleJson 结构异常，无法解析通用壳");
        }
    }

    private String writeJson(JsonNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            // readTree 成功一般 write 不会失败，这里兜底
            return root.toString();
        }
    }

    private LocalDateTime parseDateTimeLenient(String s) {
        if (!StringUtils.hasText(s)) return null;
        String v = s.trim();

        try {
            return LocalDateTime.parse(v); // 2025-12-24T09:10:00
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(v).toLocalDateTime(); // 2025-12-24T09:10:00+08:00 / Z
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(v).atStartOfDay(); // 2025-12-24
        } catch (Exception ignored) {
        }

        // 允许为空：发布时间不是强制字段（你也可以改成强校验）
        return null;
    }

    private String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) {
            if (StringUtils.hasText(s)) return s;
        }
        return null;
    }

    private String trimToNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
