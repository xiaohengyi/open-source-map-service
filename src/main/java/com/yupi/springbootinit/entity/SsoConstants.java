package com.yupi.springbootinit.entity;

/**
 * @author:
 * @date: 2024/7/27 9:46
 * @description:
 */
public class SsoConstants {
    /**
     * 令牌自定义标识(前端Headers中获取)
     */
    public static final String TOKEN_AUTHENTICATION = "Authorization";

    /**
     * 令牌自定义标识(Cookie中获取)
     */
    public final static String TOKEN_NAME = "DJLD_TOKEN";


    /**
     * 令牌自定义标识(Cookie中获取)
     */
    public final static String TOKEN_NAME_XQTB = "XQTB_TOKEN";

    /**
     * 用户缓存前缀
     */
    public final static String CACHE_TOKEN_KEY = "login_tokens:";

    /**
     * 浏览器名称缓存前缀
     */
    public final static String CACHE_AGENT_KEY = "login_agent:";

    /**
     * 三方用户缓存前缀
     */
    public final static String THRID_CACHE_TOKEN_KEY = "third_login_tokens:";

    /**
     * 令牌有效期（天）
     */
    public final static int ALIVE_DAYS = 7;


    /**
     * 令牌有效期（分钟） 暂时设置七天，供电子所调用，防止token一直掉
     */
    public final static int ALIVE_MINUTE = 300;

    /**
     * 用户状态(禁用)
     */
    public final static String USER_ENABLE = "1";

    /**
     * 用户状态(启用)
     */
    public final static String USER_DISABLE = "0";

    public static final String USER_INFO_KEY = "djld_user_info";
}
