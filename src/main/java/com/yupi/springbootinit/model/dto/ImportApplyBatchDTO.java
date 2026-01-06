package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@ApiModel("ImportApplyBatchDTO")
@Data
public class ImportApplyBatchDTO {

    @ApiModelProperty(value = "导入任务ID", required = true)
    @NotBlank
    private String jobId;

    @ApiModelProperty("指定审批的申请ID列表（不传则表示：按 jobId 全选 PENDING）")
    private List<String> applyIds;

    @ApiModelProperty("审核说明（可空）")
    private String reason;
}
