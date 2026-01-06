package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@ApiModel("ChannelTopVO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelTopVO {

    @ApiModelProperty("渠道名称")
    private String channel;

    @ApiModelProperty("站点数量")
    private Integer count;
}
