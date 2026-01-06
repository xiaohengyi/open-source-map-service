package com.yupi.springbootinit.aop;

import com.yupi.springbootinit.annotation.RequireUser;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.security.*;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;


@Aspect
@Component
@RequiredArgsConstructor
public class UserAuthAspect {

    private final HttpServletRequest request;
    private final UserSessionCache cache;
    private final UserAuthProperties props;
    private final UserDirectoryClient userDirectoryClient;

    @Around("@within(com.yupi.springbootinit.annotation.RequireUser) || @annotation(com.yupi.springbootinit.annotation.RequireUser)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // 1) 读取注解（方法优先）
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        RequireUser anno = method.getAnnotation(RequireUser.class);
        if (anno == null) {
            anno = pjp.getTarget().getClass().getAnnotation(RequireUser.class);
        }

        // 2) 从请求头读取用户信息（以 userId 为强约束；ticket 仅保留，不做本地校验）
        final String userId = header("X-User-Id");
        final String userName = header("X-User-Name");
        final String rolesHeader = header("X-User-Roles"); // 逗号分隔：ADMIN,Editor
        final String ticket = header("X-Auth-Ticket");     // 保留但不本地校验

        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少用户ID（X-User-Id）");
        }

        // 3) 先查缓存（以 uid:userId 为 Key）
        final String cacheKey = "uid:" + userId;
        UserContext ctx = cache.get(cacheKey);
        if (ctx == null) {
            // 3.1 可选：远端核验（按配置）
            if (props.isEnabled()) {
                try {
                    // 远端只“尝试”核验；ticket 可传可不传，这里只是透传；失败/空返回由 fallback 决定
                    UserContext remote = userDirectoryClient.verify(userId, ticket);
                    if (remote != null) {
                        // 兜底补齐字段
                        if (!StringUtils.hasText(remote.getUserId())) remote.setUserId(userId);
                        if (!StringUtils.hasText(remote.getUserName()))
                            remote.setUserName(StringUtils.hasText(userName) ? userName : userId);
                        if (remote.getRoles() == null || remote.getRoles().isEmpty()) {
                            remote.setRoles(parseRoles(rolesHeader));
                        }
                        if (!StringUtils.hasText(remote.getSource())) remote.setSource("REMOTE");
                        ctx = remote;
                    }
                } catch (Exception ignore) {
                    // 忽略远端异常，是否允许回退由 headerFallback 控制
                }
            }

            // 3.2 远端未得到上下文或未启用远端 —— 按 header 回退（或强制按 header）
            if (ctx == null) {
                if (!props.isEnabled() || props.isHeaderFallback()) {
                    ctx = UserContext.builder()
                            .userId(userId)
                            .userName(StringUtils.hasText(userName) ? userName : userId)
                            .roles(parseRoles(rolesHeader))
                            .ticket(ticket)          // 保留
                            .verified(false)         // 本地 header 构建不视为远端核验
                            .source("HEADER")
                            .build();
                } else {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户核验失败（远端不可用且未允许 header 回退）");
                }
            }

            // 3.3 写入缓存
            cache.put(cacheKey, ctx);
        }

        // 4) 管理员校验（如需要）
        if (anno != null && anno.admin() && !ctx.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "操作此方法需要管理员权限");
        }

        // 5) ThreadLocal 注入
        UserContextHolder.set(ctx);
        try {
            return pjp.proceed();
        } finally {
            UserContextHolder.clear();
        }
    }

    /**
     * 读取请求头的小工具方法（避免 NPE）
     */
    private String header(String name) {
        String v = request.getHeader(name);
        return v == null ? null : v.trim();
    }

    /**
     * 将逗号/空白分隔的角色串转为 List<String>，并进行去重与大小写规整
     */
    private java.util.List<String> parseRoles(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) return java.util.Collections.emptyList();
        String[] parts = rolesHeader.split("[,;\\s]+");
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (String p : parts) {
            if (StringUtils.hasText(p)) set.add(p.trim().toUpperCase());
        }
        return new java.util.ArrayList<>(set);
    }
}
