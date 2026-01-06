package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.model.entity.RelSiteScopeDO;
import com.yupi.springbootinit.model.vo.CountryScopeVO;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Mapper
@Repository
public interface RelSiteScopeMapper {

    @Delete("DELETE FROM REL_SITE_SCOPE WHERE SITE_ID = #{siteId}")
    int deleteBySiteId(@Param("siteId") String siteId);

    @Insert({
            "<script>",
            "INSERT INTO REL_SITE_SCOPE (SITE_ID, COUNTRY_CODE) VALUES",
            "<foreach collection='list' item='r' separator=','>",
            "(#{r.siteId}, #{r.countryCode})",
            "</foreach>",
            "</script>"
    })
    int batchInsert(@Param("list") List<RelSiteScopeDO> rows);


    // 新增：站点范围含 ALL 的站点（未删除）
    @Select({
            "SELECT DISTINCT r.SITE_ID",
            "FROM REL_SITE_SCOPE r",
            "JOIN OS_SITE s ON s.ID = r.SITE_ID AND s.IS_DELETE = 0",
            "WHERE r.COUNTRY_CODE = 'ALL'"
    })
    List<String> selectSiteIdsHavingAll();

    // 新增：给定 codes，返回命中的 (siteId, countryCode) 对（未删除）
    // 复用已有的 VO：CountryScopeVO，仅用到 siteId / countryCode 两列
    @Select({
            "<script>",
            "SELECT r.SITE_ID AS siteId,",
            "       r.COUNTRY_CODE AS countryCode",
            "FROM REL_SITE_SCOPE r",
            "JOIN OS_SITE s ON s.ID = r.SITE_ID AND s.IS_DELETE = 0",
            "WHERE r.COUNTRY_CODE IN",
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>",
            "#{c}",
            "</foreach>",
            "</script>"
    })
    List<CountryScopeVO> listSiteCodesIn(@Param("codes") List<String> codes);


    /**
     * 任一国家匹配（如有其它场景需要）
     */
    @Select({
            "<script>",
            "SELECT DISTINCT r.SITE_ID",
            "FROM REL_SITE_SCOPE r",
            "JOIN OS_SITE s ON s.ID = r.SITE_ID AND s.IS_DELETE = 0",
            "WHERE r.COUNTRY_CODE IN",
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>",
            "#{c}",
            "</foreach>",
            "</script>"
    })
    List<String> selectSiteIdsByCountriesAny(@Param("codes") List<String> countryCodes);

    /**
     * ★ 覆盖国家“全部匹配”
     */
    @Select({
            "<script>",
            "SELECT r.SITE_ID",
            "FROM REL_SITE_SCOPE r",
            "JOIN OS_SITE s ON s.ID = r.SITE_ID AND s.IS_DELETE = 0",
            "WHERE r.COUNTRY_CODE = 'ALL' ",
            "   OR r.COUNTRY_CODE IN ",
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>",
            "#{c}",
            "</foreach>",
            "GROUP BY r.SITE_ID",
            "HAVING",
            "  SUM(CASE WHEN MAX(CASE WHEN r.COUNTRY_CODE = 'ALL' THEN 1 ELSE 0 END)=1 THEN 1 ELSE 0 END) >= 1",
            "  OR COUNT(DISTINCT CASE WHEN r.COUNTRY_CODE IN ",
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>",
            "#{c}",
            "</foreach>",
            "  THEN r.COUNTRY_CODE END) = #{size}",
            "</script>"
    })
    List<String> selectSiteIdsByCountriesAll(@Param("codes") List<String> codes,
                                             @Param("size") int size);


    /**
     * 单站点范围（带中文/英文名），排除已删除站点
     */
    @Select({
            "SELECT r.SITE_ID        AS siteId,",
            "       r.COUNTRY_CODE   AS countryCode,",
            "       d.NAME_ZH        AS countryNameZh,",
            "       d.NAME_EN        AS countryNameEn",
            "FROM REL_SITE_SCOPE r",
            "JOIN OS_SITE s ON s.ID = r.SITE_ID AND s.IS_DELETE = 0",
            "JOIN COUNTRY_DICT d ON d.CODE = r.COUNTRY_CODE",
            "WHERE r.SITE_ID = #{siteId}",
            "ORDER BY d.NAME_EN"
    })
    List<CountryScopeVO> listScopesWithNames(@Param("siteId") String siteId);

    /**
     * 批量范围（带中文/英文名），排除已删除站点
     */
    @Select({
            "<script>",
            "SELECT r.SITE_ID      AS siteId,",
            "       r.COUNTRY_CODE AS countryCode,",
            "       d.NAME_ZH      AS countryNameZh,",
            "       d.NAME_EN      AS countryNameEn",
            "FROM REL_SITE_SCOPE r",
            "JOIN OS_SITE s ON s.ID = r.SITE_ID AND s.IS_DELETE = 0",
            "JOIN COUNTRY_DICT d ON d.CODE = r.COUNTRY_CODE",
            "WHERE r.SITE_ID IN",
            "<foreach collection='siteIds' item='sid' open='(' separator=',' close=')'>",
            "#{sid}",
            "</foreach>",
            "ORDER BY r.SITE_ID, d.NAME_EN",
            "</script>"
    })
    List<CountryScopeVO> listScopesWithNamesRaw(@Param("siteIds") List<String> siteIds);

    /**
     * 把 raw list 聚合成 Map<siteId, List<CountryScopeVO>>
     */
    default Map<String, List<CountryScopeVO>> listScopesWithNamesBatch(List<String> siteIds) {
        if (siteIds == null || siteIds.isEmpty()) return Collections.emptyMap();
        List<CountryScopeVO> list = listScopesWithNamesRaw(siteIds);
        Map<String, List<CountryScopeVO>> map = new HashMap<>();
        for (CountryScopeVO vo : list) {
            map.computeIfAbsent(vo.getSiteId(), k -> new ArrayList<>()).add(vo);
        }
        return map;
    }
}
