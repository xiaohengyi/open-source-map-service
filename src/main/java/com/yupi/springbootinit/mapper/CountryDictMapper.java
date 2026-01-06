package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.CountryDictDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.*;

@Mapper
public interface CountryDictMapper extends BaseMapper<CountryDictDO> {

    @Select({
        "<script>",
        "SELECT COUNT(1) FROM COUNTRY_DICT WHERE CODE IN",
        "<foreach collection='codes' item='c' open='(' separator=',' close=')'>",
        "#{c}",
        "</foreach>",
        "</script>"
    })
    int countByCodes(@Param("codes") List<String> codes);

    @Select("SELECT NAME_ZH, NAME_EN FROM COUNTRY_DICT WHERE CODE = #{code}")
    Map<String, String> selectNames(@Param("code") String code);

    /** 批量查询国家中文/英文名的原始行 */
    @Select({
            "<script>",
            "SELECT CODE, NAME_ZH, NAME_EN",
            "FROM COUNTRY_DICT",
            "WHERE CODE IN",
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>",
            "#{c}",
            "</foreach>",
            "</script>"
    })
    List<Map<String, Object>> selectNamesBatchRaw(@Param("codes") Collection<String> codes);

    /** 将批量结果转成 { code -> {ZH:中文, EN:英文} } 的映射 */
    default Map<String, Map<String, String>> selectNameMapBatch(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) return Collections.emptyMap();
        List<Map<String, Object>> rows = selectNamesBatchRaw(codes);
        Map<String, Map<String, String>> out = new HashMap<>();
        for (Map<String, Object> r : rows) {
            String code = Objects.toString(r.get("CODE"), null);
            String zh = Objects.toString(r.get("NAME_ZH"), null);
            String en = Objects.toString(r.get("NAME_EN"), null);
            Map<String, String> nm = new HashMap<>(2);
            nm.put("ZH", zh);
            nm.put("EN", en);
            out.put(code, nm);
        }
        return out;
    }

    default Map<String, String> selectNameMap(String code) {
        Map<String, String> m = selectNames(code);
        Map<String, String> r = new HashMap<>();
        if (m != null) {
            r.put("ZH", m.get("NAME_ZH"));
            r.put("EN", m.get("NAME_EN"));
        }
        return r;
    }
}
