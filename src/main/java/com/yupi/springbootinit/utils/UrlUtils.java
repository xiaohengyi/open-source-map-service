// 新增：UrlUtils.java
package com.yupi.springbootinit.utils;

import java.net.URI;
import java.net.URISyntaxException;

public final class UrlUtils {
    private UrlUtils() {}

    /**
     * 统一 URL 规范化：
     * - 去除末尾斜杠（path="/" → ""）
     * - host 小写
     * - 去掉默认端口（http:80 / https:443）
     * - 保留协议、host、port（非默认）、path、query、fragment（可选：此处保留 query，若业务不需要可裁剪）
     */
    public static String normalizeUrl(String raw) {
        if (raw == null) return null;
        try {
            URI u = new URI(raw.trim());
            String scheme = (u.getScheme() == null ? "http" : u.getScheme().toLowerCase());
            String host = (u.getHost() == null ? u.getAuthority() : u.getHost());
            if (host == null) host = "";
            host = host.toLowerCase();

            int port = u.getPort();
            // 去掉默认端口
            if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
                port = -1;
            }

            String path = (u.getPath() == null ? "" : u.getPath());
            // 去掉尾部斜杠（但根路径 "/" 视为空）
            if ("/".equals(path)) path = "";
            if (path.endsWith("/") && path.length() > 1) {
                path = path.replaceAll("/+$", "");
            }

            String query = u.getQuery();
            String fragment = u.getFragment();

            URI normalized = new URI(
                    scheme,
                    null,
                    host,
                    port,
                    path.isEmpty() ? null : path,
                    query,
                    fragment
            );
            return normalized.toString();
        } catch (URISyntaxException e) {
            // 解析失败时，退化为 trim 结果，避免硬失败
            return raw.trim();
        }
    }
}
