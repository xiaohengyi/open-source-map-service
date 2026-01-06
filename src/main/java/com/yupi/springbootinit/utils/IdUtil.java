package com.yupi.springbootinit.utils;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/** URL-safe Base64 UUID 生成 */
public final class IdUtil {
    private IdUtil(){}

    public static String urlSafeUuid() {
        UUID u = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        String b64 = Base64.getEncoder().encodeToString(bb.array());
        return b64.replace("=", "").replace('/', '_').replace('+', '-');
    }
}
