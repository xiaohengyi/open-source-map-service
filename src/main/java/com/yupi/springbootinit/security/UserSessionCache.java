package com.yupi.springbootinit.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserSessionCache {
    private static class Entry {
        UserContext ctx;
        long expireAt;
    }
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private static final long TTL_MILLIS = 3L * 60 * 60 * 1000; // 3h

    public UserContext get(String key) {
        Entry e = cache.get(key);
        if (e == null) return null;
        if (e.expireAt < System.currentTimeMillis()) {
            cache.remove(key);
            return null;
        }
        return e.ctx;
    }

    public void put(String key, UserContext ctx) {
        Entry e = new Entry();
        e.ctx = ctx;
        e.expireAt = System.currentTimeMillis() + TTL_MILLIS;
        cache.put(key, e);
    }
}
