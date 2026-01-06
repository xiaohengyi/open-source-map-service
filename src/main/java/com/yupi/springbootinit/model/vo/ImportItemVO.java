package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("导入明细VO")
public class ImportItemVO {

    @ApiModelProperty("明细ID")
    private String id;

    @ApiModelProperty("任务ID")
    private String jobId;

    @ApiModelProperty("行号")
    private Integer rowNo;

    @ApiModelProperty("原始行 JSON")
    private String rawJson;

    @ApiModelProperty("校验状态：PENDING/VALID/INVALID")
    private String validStatus;

    @ApiModelProperty("校验信息")
    private String validMsg;

    @ApiModelProperty("是否重复：0/1")
    private Integer dupFlag;

    @ApiModelProperty("审批态：NONE（无申请）/PENDING/APPROVED/REJECTED")
    private String approvalStatus;

    @ApiModelProperty("审批意见/原因（仅 APPROVED/REJECTED 时有值；也可在 PENDING 时为空）")
    private String approvalReason;

    @ApiModelProperty("审批完成时间（REVIEWED_AT；PENDING 为 null）")
    private LocalDateTime approvalReviewedAt;

    @ApiModelProperty("应的最新审批记录ID（前端可跳详情）")
    private String approvalApplyId;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
