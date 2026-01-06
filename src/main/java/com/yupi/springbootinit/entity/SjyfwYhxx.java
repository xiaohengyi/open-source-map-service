package com.yupi.springbootinit.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@Data
public class SjyfwYhxx implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户标志
     */
    private Integer yhbz;

    /**
     * 账号
     */
    private String zh;

    /**
     * 姓名
     */
    private String xm;

    /**
     * 证件号码
     */
    private String zjhm;

    /**
     * 单位
     */
    private String dw;

    /**
     * 职务
     */
    private String zw;

    /**
     * 密码，需经过MD5加密
     */
    private String mm;

    /**
     * 是否启用
     */
    private String dlzt;

    /**
     * 备注
     */
    private String bz;

    /**
     * 排序
     */
    private Integer px;

    /**
     * 上次登录时间
     */
    private String lastlogintime;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 办公电话
     */
    private String tel;

    /**
     * 办公电话1
     */
    private String tel1;

    /**
     * 办公电话2
     */
    private String tel2;

    /**
     * 姓名简称
     */
    private String xmjc;

    /**
     * 权限
     */
    private String qx;

    /**
     * 数据权限
     */
    private Set<String> sjqxs;

    /**
     * 创建人用户标志
     */
    private Integer cjryhbz;

    /**
     * 数据时间
     */
    //用了这个JSONfied后入库没有新增时间了！！！！！！谁再改这里联系下刘柳
    //@JSONField(name = "sjsj")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date yhcjsj;

    /**
     * 用户级别（0：超级管理员 1：管理员 2：一般用户 3：部门管理员）
     */
    private String yhjb;

    /**
     * 用户IP
     */
    private String ip;

    /**
     * 密级内码
     */
    private String mjnm;

    //=======================
    private Boolean on;
    private String bmmc;


    /**
     * 是否复制用户权限标识
     */
    private Boolean cpYhJs;


    /**
     * 旧密码
     */
    private String oldPwd;

    /**
     * 第二次新密码
     */
    private String smm;

    /**
     * 新职务 张云飞
     */
    private String xzw;

    public Boolean getCpYhJs() {
        return cpYhJs;
    }

    public void setCpYhJs(Boolean cpYhJs) {
        this.cpYhJs = cpYhJs;
    }

    public SjyfwYhxx() {
    }

    public SjyfwYhxx(Integer yhbz) {
        this.yhbz = yhbz;
    }

    public SjyfwYhxx(String xm) {
        this.xm = xm;
    }


    public void isOff() {
        this.on = OnEnum.ONE.endsWithIgnoreCase(this.getDlzt());
    }

    /**
     * 是否启用条件转换
     */
    public void isSfQy() {
        if (this.on == null) {
            return;
        }
        this.dlzt = this.on ? OnEnum.ONE.getCode() : OnEnum.ZERO.getCode();
    }

    public enum OnEnum {
        ZERO("0"),
        /**
         * 启用
         */
        ONE("1");

        private String code;

        OnEnum(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public boolean endsWithIgnoreCase(String code) {
            return this.code.equals(code);
        }
    }

    /**
     * 用户级别
     */
    @Getter
    public enum Level {
        ZER("0", "超级管理员"),
        ONE("1", "普通管理员"),
        TWO("2", "一般用户"),
        THR("3", "部门管理员");

        private String code;
        private String name;

        Level(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public static Level getCodeLevel(String code) {
            for (Level value : values()) {
                if (value.equalsIgnoreCase(code)) {
                    return value;
                }
            }
            return null;
        }

        public static String getCodeName(String code) {
            for (Level value : values()) {
                if (value.equalsIgnoreCase(code)) {
                    return value.getName();
                }
            }
            return null;
        }

        /**
         * 获取代码对应的枚举类
         *
         * @param code
         * @return
         */
        public Boolean equalsIgnoreCase(String code) {
            return this.getCode().equals(code);
        }
    }
}
