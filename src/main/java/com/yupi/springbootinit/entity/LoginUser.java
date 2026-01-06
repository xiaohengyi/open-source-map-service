package com.yupi.springbootinit.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息
 *
 * @author dev_shd
 */
@Data
public class LoginUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识
     */
    private String token;
    /**
     * 用户标识
     */
    private Long ybbz;
    /**
     * 账号
     */
    private String zh;
    /**
     * 姓名
     */
    private String xm;
    /**
     * 用户级别
     */
    private String yhjb;
}
