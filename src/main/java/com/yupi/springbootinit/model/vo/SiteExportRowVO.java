package com.yupi.springbootinit.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteExportRowVO {

    @ExcelProperty(value = "SITE_NAME", index = 0)
    private String siteName;

    /** 主题名称（逗号拼接），而不是主题 ID */
    @ExcelProperty(value = "THEME", index = 1)
    private String theme;

    @ExcelProperty(value = "PROVIDER", index = 2)
    private String provider;

    @ExcelProperty(value = "CHANNEL", index = 3)
    private String channel;

    @ExcelProperty(value = "MAIN_COUNTRY_CODE", index = 4)
    private String mainCountryCode;

    /** 覆盖国家代码，逗号拼接，如：CN,US,JP */
    @ExcelProperty(value = "COVERAGE_COUNTRIES", index = 5)
    private String coverageCountries;

    @ExcelProperty(value = "URL", index = 6)
    private String url;

    @ExcelProperty(value = "IS_DELETE", index = 7)
    private Integer isDelete;

    /** 字符串格式：yyyy-MM-dd HH:mm:ss */
    @ExcelProperty(value = "CREATED_AT", index = 8)
    private String createdAt;

    /** 字符串格式：yyyy-MM-dd HH:mm:ss */
    @ExcelProperty(value = "UPDATED_AT", index = 9)
    private String updatedAt;
}
