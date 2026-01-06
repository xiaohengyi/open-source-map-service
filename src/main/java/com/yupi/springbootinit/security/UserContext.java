package com.yupi.springbootinit.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private String userId;
    private String userName;
    private List<String> roles;

    /**
     * 会话票据（可为空）
     */
    private String ticket;

    /**
     * 是否远端核验通过
     */
    private boolean verified;

    /**
     * 来源：HEADER / REMOTE
     */
    private String source;

    public boolean isAdmin() {
        return roles != null && roles.stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }
}
