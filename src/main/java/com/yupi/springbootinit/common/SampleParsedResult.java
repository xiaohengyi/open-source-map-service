package com.yupi.springbootinit.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleParsedResult {

    /**
     * 规范化后的 JSON 字符串（一定是 JSON 对象字符串，不是带转义的字符串）
     */
    private String normalizedJson;
    private String siteUrl;
    /**
     * 自动抽取用于 OS_SITE_SAMPLE 冗余列
     */
    private String title;
    private String sourceUrl;
    private String language;
    private LocalDateTime publishedAt;
    private String summary;

    /**
     * 先预留（不一定落库），后续你想加字段/用于前端结构化展示可直接用
     */
    private String section;
    private String author;
}
