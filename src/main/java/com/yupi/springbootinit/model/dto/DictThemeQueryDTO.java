package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("DictThemeQueryDTO")
@Data
public class DictThemeQueryDTO {

    @ApiModelProperty(value = "关键字（对 name/code/description 模糊）")
    private String keyword;

    @ApiModelProperty(value = "状态：1启用 0停用（可空表示全部）")
    private Integer status;

    @ApiModelProperty(value = "当前页，从1开始", example = "1")
    private Long current = 1L;

    @ApiModelProperty(value = "每页大小", example = "20")
    private Long size = 20L;
}
