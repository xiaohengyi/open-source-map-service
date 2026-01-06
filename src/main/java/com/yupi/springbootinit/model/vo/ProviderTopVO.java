package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@ApiModel("ProviderTopVO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderTopVO {

    @ApiModelProperty("提供方名称")
    private String provider;

    @ApiModelProperty("站点数量")
    private Integer count;
}
