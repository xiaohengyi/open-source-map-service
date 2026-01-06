package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 主题领域分组统计
 */
@Data
@ApiModel("主题领域聚合统计")
public class ThemeStatVO {

    @ApiModelProperty("主题ID")
    private String themeId;

    @ApiModelProperty("主题名称")
    private String themeName;

    @ApiModelProperty("该主题下站点数量（仅统计未删除站点）")
    private Long siteCount;
}
