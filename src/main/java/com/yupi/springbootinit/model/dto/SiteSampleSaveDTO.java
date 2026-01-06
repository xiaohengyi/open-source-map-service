package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SiteSampleSaveDTO {

    @ApiModelProperty("样例ID（为空=新增；不为空=编辑该样例）")
    private String id;

    @ApiModelProperty("数据源ID（OS_SITE.ID）")
    @NotBlank(message = "siteId 不能为空")
    private String siteId;

    @ApiModelProperty("完整样例JSON（通用壳）；支持粘贴 JSON 对象，或粘贴带转义的 JSON 字符串")
    @NotBlank(message = "sampleJson 不能为空")
    private String sampleJson;
}
