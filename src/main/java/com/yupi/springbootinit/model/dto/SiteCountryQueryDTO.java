package com.yupi.springbootinit.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 按国家下钻查询 DTO
 * 继承 SiteQueryDTO，只额外加了 countryCode 和 includeGlobal 两个语义字段
 */
@ApiModel("SiteCountryQueryDTO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SiteCountryQueryDTO extends SiteQueryDTO {

    @ApiModelProperty("下钻国家代码（例如 CN / US），必填")
    @NotBlank(message = "国家代码不能为空")
    private String countryCode;

    @ApiModelProperty("是否包含主覆盖国家为 ALL（全球）的数据；默认 true")
    private Boolean includeGlobal = Boolean.TRUE;

    @ApiModelProperty("分页：当前页")
    @Max(value = 100, message = "分页页码最小值为1，最大值为100")
    @Min(value = 1, message = "分页页码最小值为1，最大值为100")
    @Override
    public Long getCurrent() {
        return super.getCurrent();
    }

    @ApiModelProperty("分页：每页大小")
    @Max(value = 20, message = "每页最多展示数据条目为20条")
    @Min(value = 1, message = "每页最少展示数据条目为1条")
    @Override
    public Long getSize() {
        return super.getSize();
    }
}
