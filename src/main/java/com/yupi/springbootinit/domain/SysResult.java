package com.yupi.springbootinit.domain;

import cn.hutool.http.HttpStatus;
import com.yupi.springbootinit.enums.StatusEnum;


import java.io.Serializable;
import java.util.Arrays;

/**
 * 前端统一返回对象
 *
 * @param <T>
 * @author wkl
 */
public class SysResult<T> implements Serializable {

    /**
     * 序列化
     */
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 返回说明
     */
    private String message;

    /**
     * 返回对象
     */
    private T data;

    private boolean success;

    public SysResult() {
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "SysResult{" +
                "data=" + data +
                ", message='" + message + '\'' +
                ", code=" + code +
                ", success=" + success +
                '}';
    }

    public SysResult(String message, Integer code, T data) {
        super();
        this.data = data;
        this.message = message;
        this.code = code;
    }

    public SysResult(T data) {
        this.data = data;
        this.message = StatusEnum.OK.getMsg();
        this.code = StatusEnum.OK.getCode();
    }

    public SysResult(String msg, Integer code) {
        this.message = msg;
        this.code = code;
    }

    public SysResult(T data, String msg, Integer code) {
        this.data = data;
        this.message = msg;
        this.code = code;
    }

    public SysResult(StatusEnum code) {
        this.message = code.getMsg();
        this.code = code.getCode();
    }

    /**
     * 返回成功消息
     *
     * @return 成功消息
     */
    public static <T> SysResult<T> success() {
        return SysResult.success("操作成功");
    }

    /**
     * 返回成功数据
     *
     * @return 成功消息
     */
    public static <T> SysResult<T> success(T data) {
        return SysResult.success("操作成功", data);
    }

    /**
     * 返回成功消息
     *
     * @param msg 返回内容
     * @return 成功消息
     */
    public static <T> SysResult<T> success(String msg) {
        return SysResult.success(msg, null);
    }

    /**
     * 返回成功消息
     *
     * @param msg  返回内容
     * @param data 数据对象
     * @return 成功消息
     */
    public static <T> SysResult<T> success(String msg, T data) {
        return new SysResult<T>(msg, HttpStatus.HTTP_OK, data);
    }

    /**
     * 返回错误消息
     *
     * @return
     */
    public static <T> SysResult<T> error() {
        return SysResult.error("操作失败");
    }

    /**
     * 返回错误消息
     *
     * @param msg 返回内容
     * @return 警告消息
     */
    public static <T> SysResult<T> error(String msg) {
        return SysResult.error(msg, null);
    }

    public static <T> SysResult<T> error1(String msg) {
        return SysResult.error1(msg, null);
    }

    /**
     * 返回错误消息
     *
     * @param msg  返回内容
     * @param data 数据对象
     * @return 警告消息
     */
    public static <T> SysResult<T> error(String msg, T data) {
        return new SysResult<>(msg, StatusEnum.SERVER_ERROR.getCode(), data);
    }

    public static <T> SysResult<T> error1(String msg, T data) {
        return new SysResult<>(msg, StatusEnum.USER_NOT_LOGIN.getCode(), data);
    }

    /**
     * 返回错误消息
     *
     * @param code 状态码
     * @param msg  返回内容
     * @return 警告消息
     */
    public static <T> SysResult<T> error(int code, String msg) {
        return new SysResult<T>(msg, code, null);
    }

    public static SysResult<Object> toAjax(int rows) {
        return rows > 0 ? SysResult.success() : SysResult.error();
    }

    public static SysResult<Object> toBoolAjax(boolean bool) {
        return bool ? SysResult.success() : SysResult.error();
    }

    /**
     * 修改字段自定义返回值
     *
     * @param alertMessage 消息
     * @return 结果
     */
    public static SysResult<Object> getResult(String alertMessage) {
        String[] split = alertMessage.split(",");
        String successMsg = Arrays.stream(split)
                .filter(n -> !n.contains("失败"))
                .reduce((first, second) -> first + "," + second)
                .orElse("");
        String errorMsg = Arrays.stream(split)
                .filter(n -> n.contains("失败"))
                .reduce((first, second) -> first + "," + second)
                .orElse("");
        if (!errorMsg.isEmpty()) {
            if (!successMsg.isEmpty()) {
                return SysResult.error(successMsg + "修改成功，" + errorMsg + "！");
            }
            return SysResult.error("修改" + errorMsg + "！");
        } else {
            return SysResult.success("修改成功！");
        }
    }
}
