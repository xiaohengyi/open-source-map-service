package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SiteSampleDeleteDTO {

    @ApiModelProperty("样例ID")
    @NotBlank(message = "id 不能为空")
    private String id;
}
