package com.yupi.springbootinit.security;

public interface UserDirectoryClient {
    /**
     * 通过用户系统核验并拉取用户上下文。
     * @param userId  请求头传来的用户ID（可选）
     * @param ticket  会话票据/令牌/SessionId（从请求头或Cookie传过来）
     * @return 核验通过则返回上下文；无法核验返回 null
     * @throws Exception 网络/解析异常
     */
    UserContext verify(String userId, String ticket) throws Exception;
}
