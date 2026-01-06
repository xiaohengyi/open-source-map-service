package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
@TableName("COUNTRY_DICT")
public class CountryDictDO {

    @ApiModelProperty("国家代码（ISO-3166-1 alpha-2，例：CN/US/GB）")
    @TableId(value = "CODE", type = IdType.INPUT)
    private String code;

    @ApiModelProperty("国家中文名（例：中国）")
    @TableField("NAME_ZH")
    private String nameZh;

    @ApiModelProperty("国家英文名（例：China）")
    @TableField("NAME_EN")
    private String nameEn;
}
