package com.yupi.springbootinit.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class LoginInfo {
    @NotNull(message = "username不能为空！")
    private String username;

    @NotNull(message = "password不能为空！")
    private String password;

    private boolean md5 = false;

}
