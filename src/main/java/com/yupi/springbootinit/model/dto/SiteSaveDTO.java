package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@ApiModel("SiteSaveDTO")
@Data
public class SiteSaveDTO {

    @ApiModelProperty(value = "站点ID（编辑时必传，新建可空）", example = "ZXxk2JrGQ0mXl1oZ1o-7uQ")
    private String id;

    @ApiModelProperty(value = "网站名称", required = true)
    @NotBlank(message = "网站名称不能为空")
    @Size(max = 40,message = "网站名称过长，最长不能超过40字符")
    private String siteName;

    @ApiModelProperty(value = "网站地址（必填）", required = true, example = "https://www.example.com/path")
    @NotBlank(message = "网站地址不能为空")
    @Size(max = 512, message = "网站地址过长")
    private String url;

    @ApiModelProperty("主题ID列表（DICT_THEME.ID）")
    private List<String> themeIds;

    @ApiModelProperty("提供方")
    @Size(max = 20, message = "提供方名称过长，最长不能超过20字符")
    private String provider;

    @ApiModelProperty("信息渠道/获取手段")
    private String channel;

    @ApiModelProperty("摘要")
    @Size(max = 1024, message = "摘要内容过长，最长不能超过1024字符")
    private String summary;

    @ApiModelProperty("关键词文本（逗号分隔）")
    private String keywordsText;

    @ApiModelProperty("备注")
    @Size(max = 512, message = "备注内容过长，最长不能超过512字符")
    private String remark;

    @ApiModelProperty("主覆盖国家（ISO-3166-1 alpha-2），为空则从 scopes 的 isPrimary 推断")
    @Size(min = 2, max = 3, message = "标准国家代码必须是长度为两位或者三位，例如CN、US或者是ALL（全球）")
    @NotBlank
    private String mainCountryCode;

    @ApiModelProperty(value = "覆盖范围国家列表（可指定是否主覆盖）", required = true)
    private List<String> scopes;


}
