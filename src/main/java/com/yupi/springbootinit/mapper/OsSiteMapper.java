package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.OsSiteDO;
import com.yupi.springbootinit.model.vo.ChannelTopVO;
import com.yupi.springbootinit.model.vo.CountryStatVO;
import com.yupi.springbootinit.model.vo.ProviderTopVO;
import com.yupi.springbootinit.model.vo.ThemeStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OsSiteMapper extends BaseMapper<OsSiteDO> {

    /**
     * 国家聚合统计（地图气泡）
     */
    @Select({
            "SELECT r.COUNTRY_CODE   AS countryCode,",
            "       d.NAME_ZH        AS countryNameZh,",
            "       d.NAME_EN        AS countryNameEn,",
            "       COUNT(DISTINCT r.SITE_ID) AS siteCount",
            "FROM REL_SITE_SCOPE r",
            "JOIN OS_SITE s ON s.ID = r.SITE_ID AND s.IS_DELETE = 0",
            "JOIN COUNTRY_DICT d ON d.CODE = r.COUNTRY_CODE",
            "WHERE r.COUNTRY_CODE <> 'ALL'",
            "GROUP BY r.COUNTRY_CODE, d.NAME_ZH, d.NAME_EN",
            "ORDER BY siteCount DESC"
    })
    List<CountryStatVO> statByCountry();




    /**
     * 统计当前系统中“主覆盖国家”的去重数量（只统计未删除站点，且国家码需在字典中有效）
     */
    @Select({
            "SELECT COUNT(*)",
            "FROM (",
            "   SELECT DISTINCT s.MAIN_COUNTRY_CODE",
            "   FROM OS_SITE s",
            "   JOIN COUNTRY_DICT d ON d.CODE = s.MAIN_COUNTRY_CODE",
            "   WHERE s.IS_DELETE = 0",
            "     AND s.MAIN_COUNTRY_CODE IS NOT NULL",
            "     AND s.MAIN_COUNTRY_CODE <> 'ALL'",
            ") t"
    })
    Long countDistinctMainCountry();



    /**
     * 主题领域聚合统计（柱状图数据）
     * 口径：统计各主题下未删除站点数量；包含无站点的主题，数量为 0。
     */
    @Select({
            "SELECT",
            "  t.ID   AS themeId,",
            "  t.NAME AS themeName,",
            "  COUNT(DISTINCT CASE WHEN s.ID IS NOT NULL THEN s.ID END) AS siteCount",
            "FROM DICT_THEME t",
            "LEFT JOIN REL_SITE_THEME r ON r.THEME_ID = t.ID",
            "LEFT JOIN OS_SITE s ON s.ID = r.SITE_ID AND s.IS_DELETE = 0",
            "GROUP BY t.ID, t.NAME",
            "ORDER BY siteCount DESC, t.NAME ASC"
    })
    List<ThemeStatVO> statByTheme();

    /**
     * 在给定时间区间内，返回所有未删除站点的创建时间（用于按天/月聚合）
     */
    @Select({
            "SELECT CREATED_AT",
            "FROM OS_SITE",
            "WHERE IS_DELETE = 0",
            "  AND CREATED_AT >= #{begin}",
            "  AND CREATED_AT <  #{end}"
    })
    List<LocalDateTime> selectCreatedAtBetween(@Param("begin") LocalDateTime begin,
                                               @Param("end") LocalDateTime end);

    /**
     * 拉取未删除站点的关键词文本（用于词云统计）
     */
    @Select({
            "SELECT KEYWORDS_TEXT",
            "FROM OS_SITE",
            "WHERE IS_DELETE = 0",
            "  AND KEYWORDS_TEXT IS NOT NULL",
            "  AND TRIM(KEYWORDS_TEXT) <> ''"
    })
    List<String> listAllKeywordsText();


    /**
     * Provider Top-N 聚合：
     * - 仅统计未删除（IS_DELETE=0）
     * - 忽略空/空白 PROVIDER
     * - 聚合时大小写不敏感（按 LOWER(TRIM(PROVIDER)) 分组）
     * - 展示名称选用 MIN(TRIM(PROVIDER)) 作为代表（尽量保留原有大小写风格）
     */
    @Select({
            "<script>",
            "SELECT",
            "  MIN(TRIM(PROVIDER)) AS provider,",
            "  COUNT(*)            AS count",
            "FROM OS_SITE",
            "WHERE IS_DELETE = 0",
            "  AND PROVIDER IS NOT NULL",
            "  AND LENGTH(TRIM(PROVIDER)) > 0",
            "GROUP BY LOWER(TRIM(PROVIDER))",
            "ORDER BY count",
            "  <choose>",
            "    <when test='order == \"ASC\"'>ASC</when>",
            "    <otherwise>DESC</otherwise>",
            "  </choose>",
            "LIMIT #{limit}",
            "</script>"
    })
    List<ProviderTopVO> selectProviderTop(@Param("limit") int limit, @Param("order") String order);

    @Select({
            "<script>",
            "SELECT",
            "  MIN(TRIM(CHANNEL)) AS channel,",
            "  COUNT(*)           AS count",
            "FROM OS_SITE",
            "WHERE IS_DELETE = 0",
            "  AND CHANNEL IS NOT NULL",
            "  AND LENGTH(TRIM(CHANNEL)) > 0",
            "GROUP BY LOWER(TRIM(CHANNEL))",
            "ORDER BY count",
            "  <choose>",
            "    <when test='order == \"ASC\"'>ASC</when>",
            "    <otherwise>DESC</otherwise>",
            "  </choose>",
            "LIMIT #{limit}",
            "</script>"
    })
    List<ChannelTopVO> selectChannelTop(@Param("limit") int limit, @Param("order") String order);


}
