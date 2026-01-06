package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/** 单任务审批概览：Job 基本信息 + 行数/统计 */
@Data
public class ImportJobOverviewVO {
    @ApiModelProperty("任务ID")
    private String id;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件大小")
    private Long fileSize;

    @ApiModelProperty("任务状态")
    private String status;

    @ApiModelProperty("总行")
    private Integer rowsTotal;

    @ApiModelProperty("可提交（VALID 非重复）估算，保持与现有DO含义")
    private Integer rowsReady;

    @ApiModelProperty("跳过")
    private Integer rowsSkipped;

    @ApiModelProperty("重复")
    private Integer rowsDup;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;

    // 统计字段
    @ApiModelProperty("待审申请数")
    private Integer appliesPendingCount;

    @ApiModelProperty("已过申请数")
    private Integer appliesApprovedCount;

    @ApiModelProperty("已驳回申请数")
    private Integer appliesRejectedCount;

    @ApiModelProperty("VALID 明细数")
    private Integer itemsValidCount;

    @ApiModelProperty("INVALID 明细数")
    private Integer itemsInvalidCount;

    @ApiModelProperty("SUBMITTED 明细数")
    private Integer itemsSubmittedCount;

    @ApiModelProperty("是否包含待审申请")
    private Boolean hasPending;
}
