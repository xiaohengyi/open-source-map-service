package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@ApiModel("导入明细提交为导入申请")
public class ImportCommitDTO {

    @ApiModelProperty(value = "任务ID", required = true)
    @NotBlank
    private String jobId;

    @ApiModelProperty("提交模式：ALL_VALID 表示提交该任务下全部 VALID 且未提交过的；否则仅提交 itemIds")
    private String mode; // ALL_VALID / null

    @ApiModelProperty("指定提交的明细ID列表（可空）")
    private List<String> itemIds;

    @ApiModelProperty("动作类型（CREATE/UPDATE），不填默认 CREATE")
    private String actionType;

    @ApiModelProperty("是否在提交后自动审批（管理员直通）。前端可不传，服务端也可基于角色默认开启。")
    private Boolean autoApprove = false;
}
