package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待审核申请的视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("SiteApplyVO")
public class SiteApplyVO {

    @ApiModelProperty("申请单ID")
    private String id;
    @ApiModelProperty("涉及的站点ID（编辑为原站点ID；新增为预生成ID）")
    private String targetSiteId;
    @ApiModelProperty("动作类型：CREATE / UPDATE")
    private String actionType;
    @ApiModelProperty("站点名称")
    private String siteName;
    @ApiModelProperty("站点网址")
    private String url;
    @ApiModelProperty("提供方")
    private String provider;
    @ApiModelProperty("信息渠道")
    private String channel;
    @ApiModelProperty("摘要")
    private String summary;
    @ApiModelProperty("关键词文本（逗号分隔）")
    private String keywordsText;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("数据质量：一般/重要/非常重要")
    private String dataQuality;
    @ApiModelProperty("主覆盖国家代码（ISO2）")
    private String mainCountryCode;
    @ApiModelProperty("主覆盖国家中文名")
    private String mainCountryNameZh;
    @ApiModelProperty("主覆盖国家英文名")
    private String mainCountryNameEn;
    @ApiModelProperty("主题ID列表")
    private List<String> themeIds;
    @ApiModelProperty("主题名称列表")
    private List<String> themeNames;
    @ApiModelProperty("覆盖范围国家（含中英文名）")
    private List<CountryScopeVO> scopes;
    @ApiModelProperty("申请状态：PENDING / APPROVED / REJECTED")
    private String status;
    @ApiModelProperty("提交人ID")
    private String submitUserId;
    @ApiModelProperty("提交人姓名")
    private String submitUserName;
    @ApiModelProperty("审核人ID")
    private String reviewUserId;
    @ApiModelProperty("提交人姓名")
    private String reviewUserName;
    @ApiModelProperty("审核说明")
    private String reviewReason;
    @ApiModelProperty("审核时间")
    private LocalDateTime createdAt;
    @ApiModelProperty("提交时间")
    private LocalDateTime reviewedAt;
    @ApiModelProperty("最后更新时间")
    private LocalDateTime updatedAt;
}
