package com.yupi.springbootinit.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "auth.remote")
@Configuration
@Data
public class UserAuthProperties {
    /**
     * 是否启用远端核验
     */
    private boolean enabled = false;
    /**
     * 用户系统核验接口地址，例如：<a href="http://user-sys.local/api/auth/verify">...</a>
     */
    private String verifyUrl;
    /**
     * 连接/读取超时（毫秒）
     */
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 3000;
    /**
     * 远端核验失败时，是否回退使用请求头（非强安全场景可 true）
     */
    private boolean headerFallback = true;


}
