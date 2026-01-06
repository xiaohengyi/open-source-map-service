package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@ApiModel("DictThemeSaveDTO")
@Data
public class DictThemeSaveDTO {

    @ApiModelProperty(value = "主题ID（编辑时必传；新增可空）", example = "thm_xxx_001")
    private String id;

    @ApiModelProperty(value = "主题名称", required = true, example = "金融")
    @NotBlank(message = "主题名称不能为空")
    @Size(max = 128, message = "主题名称过长")
    private String name;

    @ApiModelProperty(value = "主题编码（可空；唯一）", example = "finance")
    @Size(max = 64, message = "主题编码过长")
    @Pattern(regexp = "^[A-Za-z0-9_\\-]*$", message = "主题编码仅允许字母数字下划线和中划线")
    private String code;

    @ApiModelProperty(value = "描述（可空）")
    @Size(max = 512, message = "描述过长")
    private String description;

    @ApiModelProperty(value = "状态：1启用 0停用，默认启用", example = "1")
    private Integer status;
}
