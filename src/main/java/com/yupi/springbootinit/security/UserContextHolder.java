package com.yupi.springbootinit.security;

public class UserContextHolder {
    private static final ThreadLocal<UserContext> TL = new ThreadLocal<>();

    public static void set(UserContext ctx) { TL.set(ctx); }
    public static UserContext get() { return TL.get(); }
    public static void clear() { TL.remove(); }
}
