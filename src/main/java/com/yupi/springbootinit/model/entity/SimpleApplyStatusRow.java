package com.yupi.springbootinit.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 每个导入明细的“最新审批状态”行（仅含必要字段）
 */
@Data
@ApiModel(value = "SimpleApplyStatusRow", description = "导入明细的最新审批状态概要")
public class SimpleApplyStatusRow {

    @ApiModelProperty(value = "导入明细ID", example = "itm_123")
    private String importItemId;

    @ApiModelProperty(value = "最新审批记录ID", example = "apply_123")
    private String applyId;

    @ApiModelProperty(value = "最新审批状态（PENDING/APPROVED/REJECTED）", example = "PENDING")
    private String approvalStatus;

    @ApiModelProperty(value = "最新审批说明（可能为空）")
    private String approvalReason;

    @ApiModelProperty(value = "最新审批时间（可能为空）")
    private LocalDateTime approvalReviewedAt;
}
