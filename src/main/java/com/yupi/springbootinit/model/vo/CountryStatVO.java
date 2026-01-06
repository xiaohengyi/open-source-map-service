package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel("国家聚合统计")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountryStatVO {
    private String countryCode;
    private String countryNameZh;
    private String countryNameEn;
    private Long siteCount;
}
