package com.yupi.springbootinit.utils;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 数据质量字段的统一校验与规范化工具。
 */
public final class DataQualityUtils {

    private DataQualityUtils() {
    }

    public static final String QUALITY_NORMAL = "一般";
    public static final String QUALITY_IMPORTANT = "重要";
    public static final String QUALITY_CRITICAL = "非常重要";

    private static final List<String> ALLOWED = Collections.unmodifiableList(
            Arrays.asList(QUALITY_NORMAL, QUALITY_IMPORTANT, QUALITY_CRITICAL)
    );

    /**
     * 归一化入参：
     * - 空/空白 => 默认“{@link #QUALITY_NORMAL}”
     * - 合法值 => 原样返回
     * - 非法值 => 返回 null
     */
    public static String normalizeOrDefault(String value) {
        String v = StringUtils.trimWhitespace(Objects.toString(value, ""));
        if (!StringUtils.hasText(v)) {
            return QUALITY_NORMAL;
        }
        return ALLOWED.contains(v) ? v : null;
    }

    /**
     * 校验并返回规范化结果，非法值抛出业务异常。
     */
    public static String requireValidOrDefault(String value) {
        String norm = normalizeOrDefault(value);
        if (norm == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据质量仅支持：一般/重要/非常重要");
        }
        return norm;
    }
}
