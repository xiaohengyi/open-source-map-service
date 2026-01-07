package com.yupi.springbootinit.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.List;

@ApiModel("SiteQueryDTO")
@Data
public class SiteQueryDTO {

    @ApiModelProperty("网站名称模糊")
    private String nameLike;

    @ApiModelProperty("主题ID列表（任一匹配）")
    private List<String> themeIds;

    @ApiModelProperty("提供方（模糊）")
    private String provider;

    @ApiModelProperty("信息渠道（模糊）")
    private String channel;

    @ApiModelProperty("数据质量（一般/重要/非常重要）")
    private String dataQuality;

    @ApiModelProperty("覆盖国家代码列表（必须全部命中，AND 条件，基于 REL_SITE_SCOPE）")
    private List<String> countryCodes;

    @ApiModelProperty("主覆盖国家代码（单选，IN OS_SITE.MAIN_COUNTRY_CODE；为空则不过滤）")
    private String mainCountryCode;

    @ApiModelProperty("更新时间起（yyyy-MM-dd HH:mm:ss）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @ApiModelProperty("更新时间止（yyyy-MM-dd HH:mm:ss）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty("分页：当前页")
    @Max(value = 100, message = "分页页码最小值为1，最大值为100")
    @Min(value = 1, message = "分页页码最小值为1，最大值为100")
    private Long current = 1L;

    @ApiModelProperty("分页：每页大小")
    @Max(value = 20, message = "每页最多展示数据条目为20条")
    @Min(value = 1, message = "每页最少展示数据条目为1条")
    private Long size = 10L;

    @ApiModelProperty(
            value = "排序字段（可选）" +
                    "，允许值：updatedAt / createdAt / siteName / provider / mainCountryCode",
            example = "updatedAt"
    )
    @Pattern(
            regexp = "(updatedAt|createdAt|siteName|provider|mainCountryCode)",
            message = "排序字段不合法"
    )
    private String sortField;

    @ApiModelProperty(
            value = "排序方式（可选），asc 或 desc，默认 desc",
            example = "desc"
    )
    @Pattern(
            regexp = "(?i)(asc|desc)",   // (?i) 忽略大小写
            message = "排序方式不合法"
    )
    private String sortOrder;
}
