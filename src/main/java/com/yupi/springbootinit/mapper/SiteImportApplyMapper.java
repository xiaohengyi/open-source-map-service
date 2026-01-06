package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.SimpleApplyStatusRow;
import com.yupi.springbootinit.model.entity.SiteImportApplyDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SiteImportApplyMapper extends BaseMapper<SiteImportApplyDO> {
    /**
     * 按 import_item_id 取“最新一条”并加行级锁
     * 口径：按 UPDATED_AT / CREATED_AT / ID 倒序作为“最新”
     */
    @Select(
            "SELECT * FROM  " +
            "( SELECT a.*" +
                    " FROM SITE_IMPORT_APPLY a " +
                    " WHERE a.IMPORT_ITEM_ID = #{itemId} " +
                    " ORDER BY a.UPDATED_AT DESC, a.CREATED_AT DESC, a.ID DESC ) t " +
                    " WHERE ROWNUM <= 1 FOR UPDATE"
    )
    SiteImportApplyDO selectLatestByItemForUpdate(@Param("itemId") String itemId);

    /**
     * 统计“每条明细的最新状态”= 指定 status 的条数
     * 用窗口函数挑出每个 IMPORT_ITEM_ID 最新的一条（RN=1）
     */
    @Select(
            "SELECT COUNT(1) " +
                    "  FROM ( " +
                    "        SELECT a.IMPORT_ITEM_ID, a.STATUS, " +
                    "               ROW_NUMBER() OVER (" +
                    "                 PARTITION BY a.IMPORT_ITEM_ID " +
                    "                 ORDER BY a.UPDATED_AT DESC, a.CREATED_AT DESC, a.ID DESC " +
                    "               ) AS RN " +
                    "          FROM SITE_IMPORT_APPLY a " +
                    "         WHERE a.IMPORT_JOB_ID = #{jobId} " +
                    "       ) T " +
                    " WHERE T.RN = 1 " +
                    "   AND T.STATUS = #{status}"
    )
    long countLatestByJobAndStatus(@Param("jobId") String jobId, @Param("status") String status);

    /**
     * 物理删除某个明细的全部导入审批记录（仅在 item 可删时调用）
     */
    @Delete("DELETE FROM SITE_IMPORT_APPLY WHERE IMPORT_ITEM_ID = #{itemId}")
    void deleteByImportItemId(@Param("itemId") String itemId);


    /**
     * 批量查询每个 import_item_id 的“最新一条”申请记录
     */
    @Select({
            "<script>",
            "SELECT * FROM SITE_IMPORT_APPLY ",
            "WHERE IMPORT_ITEM_ID IN ",
            "<foreach collection='itemIds' item='id' open='(' separator=',' close=')'>",
            "  #{id}",
            "</foreach>",
            "</script>"
    })
    List<SiteImportApplyDO> selectByItemIds(@Param("itemIds") Collection<String> itemIds);

    /**
     * 查询给定 itemId 列表中，每个明细的“最新一条审批”的状态
     * 说明：SQL 简洁易懂，聚合后回表一次；返回最必要的字段，业务组装放在 Service。
     */
    @Select({
            "<script>",
            "SELECT s1.import_item_id   AS importItemId,",
            "       s1.id               AS applyId,",
            "       s1.status           AS approvalStatus,",
            "       s1.review_reason    AS approvalReason,",
            "       s1.reviewed_at      AS approvalReviewedAt",
            "FROM site_import_apply s1",
            "JOIN (",
            "   SELECT import_item_id, MAX(created_at) AS max_created_at",
            "   FROM site_import_apply",
            "   WHERE import_item_id IN",
            "   <foreach collection='itemIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "   GROUP BY import_item_id",
            ") t ON t.import_item_id = s1.import_item_id AND t.max_created_at = s1.created_at",
            "</script>"
    })
    List<SimpleApplyStatusRow> selectLatestStatusByItemIds(@Param("itemIds") List<String> itemIds);
}
