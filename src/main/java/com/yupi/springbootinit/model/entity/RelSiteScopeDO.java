package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("REL_SITE_SCOPE")
public class RelSiteScopeDO {

    @ApiModelProperty("站点ID（OS_SITE.ID）")
    @TableField("SITE_ID")
    private String siteId;

    @ApiModelProperty("覆盖范围国家代码（COUNTRY_DICT.CODE）")
    @TableField("COUNTRY_CODE")
    private String countryCode;

}
