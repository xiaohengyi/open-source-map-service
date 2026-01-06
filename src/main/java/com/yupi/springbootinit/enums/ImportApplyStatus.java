package com.yupi.springbootinit.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;

import java.util.Arrays;

@ApiModel("导入申请审批状态")
public enum ImportApplyStatus {
    PENDING,
    APPROVED,
    REJECTED;

    @JsonCreator
    public static ImportApplyStatus from(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("非法导入申请状态: " + value));
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
