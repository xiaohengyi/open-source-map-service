package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.yupi.springbootinit.injector.DmLocalDateTimeTypeHandler;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("OS_SITE")
public class OsSiteDO {

    @ApiModelProperty("业务唯一ID（Base64/UUID）")
    @TableId(value = "ID", type = IdType.INPUT)
    private String id;

    @ApiModelProperty("网站名称")
    @TableField("SITE_NAME")
    private String siteName;

    @ApiModelProperty("网站地址（协议+域名+路径）")
    @TableField("URL")
    private String url;

    @ApiModelProperty("主题领域（冗余文本，逗号拼接）")
    @TableField("THEME")
    private String theme;

    @ApiModelProperty("提供方")
    @TableField("PROVIDER")
    private String provider;

    @ApiModelProperty("信息渠道/获取手段")
    @TableField("CHANNEL")
    private String channel;

    @ApiModelProperty("摘要")
    @TableField("SUMMARY")
    private String summary;

    @ApiModelProperty("关键词（冗余文本，逗号拼接）")
    @TableField("KEYWORDS_TEXT")
    private String keywordsText;

    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty("主覆盖国家代码（ISO-3166-1 alpha-2，例：CN/US/GB；真实范围见关系表）")
    @TableField("MAIN_COUNTRY_CODE")
    private String mainCountryCode;
    @ApiModelProperty("创建人用户ID")
    @TableField("CREATED_BY")
    private String createdBy;
    @ApiModelProperty("更新人用户ID")
    @TableField("UPDATED_BY")
    private String updatedBy;

    @ApiModelProperty("创建时间")
    @TableField(value = "CREATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    @TableField(value = "UPDATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @ApiModelProperty("逻辑删除：0=未删 1=已删")
    @TableLogic(value = "0", delval = "1")
    @TableField("IS_DELETE")
    private Integer isDelete;
}
