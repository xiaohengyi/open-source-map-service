package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yupi.springbootinit.injector.DmLocalDateTimeTypeHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("SITE_IMPORT_APPLY")
@ApiModel("导入审批申请")
public class SiteImportApplyDO {

    @ApiModelProperty("申请ID（String-UUID）")
    @TableId(type = IdType.INPUT)
    private String id;

    @ApiModelProperty("CREATE/UPDATE")
    private String actionType;

    @ApiModelProperty("网站名称")
    private String siteName;

    @ApiModelProperty("网站地址")
    private String url;

    @ApiModelProperty("主覆盖国家代码")
    private String mainCountryCode;

    @ApiModelProperty("主题ID串（逗号分隔）")
    private String themeIdsText;

    @ApiModelProperty("覆盖国家串（逗号分隔）")
    private String scopesText;

    @ApiModelProperty("提供方")
    private String provider;

    @ApiModelProperty("渠道")
    private String channel;

    @ApiModelProperty("摘要")
    private String summary;

    @ApiModelProperty("关键词（逗号分隔）")
    private String keywordsText;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("状态：PENDING/APPROVED/REJECTED")
    private String status;

    @ApiModelProperty("提交人ID")
    private String submitUserId;

    @ApiModelProperty("提交人")
    private String submitUserName;

    @ApiModelProperty("审核人ID")
    private String reviewUserId;

    @ApiModelProperty("审核人")
    private String reviewUserName;

    @ApiModelProperty("审核说明")
    private String reviewReason;

    @ApiModelProperty("导入任务ID")
    private String importJobId;

    @ApiModelProperty("导入明细ID")
    private String importItemId;

    @ApiModelProperty("审批通过后落主表的站点ID")
    private String targetSiteId;

    @ApiModelProperty("创建时间")
    @TableField(value = "CREATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class)
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    @TableField(value = "UPDATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class)
    private LocalDateTime updatedAt;

    @ApiModelProperty("审核时间")
    @TableField(value = "REVIEWED_AT", typeHandler = DmLocalDateTimeTypeHandler.class)
    private LocalDateTime reviewedAt;
}
