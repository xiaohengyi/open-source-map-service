package com.yupi.springbootinit.model.dto;

import com.yupi.springbootinit.enums.ImportJobStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 我的导入任务分页查询 DTO
 * 仅返回当前登录用户的导入任务；可按状态过滤
 */
@Data
@ApiModel("我的导入任务分页查询")
public class ImportJobQueryDTO {

    @ApiModelProperty(value = "任务状态（可选：NEW/READY/PARTIAL/DONE/FAILED/CANCELED）", example = "READY")
    private ImportJobStatus status;

    @ApiModelProperty(value = "分页：当前页（默认1，范围1~100）", example = "1")
    @Min(value = 1, message = "分页页码最小值为1")
    @Max(value = 100, message = "分页页码最大值为100")
    private Long current = 1L;

    @ApiModelProperty(value = "分页：每页大小（默认10，范围1~20）", example = "10")
    @Min(value = 1, message = "每页最少展示数据条目为1条")
    @Max(value = 20, message = "每页最多展示数据条目为20条")
    private Long size = 10L;
}
