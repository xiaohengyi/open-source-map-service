package com.yupi.springbootinit.model.vo;

import com.yupi.springbootinit.enums.ReviewedExistenceStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * “我已发布（我提交并被通过）”项的展示模型：
 * - 左侧（申请快照）：字段名与 SiteApplyDO 对齐，便于前端复用
 * - 右侧（当前主表对照）：当前站点关键信息 + 存在性标签
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel("MyApprovedSiteItemVO")
public class MyApprovedSiteItemVO {

    // -------------------- 申请侧（历史快照，与 SiteApplyDO 字段一致） --------------------

    @ApiModelProperty("申请单ID")
    private String id;

    @ApiModelProperty("目标站点ID（审批通过后落地的站点主键或预分配主键）")
    private String targetSiteId;

    @ApiModelProperty("动作类型：CREATE / UPDATE")
    private String actionType;

    @ApiModelProperty("站点名称（申请时快照）")
    private String siteName;

    @ApiModelProperty("站点URL（申请时快照）")
    private String url;

    @ApiModelProperty("提供方（申请时快照）")
    private String provider;

    @ApiModelProperty("渠道（申请时快照）")
    private String channel;

    @ApiModelProperty("摘要（申请时快照）")
    private String summary;

    @ApiModelProperty("关键词文本（申请时快照）")
    private String keywordsText;

    @ApiModelProperty("备注（申请时快照）")
    private String remark;

    @ApiModelProperty("主覆盖国家代码（ISO-3166-1 alpha-2 或 ALL）（申请时快照）")
    private String mainCountryCode;

    @ApiModelProperty("主题ID列表（申请时快照，已解析）")
    private List<String> themeIds;

    @ApiModelProperty("主题名称列表（申请时快照）")
    private List<String> themeNames;

    @ApiModelProperty("覆盖范围国家列表（申请时快照，含中英文名）")
    private List<CountryScopeVO> scopes;

    @ApiModelProperty("申请状态（本列表恒为 APPROVED）")
    private String status;

    @ApiModelProperty("申请提交人用户ID")
    private String submitUserId;

    @ApiModelProperty("申请提交人用户名")
    private String submitUserName;

    @ApiModelProperty("审核人用户ID")
    private String reviewUserId;

    @ApiModelProperty("审核人用户名")
    private String reviewUserName;

    @ApiModelProperty("审核意见/驳回原因")
    private String reviewReason;

    @ApiModelProperty("申请创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("申请更新时间")
    private LocalDateTime updatedAt;

    @ApiModelProperty("审核时间（通过时间）")
    private LocalDateTime reviewedAt;

    @ApiModelProperty("主覆盖国家中文名（快照辅助展示）")
    private String mainCountryNameZh;

    @ApiModelProperty("主覆盖国家英文名（快照辅助展示）")
    private String mainCountryNameEn;

    // -------------------- 当前主表（实时对照） --------------------

    @ApiModelProperty("当前站点是否存在且未逻辑删除")
    private boolean existsNow;

    @ApiModelProperty("当前站点名称（若存在）")
    private String currentSiteName;

    @ApiModelProperty("当前站点URL（若存在）")
    private String currentUrl;

    @ApiModelProperty("当前主覆盖国家代码（若存在）")
    private String currentMainCountryCode;

    @ApiModelProperty("当前站点最近更新时间（若存在）")
    private LocalDateTime currentUpdatedAt;

    // -------------------- 综合标签 --------------------

    @ApiModelProperty("存在性标签：NORMAL / CHANGED / DELETED / MISSING")
    private ReviewedExistenceStatus existenceLabel;
}
