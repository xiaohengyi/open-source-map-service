package com.yupi.springbootinit.model.dto;

import com.yupi.springbootinit.enums.ImportItemValidStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 导入明细分页查询 DTO
 */
@Data
@ApiModel("导入明细分页查询")
public class ImportItemQueryDTO {

    @ApiModelProperty(value = "任务ID（必填）", required = true)
    @NotBlank(message = "jobId 不能为空")
    private String jobId;

    @ApiModelProperty(value = "轻校验状态（可选：PENDING/VALID/INVALID）", example = "VALID")
    private ImportItemValidStatus validStatus;

    @ApiModelProperty(value = "审批状态（可选：NONE/PENDING/APPROVED/REJECTED）", example = "PENDING")
    private String approvalStatus;

    @ApiModelProperty(value = "分页：当前页（默认1，范围1~100）", example = "1")
    @Min(1) @Max(100)
    private Long current = 1L;

    @ApiModelProperty(value = "分页：每页大小（默认10，范围1~20）", example = "10")
    @Min(1) @Max(20)
    private Long size = 10L;
}
