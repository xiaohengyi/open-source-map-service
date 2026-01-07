package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站点详情/列表返回视图对象
 * 聚合了主表 OS_SITE 的核心信息、主题名称与覆盖国家名称等可读字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("SiteVO")
public class SiteVO {

    @ApiModelProperty("站点ID（OS_SITE.ID）")
    private String id;

    @ApiModelProperty("站点名称")
    private String siteName;

    @ApiModelProperty("站点网址（协议+域名+路径）")
    private String url;

    @ApiModelProperty("主题领域冗余文本（由主题字典名称用逗号拼接，仅做展示）")
    private String theme;

    @ApiModelProperty("提供方")
    private String provider;

    @ApiModelProperty("信息渠道 / 获取手段")
    private String channel;

    @ApiModelProperty("摘要")
    private String summary;

    @ApiModelProperty("关键词冗余文本（逗号拼接，仅做展示）")
    private String keywordsText;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("数据质量：一般/重要/非常重要")
    private String dataQuality;

    @ApiModelProperty("主覆盖国家代码（ISO-3166-1 alpha-2，例：CN/US）")
    private String mainCountryCode;

    @ApiModelProperty("主覆盖国家中文名")
    private String mainCountryNameZh;

    @ApiModelProperty("主覆盖国家英文名")
    private String mainCountryNameEn;

    @ApiModelProperty("主题ID列表（来自关系表 REL_SITE_THEME）")
    private List<String> themeIds;

    @ApiModelProperty("主题名称列表（由主题字典翻译而来）")
    private List<String> themeNames;

    @ApiModelProperty("覆盖范围国家列表（带中英文名，来自 REL_SITE_SCOPE + COUNTRY_DICT）")
    private List<CountryScopeVO> scopes;

    @ApiModelProperty("创建时间（由 MetaObjectHandler 自动填充）")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间（由 MetaObjectHandler 自动填充）")
    private LocalDateTime updatedAt;
}
