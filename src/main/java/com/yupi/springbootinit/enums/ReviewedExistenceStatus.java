package com.yupi.springbootinit.enums;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 我已审核列表中，用于标识“当前主表站点”存在性/一致性的标签。
 */
@ApiModel("ReviewedExistenceStatus")
public enum ReviewedExistenceStatus {

    @ApiModelProperty("站点仍存在，且与申请时的核心字段（名称、URL）保持一致")
    NORMAL,

    @ApiModelProperty("站点仍存在，但与申请时的核心字段（名称、URL）已发生变更")
    CHANGED,

    @ApiModelProperty("站点记录已逻辑删除（或被标记为删除）")
    DELETED,

    @ApiModelProperty("主表未找到该记录（可能未落地或被物理删除）")
    MISSING
}
