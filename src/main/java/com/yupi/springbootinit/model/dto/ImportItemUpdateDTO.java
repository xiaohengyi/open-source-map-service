package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 导入明细编辑 DTO
 * 仅任务拥有者可编辑；提交字段为“部分更新”，未提供的字段不变。
 * 注意：
 * 1) themeIdsText 与 themeNamesText 同时提供时，以 themeIdsText 为准；
 * 2) scopesText 为逗号分隔国家代码串（兼容中文逗号），服务端会统一转大写去重；
 * 3) mainCountryCode 建议使用 ISO-3166-1 alpha-2/3 或 ALL。
 */
@Data
@ApiModel("导入明细编辑请求")
public class ImportItemUpdateDTO {

    @ApiModelProperty(value = "明细ID（必填）", required = true, example = "ZXxk2JrGQ0mXl1oZ1o-7uQ")
    @NotBlank(message = "itemId 不能为空")
    private String itemId;

    // —— 可选：以下字段任意提供即覆盖原值（未提供保持不变） —— //

    @ApiModelProperty(value = "网站名称（可选，≤40）", example = "Open Source Map")
    @Size(max = 40, message = "网站名称过长，最长不能超过40字符")
    private String siteName;

    @ApiModelProperty(value = "网站地址（可选，≤512）", example = "https://www.example.com/path")
    @Size(max = 512, message = "网站地址过长")
    private String url;

    @ApiModelProperty(value = "主覆盖国家代码（可选，长度2或3，例如 CN/USA/ALL）", example = "CN")
    @Size(min = 2, max = 3, message = "标准国家代码必须是长度为两位或者三位，例如 CN、US 或 ALL")
    private String mainCountryCode;

    @ApiModelProperty(value = "提供方（可选）", example = "CYZK")
    private String provider;

    @ApiModelProperty(value = "渠道（可选）", example = "互联网检测")
    private String channel;

    @ApiModelProperty(value = "摘要（可选）")
    private String summary;

    @ApiModelProperty(value = "关键词（逗号分隔，可选）", example = "能源,国际关系")
    private String keywordsText;

    @ApiModelProperty(value = "备注（可选）")
    private String remark;

    @ApiModelProperty(value = "覆盖国家代码列表（逗号分隔，可选，服务端统一转大写去重）", example = "CN,US,JP")
    private String scopesText;

    @ApiModelProperty(value = "主题ID列表（逗号分隔，可选）", example = "Fn-_A1tZRK-SBJ-XK7E90Q,ZaYzv7_xTPeTW7cnfkvgFw")
    private String themeIdsText;

    @ApiModelProperty(value = "主题名称列表（逗号分隔，可选；若同时提供 themeIdsText 则忽略）", example = "人物,能源")
    private String themeNamesText;
}
