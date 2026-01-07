package com.yupi.springbootinit.common;

import java.util.Arrays;
import java.util.List;

public final class SiteExcelHeaders {

    private SiteExcelHeaders() {}

    public static final String COL_SITE_NAME           = "SITE_NAME";
    public static final String COL_URL                 = "URL";
    public static final String COL_MAIN_COUNTRY_CODE   = "MAIN_COUNTRY_CODE";
    public static final String COL_THEME               = "THEME";
    public static final String COL_COVERAGE_COUNTRIES  = "COVERAGE_COUNTRIES";
    public static final String COL_PROVIDER            = "PROVIDER";
    public static final String COL_CHANNEL             = "CHANNEL";
    public static final String COL_DATA_QUALITY        = "DATA_QUALITY";
    public static final String COL_SUMMARY             = "SUMMARY";
    public static final String COL_KEYWORDS_TEXT       = "KEYWORDS_TEXT";
    public static final String COL_REMARK              = "REMARK";

    // 你导出的额外字段（可选）
    public static final String COL_IS_DELETE           = "IS_DELETE";
    public static final String COL_CREATED_AT          = "CREATED_AT";
    public static final String COL_UPDATED_AT          = "UPDATED_AT";

    // 统一列顺序（导出时用）
    public static final List<String> EXPORT_HEADER_ORDER =
            Arrays.asList(
                    COL_SITE_NAME,
                    COL_THEME,
                    COL_PROVIDER,
                    COL_CHANNEL,
                    COL_DATA_QUALITY,
                    COL_MAIN_COUNTRY_CODE,
                    COL_COVERAGE_COUNTRIES,
                    COL_URL,
                    COL_SUMMARY,
                    COL_KEYWORDS_TEXT,
                    COL_REMARK,
                    COL_IS_DELETE,
                    COL_CREATED_AT,
                    COL_UPDATED_AT
            );
}
