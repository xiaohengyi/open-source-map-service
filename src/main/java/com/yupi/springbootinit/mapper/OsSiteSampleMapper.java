package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.OsSiteSampleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OsSiteSampleMapper extends BaseMapper<OsSiteSampleDO> {
    @Select({
            "SELECT *",
            "FROM OS_SITE_SAMPLE",
            "WHERE ID = #{id}",
            "  AND IS_DELETE = 0"
    })
    OsSiteSampleDO selectActiveById(@Param("id") String id);

    @Select({
            "SELECT *",
            "FROM OS_SITE_SAMPLE",
            "WHERE SITE_ID = #{siteId}",
            "  AND IS_DELETE = 0",
            "ORDER BY UPDATED_AT DESC, CREATED_AT DESC"
    })
    List<OsSiteSampleDO> selectActiveBySiteId(@Param("siteId") String siteId);
}
