package com.yupi.springbootinit.entity;

/**
 * @author dev_shd
 */
public class SecurityConstants {

    /**
     * 令牌自定义标识
     */
    public static final String AUTHENTICATION = "Authorization";

    /**
     * 令牌前缀
     */
    public static final String PREFIX = "Bearer ";

    /**
     * 令牌秘钥
     */
    public final static String SECRET = "abqeyza1rryedzs8h9pw7nopqrstuvwxyz";

    /**
     * 令牌自定义标识(Cookie中获取)
     */
    public final static String TOKEN_NAME = "SJTB_TOKEN";

    /**
     * 用户ID字段
     */
    public static final String DETAILS_YHBZ = "yhbz";
    /**
     * 账号
     */
    public static final String DETAILS_ZH = "zh";
    /**
     * 姓名
     */
    public static final String DETAILS_XM = "xm";
    /**
     * 用户级别
     */
    public static final String DETAILS_YHJB = "yhjb";

    /**
     * 登录用户
     */
    public static final String LOGIN_USER = "login_user";

    /**
     * 令牌有效期（天）
     */
    public final static int ALIVE_DAYS = 7;

    /**
     * 令牌有效期（分钟）
     */
    public final static int ALIVE_MINUTE = 180;
}
