package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 覆盖范围的国家视图对象
 * 用于把站点的覆盖国家代码翻译为可读的中英文名称
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel("CountryScopeVO")
public class CountryScopeVO {

    @ApiModelProperty("站点ID（可为空：在审批阶段仅展示覆盖范围时可不填）")
    private String siteId;

    @ApiModelProperty("国家代码（ISO-3166-1 alpha-2，例：CN/US）")
    private String countryCode;

    @ApiModelProperty("国家中文名")
    private String countryNameZh;

    @ApiModelProperty("国家英文名")
    private String countryNameEn;
}
