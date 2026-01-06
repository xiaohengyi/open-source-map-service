package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@ApiModel(value = "CountryDictDTO", description = "国家字典新增/编辑 DTO")
@Data
public class CountryDictDTO {

    @ApiModelProperty(value = "国家代码（ISO-3166-1 alpha-2）", required = true, example = "CN")
    @NotBlank(message = "国家代码不能为空")
    @Size(min = 2, max = 2, message = "国家代码必须为2位（alpha-2）")
    @Pattern(regexp = "^[A-Z]{2}$", message = "国家代码必须为2位大写字母")
    private String code;

    @ApiModelProperty(value = "国家中文名", required = true, example = "中国")
    @NotBlank(message = "中文名不能为空")
    private String nameZh;

    @ApiModelProperty(value = "国家英文名", required = true, example = "China")
    @NotBlank(message = "英文名不能为空")
    private String nameEn;
}
