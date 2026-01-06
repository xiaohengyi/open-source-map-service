package com.yupi.springbootinit.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireUser {
    /** 需要管理员则设为 true */
    boolean admin() default false;
}
