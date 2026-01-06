package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.ImportItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImportItemMapper extends BaseMapper<ImportItemDO> {

    @Select("SELECT COUNT(1) FROM OS_IMPORT_ITEM WHERE JOB_ID=#{jobId} AND VALID_STATUS=#{status}")
    int countByJobAndStatus(@Param("jobId") String jobId, @Param("status") String status);

    @Select("SELECT COUNT(1) FROM SITE_IMPORT_APPLY WHERE IMPORT_ITEM_ID=#{itemId}")
    int countApplyByItemId(@Param("itemId") String itemId);

    /**
     * 返回某任务下（可选按 validStatus）所有明细ID，按 row_no ASC, id ASC 排序
     * 说明：仅查 ID，便于在 Service 层做二次过滤/分页。
     */
    @Select({
            "<script>",
            "SELECT id",
            "FROM os_import_item",
            "WHERE job_id = #{jobId}",
            "<if test='validStatus != null and validStatus != \"\"'>",
            "  AND valid_status = #{validStatus}",
            "</if>",
            "ORDER BY row_no ASC, id ASC",
            "</script>"
    })
    List<String> selectOrderedItemIdsByJob(
            @Param("jobId") String jobId,
            @Param("validStatus") String validStatus
    );

    /**
     * 按 ID 集查询完整明细（结果仍按 row_no, id 排序，保证与前端展示一致）
     */
    @Select({
            "<script>",
            "SELECT *",
            "FROM os_import_item",
            "WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "  #{id}",
            "</foreach>",
            "ORDER BY row_no ASC, id ASC",
            "</script>"
    })
    List<ImportItemDO> selectItemsByIdsOrdered(@Param("ids") List<String> ids);
}
