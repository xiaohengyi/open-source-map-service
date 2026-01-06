package com.yupi.springbootinit.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;

/**
 * 该工具类实现objecjtMapper的对象的转化,
 * 优化了try-cache方法
 *
 * @author liuliu
 */
public class MapperUtil {

    /**
     * 将对象转化为json写法
     *
     * @param object
     * @return
     */
    public static String toJSON(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        return json;
    }

    /**
     * 将json数据转化为对象
     *
     * @param json
     * @param targetClass
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T toObject(String json, Class<?> targetClass) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        T object = null;
        try {
            object = (T) mapper.readValue(json, targetClass);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    public static <T> T mapToObject(String json, Class<?> targetClass) {
        ObjectMapper mapper = new ObjectMapper();
        T object = null;
        try {
            JavaType javaType = mapper.getTypeFactory().constructParametricType(Map.class, targetClass);
            object = (T) mapper.readValue(json, javaType);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        return object;
    }

    public static Map<String, String> jsonToString2Map(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        JavaType javaType = mapper.getTypeFactory().constructParametricType(Map.class, String.class, String.class);
        try {
            return mapper.readValue(json, javaType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Object> jsonToString3Map(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        JavaType javaType = mapper.getTypeFactory().constructParametricType(Map.class, String.class, String.class);
        try {
            return mapper.readValue(json, javaType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
