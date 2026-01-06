package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.model.entity.RelSiteThemeDO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

@Mapper
public interface RelSiteThemeMapper {

    @Delete("DELETE FROM REL_SITE_THEME WHERE SITE_ID = #{siteId}")
    int deleteBySiteId(@Param("siteId") String siteId);

    @Insert({
            "<script>",
            "INSERT INTO REL_SITE_THEME(SITE_ID, THEME_ID) VALUES",
            "<foreach collection='rows' item='r' separator=','>",
            "(#{r.siteId}, #{r.themeId})",
            "</foreach>",
            "</script>"
    })
    int batchInsert(@Param("rows") List<RelSiteThemeDO> rows);

    @Select("SELECT THEME_ID FROM REL_SITE_THEME WHERE SITE_ID = #{siteId}")
    List<String> listThemeIds(@Param("siteId") String siteId);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM REL_SITE_THEME WHERE THEME_ID = #{themeId}",
            "</script>"
    })
    int countByThemeId(@Param("themeId") String themeId);

    @Delete("DELETE FROM REL_SITE_THEME WHERE THEME_ID = #{themeId}")
    int deleteByThemeId(@Param("themeId") String themeId);

    /** 任一匹配（如有需要的场景；检索改为用 All） */
    @Select({
            "<script>",
            "SELECT DISTINCT r.SITE_ID",
            "FROM REL_SITE_THEME r",
            "JOIN OS_SITE s ON s.ID = r.SITE_ID AND s.IS_DELETE = 0",
            "WHERE r.THEME_ID IN",
            "<foreach collection='themeIds' item='t' open='(' separator=',' close=')'>",
            "#{t}",
            "</foreach>",
            "</script>"
    })
    List<String> selectSiteIdsByThemeIdsAny(@Param("themeIds") List<String> themeIds);

    /** ★ 主题“全部匹配” */
    @Select({
            "<script>",
            "SELECT r.SITE_ID",
            "FROM REL_SITE_THEME r",
            "JOIN OS_SITE s ON s.ID = r.SITE_ID AND s.IS_DELETE = 0",
            "WHERE r.THEME_ID IN",
            "<foreach collection='themeIds' item='t' open='(' separator=',' close=')'>",
            "#{t}",
            "</foreach>",
            "GROUP BY r.SITE_ID",
            "HAVING COUNT(DISTINCT r.THEME_ID) = #{size}",
            "</script>"
    })
    List<String> selectSiteIdsByThemeIdsAll(@Param("themeIds") List<String> themeIds,
                                            @Param("size") int size);

    @Select({
            "<script>",
            "SELECT SITE_ID, THEME_ID FROM REL_SITE_THEME",
            "WHERE SITE_ID IN",
            "<foreach collection='siteIds' item='s' open='(' separator=',' close=')'>",
            "#{s}",
            "</foreach>",
            "</script>"
    })
    List<Map<String, String>> listPairsBySiteIds(@Param("siteIds") List<String> siteIds);

    // 把 raw list 聚合成 Map<siteId, List<themeId>>
    default Map<String, List<String>> listThemeIdsBatch(List<String> siteIds) {
        if (siteIds == null || siteIds.isEmpty()) return emptyMap();
        List<Map<String, String>> rows = listPairsBySiteIds(siteIds);
        return rows.stream().collect(Collectors.groupingBy(
                m -> m.get("SITE_ID"),
                Collectors.mapping(m -> m.get("THEME_ID"), Collectors.toList())
        ));
    }
}
