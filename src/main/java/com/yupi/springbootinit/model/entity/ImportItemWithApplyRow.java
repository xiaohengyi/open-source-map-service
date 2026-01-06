package com.yupi.springbootinit.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库查询行映射：导入明细 + 最新审批概要
 * 注意：这是查询结果的承载结构，非 DO/实体，不直接映射表。
 */
@Data
@ApiModel(value = "ImportItemWithApplyRow", description = "导入明细及其最新审批信息的查询行映射（非实体）")
public class ImportItemWithApplyRow {

    @ApiModelProperty(value = "导入明细ID", example = "itm_123")
    private String itemId;

    @ApiModelProperty(value = "导入任务ID", example = "job_123")
    private String jobId;

    @ApiModelProperty(value = "Excel 行号（仅用于展示排序）", example = "12")
    private Long rowNo;

    @ApiModelProperty(value = "校验状态", example = "VALID")
    private String validStatus;

    @ApiModelProperty(value = "校验提示信息")
    private String validMsg;

    @ApiModelProperty(value = "是否重复标识（0/1）", example = "0")
    private Integer dupFlag;

    @ApiModelProperty(value = "原始行 JSON（用于编辑回显）")
    private String rawJson;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "最新审批记录ID（可能为空）", example = "apply_123")
    private String approvalApplyId;

    @ApiModelProperty(value = "最新审批状态（NONE/PENDING/APPROVED/REJECTED）", example = "PENDING")
    private String approvalStatus;

    @ApiModelProperty(value = "最新审批说明（可能为空）")
    private String approvalReason;

    @ApiModelProperty(value = "最新审批时间（可能为空）")
    private LocalDateTime approvalReviewedAt;
}
