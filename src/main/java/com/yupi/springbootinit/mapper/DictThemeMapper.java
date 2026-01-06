package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.DictThemeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.*;

@Mapper
public interface DictThemeMapper extends BaseMapper<DictThemeDO> {
    @Select({
            "<script>",
            "SELECT NAME FROM DICT_THEME WHERE ID IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<String> selectNamesByIds(@Param("ids") List<String> ids);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM DICT_THEME WHERE ID IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int countByIds(@Param("ids") List<String> ids);

    /** 名称唯一性（排除自身ID） */
    @Select({
            "<script>",
            "SELECT COUNT(1) FROM DICT_THEME",
            "WHERE NAME = #{name}",
            "<if test='excludeId != null and excludeId != \"\"'>",
            "  AND ID &lt;&gt; #{excludeId}",
            "</if>",
            "</script>"
    })
    int countByName(@Param("name") String name, @Param("excludeId") String excludeId);

    /** 编码唯一性（排除自身ID；CODE 允许为空，因此注意过滤 null） */
    @Select({
            "<script>",
            "SELECT COUNT(1) FROM DICT_THEME",
            "WHERE CODE = #{code}",
            "<if test='excludeId != null and excludeId != \"\"'>",
            "  AND ID &lt;&gt; #{excludeId}",
            "</if>",
            "</script>"
    })
    int countByCode(@Param("code") String code, @Param("excludeId") String excludeId);

    /** 批量拉取 (ID, NAME) 原始行 */
    @Select({
            "<script>",
            "SELECT ID, NAME FROM DICT_THEME",
            "WHERE ID IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<Map<String, Object>> selectIdNamePairs(@Param("ids") Collection<String> ids);

    /** 将 (ID, NAME) 列表转成 Map<ID, NAME> */
    default Map<String, String> selectIdNameMapByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        List<Map<String, Object>> rows = selectIdNamePairs(ids);
        Map<String, String> map = new HashMap<>(rows.size());
        for (Map<String, Object> r : rows) {
            map.put(Objects.toString(r.get("ID"), null),
                    Objects.toString(r.get("NAME"), null));
        }
        return map;
    }
}
