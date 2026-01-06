package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@ApiModel("词云节点")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordCloudVO {

    @ApiModelProperty("关键词")
    private String keyword;

    @ApiModelProperty("出现次数")
    private Integer count;
}
