package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.yupi.springbootinit.injector.DmLocalDateTimeTypeHandler;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待审核申请表：仅普通用户提交；管理员审核后落入正式表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("OS_SITE_APPLY")
public class SiteApplyDO {

    @TableId(value = "ID", type = IdType.INPUT)
    private String id;

    @ApiModelProperty("生成后最终将成为 OS_SITE.ID，便于关系一致（也可在通过时再生成）")
    @TableField("TARGET_SITE_ID")
    private String targetSiteId;

    @ApiModelProperty("申请类型，分为CREATE / UPDATE / DELETE")
    @TableField("ACTION_TYPE")
    private String actionType;
    @ApiModelProperty("网站名称")
    @TableField("SITE_NAME")
    private String siteName;
    @ApiModelProperty("网站地址")
    @TableField("URL")
    private String url;
    @ApiModelProperty("信息源提供人")
    @TableField("PROVIDER")
    private String provider;
    @ApiModelProperty("信息来源渠道，共分为三种渠道，后续改为枚举类型加强校验")
    @TableField("CHANNEL")
    private String channel;
    @ApiModelProperty("摘要")
    @TableField("SUMMARY")
    private String summary;
    @ApiModelProperty("关键词，也可采用逗号拼接，后端直接存储为字符串")
    @TableField("KEYWORDS_TEXT")
    private String keywordsText;
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
    @ApiModelProperty("主要国家代码")
    @TableField("MAIN_COUNTRY_CODE")
    private String mainCountryCode;

    @ApiModelProperty("主题ID，逗号拼接；关系落库在通过时写入正式表")
    @TableField("THEME_IDS_TEXT")
    private String themeIdsText;

    @ApiModelProperty("覆盖国家列表，使用逗号拼接")
    @TableField("SCOPE_COUNTRY_CODES_TEXT")
    private String scopeCountryCodesText;
    @ApiModelProperty("申请状态，PENDING / APPROVED / REJECTED")
    @TableField("STATUS")
    private String status;
    @ApiModelProperty("审核人id")
    @TableField("SUBMIT_USER_ID")
    private String submitUserId;
    @ApiModelProperty("申请人名称")
    @TableField("SUBMIT_USER_NAME")
    private String submitUserName;
    @ApiModelProperty("审核人id")
    @TableField("REVIEW_USER_ID")
    private String reviewUserId;
    @ApiModelProperty("审核人名称")
    @TableField("REVIEW_USER_NAME")
    private String reviewUserName;
    @ApiModelProperty("审核时间")
    @TableField(value = "REVIEWED_AT",typeHandler = DmLocalDateTimeTypeHandler.class,fill = FieldFill.INSERT)
    private LocalDateTime reviewedAt;

    @ApiModelProperty("审核信息")
    @TableField("REVIEW_REASON")
    private String reviewReason;

    @ApiModelProperty("申请提交时间")
    @TableField(value = "CREATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class,fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @ApiModelProperty("申请更新时间")
    @TableField(value = "UPDATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class,fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
