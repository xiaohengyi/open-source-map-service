package com.yupi.springbootinit.utils;

import org.springframework.util.StringUtils;

public final class SqlLikeUtils {

    /** 选一个不太会被用户输入到的转义字符，避免 Java/SQL 里写反斜杠太绕 */
    public static final char ESC = '~';

    private SqlLikeUtils() {}

    /** 把用户输入当“字面量”处理：转义 %、_、以及 ESC 本身 */
    public static String escapeLiteral(String input) {
        if (!StringUtils.hasText(input)) return null;
        String s = input.trim();
        // 先转义 ESC 本身
        s = s.replace(String.valueOf(ESC), String.valueOf(ESC) + ESC);
        // 转义 LIKE 通配符
        s = s.replace("%", String.valueOf(ESC) + "%");
        s = s.replace("_", String.valueOf(ESC) + "_");
        return s;
    }

    /** contains 模糊匹配：%xxx%（已转义） */
    public static String likeContainsLiteral(String input) {
        String s = escapeLiteral(input);
        return (s == null) ? null : "%" + s + "%";
    }
}
