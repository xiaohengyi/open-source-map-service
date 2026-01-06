package com.yupi.springbootinit.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Arrays;
import java.util.EnumSet;

@ApiModel("导入任务状态")
public enum ImportJobStatus {

    @ApiModelProperty("新建，已接收文件但未完成轻校验/去重统计")
    NEW,
    @ApiModelProperty("解析处理中/异步")
    PROCESSING,

    @ApiModelProperty("可提交：已完成轻校验/去重统计")
    READY,

    @ApiModelProperty("部分提交：还有可提交项未处理完")
    PARTIAL,

    @ApiModelProperty("全部提交完成")
    DONE,

    @ApiModelProperty("提交中（可选，预留）")
    COMMITTING,

    @ApiModelProperty("失败（解析或流程级别）")
    FAILED,

    @ApiModelProperty("已作废")
    CANCELED;

    /** 供前端/接口大小写不敏感地反序列化 */
    @JsonCreator
    public static ImportJobStatus from(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("非法任务状态: " + value));
    }

    /** 返回枚举名作为 JSON 值 */
    @JsonValue
    public String toValue() {
        return this.name();
    }

    /** 是否允许提交（READY / PARTIAL） */
    public static boolean isSubmittable(ImportJobStatus s) {
        return EnumSet.of(READY, PARTIAL).contains(s);
    }
}
