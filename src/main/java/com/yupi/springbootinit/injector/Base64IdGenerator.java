package com.yupi.springbootinit.injector;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class Base64IdGenerator implements IdentifierGenerator {

    @Override
    public String nextUUID(Object entity) {
        return generateBase64Id();
    }

    @Override
    public Number nextId(Object entity) {
        // 返回默认值避免空指针
        return ThreadLocalRandom.current().nextLong();
    }


    private String generateBase64Id() {
        //            DataSource ds = (DataSource) entity;
        //            return Base64.getUrlEncoder()
        //                    .encodeToString(ds.getMediaName().getBytes(StandardCharsets.UTF_8));
        return UUID.randomUUID().toString().replace("-", "");
    }
}