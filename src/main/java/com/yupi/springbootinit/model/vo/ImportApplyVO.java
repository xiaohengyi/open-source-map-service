package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@ApiModel("ImportApplyVO")
@Data
public class ImportApplyVO {
    @ApiModelProperty("申请ID")
    private String id;

    @ApiModelProperty("动作类型")
    private String actionType;

    @ApiModelProperty("网站名称")
    private String siteName;

    @ApiModelProperty("网站地址")
    private String url;

    @ApiModelProperty("主覆盖国家")
    private String mainCountryCode;

    @ApiModelProperty("主覆盖国家中文名")
    private String mainCountryNameZh;

    @ApiModelProperty("主覆盖国家英文名")
    private String mainCountryNameEn;

    @ApiModelProperty("主题名列表")
    private List<String> themeNames;

    @ApiModelProperty("覆盖国家（含中英名）")
    private List<CountryScopeVO> scopes;

    @ApiModelProperty("渠道")
    private String channel;

    @ApiModelProperty("提供方")
    private String provider;

    @ApiModelProperty("关键词（逗号分隔）")
    private String keywordsText;

    @ApiModelProperty("摘要")
    private String summary;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("数据质量")
    private String dataQuality;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("提交人")
    private String submitUserName;

    @ApiModelProperty("审核人")
    private String reviewUserName;

    @ApiModelProperty("审核说明")
    private String reviewReason;

    @ApiModelProperty("导入任务ID")
    private String importJobId;

    @ApiModelProperty("导入明细ID")
    private String importItemId;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;

    @ApiModelProperty("审核时间")
    private LocalDateTime reviewedAt;
}
