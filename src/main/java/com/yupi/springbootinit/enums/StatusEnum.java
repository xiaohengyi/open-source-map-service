package com.yupi.springbootinit.enums;


/**
 * 状态码枚举类
 */


public enum StatusEnum {
    /**
     * 成功
     */
    OK(200, "成功"),
    /**
     * 服务器成功处理请求，但没有任何返回
     */
    RESET_CONTENTY(205, "服务器成功处理请求，但没有任何返回"),
    /**
     * 请求参数错误
     */
    BAD_REQUEST(400, "请求参数错误"),
    /**
     * 运行时异常状态码(异常信息由RuntimeException提供)
     */
    RUNTIME(401, ""),
    /**
     * 请求资源未找到
     */
    NOT_FOUND(404, "请求资源未找到"),
    /**
     * 服务器异常
     */
    SERVER_ERROR(500, "服务器异常"),
    /**
     * 未找到调用方法
     */
    NOT_FOUND_METHOD(501, "未找到调用方法"),
    /**
     * 未找到调用方法
     */
    NOT_FOUND_SPRING_BEAN(502, "Spring容器中未找到对象"),
    /**
     * 参数校验异常
     */
    PARAM_NOTVALID_ERROR(601, "参数校验失败"),
    /**
     * 未登陆
     */
    USER_NOT_LOGIN(602, "未登陆"),

    /**
     * 该方法正在执行,请稍后重试
     */
    PERFORMQ_ING(603, "该方法正在执行,请稍后重试!"),

    /**
     * 登录状态已过期
     */
    LOGIN_EXPIRED(604, "登录状态已过期"),
    /**
     * 授权失败
     */
    AUTH_ERROR(605, "登录状态已过期");

    private int code;
    private String msg;

    StatusEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public boolean equalsIgnoreCase(Integer code) {
        return code != null && code.equals(this.code);
    }
}
