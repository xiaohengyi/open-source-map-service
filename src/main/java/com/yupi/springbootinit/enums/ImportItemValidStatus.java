package com.yupi.springbootinit.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;

import java.util.Arrays;

@ApiModel("导入明细轻校验状态")
public enum ImportItemValidStatus {
    PENDING,  // 初始
    VALID,    // 通过轻校验
    SUBMITTED, // 已提交专项
    INVALID;  // 未通过轻校验/编辑后仍不合法

    @JsonCreator
    public static ImportItemValidStatus from(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("非法明细校验状态: " + value));
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
