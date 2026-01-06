package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel("DictThemeUpdateStatusDTO")
@Data
public class DictThemeUpdateStatusDTO {

    @ApiModelProperty(value = "主题ID", required = true)
    @NotBlank(message = "主题ID不能为空")
    private String id;

    @ApiModelProperty(value = "状态：1启用 0停用", required = true)
    @NotNull
    @Min(0) @Max(1)
    private Integer status;
}
