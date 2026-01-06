package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.OsSiteDO;
import com.yupi.springbootinit.model.entity.SiteApplyDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SiteApplyMapper extends BaseMapper<SiteApplyDO> {

    /**
     * 行级锁读取（审批/撤销/编辑草稿等并发敏感场景）
     */
    @Select("SELECT * FROM OS_SITE_APPLY WHERE ID = #{id} FOR UPDATE")
    SiteApplyDO selectByIdForUpdate(@Param("id") String id);


    /**
     * 并发防抖：仅当 PENDING 才能改为 APPROVED/REJECTED
     */
    @Update("UPDATE OS_SITE_APPLY " +
            "SET STATUS = #{status}, REVIEW_USER_ID = #{reviewUserId}, REVIEW_USER_NAME = #{reviewUserName}, " +
            "    REVIEW_REASON = #{reviewReason} " +
            "WHERE ID = #{id}")
    int resubmit(@Param("id") String id,
                 @Param("status") String status,
                 @Param("reviewUserId") String reviewUserId,
                 @Param("reviewUserName") String reviewUserName,
                 @Param("reviewReason") String reviewReason);

    /**
     * 仅当当前状态 == fromStatus 时，允许更新为 toStatus
     */
    @Update("UPDATE OS_SITE_APPLY " +
            "SET STATUS = #{toStatus}, " +
            "    REVIEW_USER_ID = #{reviewUserId}, " +
            "    REVIEW_USER_NAME = #{reviewUserName}, " +
            "    REVIEW_REASON = #{reviewReason}, " +
            "    REVIEWED_AT = SYSDATE " +
            "WHERE ID = #{id} AND STATUS = #{fromStatus}")
    int updateReviewIfStatus(@Param("id") String id,
                             @Param("fromStatus") String fromStatus,
                             @Param("toStatus") String toStatus,
                             @Param("reviewUserId") String reviewUserId,
                             @Param("reviewUserName") String reviewUserName,
                             @Param("reviewReason") String reviewReason);

    /**
     * 仅当当前状态 IN (from1, from2) 时，允许更新为 toStatus —— 用于 DRAFT/REJECTED -> PENDING
     */
    @Update("UPDATE OS_SITE_APPLY " +
            "SET STATUS = #{toStatus}, " +
            "    REVIEW_USER_ID = #{reviewUserId}, " +
            "    REVIEW_USER_NAME = #{reviewUserName}, " +
            "    REVIEW_REASON = #{reviewReason}, " +
            "    REVIEWED_AT = SYSDATE " +
            "WHERE ID = #{id} AND STATUS IN (#{fromStatus1}, #{fromStatus2})")
    int updateReviewIfStatusIn(@Param("id") String id,
                               @Param("fromStatus1") String fromStatus1,
                               @Param("fromStatus2") String fromStatus2,
                               @Param("toStatus") String toStatus,
                               @Param("reviewUserId") String reviewUserId,
                               @Param("reviewUserName") String reviewUserName,
                               @Param("reviewReason") String reviewReason);

    @Select({
            "SELECT COUNT(1) FROM OS_SITE_APPLY",
            "WHERE STATUS = 'PENDING' AND ACTION_TYPE = 'CREATE'",
            "  AND (URL = #{url} OR SITE_NAME = #{name})"
    })
    int countPendingCreateByUrlOrName(@Param("url") String url, @Param("name") String name);

    @Select("SELECT COUNT(1) FROM OS_SITE_APPLY WHERE STATUS='PENDING' AND ACTION_TYPE='UPDATE' AND TARGET_SITE_ID=#{siteId}")
    int countPendingUpdateBySite(@Param("siteId") String siteId);

    /**
     * 管理员查看自己已审批（通过/拒绝）的申请，按审核时间倒序
     */
    @Select({
            "SELECT",
            "  a.ID,",
            "  a.TARGET_SITE_ID,",
            "  a.ACTION_TYPE,",
            "  a.SITE_NAME,",
            "  a.URL,",
            "  a.PROVIDER,",
            "  a.CHANNEL,",
            "  a.SUMMARY,",
            "  a.KEYWORDS_TEXT,",
            "  a.REMARK,",
            "  a.MAIN_COUNTRY_CODE,",
            "  a.THEME_IDS_TEXT,",
            "  a.SCOPE_COUNTRY_CODES_TEXT,",
            "  a.STATUS,",
            "  a.SUBMIT_USER_ID,",
            "  a.SUBMIT_USER_NAME,",
            "  a.REVIEW_USER_ID,",
            "  a.REVIEW_USER_NAME,",
            "  a.REVIEW_REASON,",
            "  a.CREATED_AT,",
            "  a.UPDATED_AT,",
            "  a.REVIEWED_AT",
            "FROM OS_SITE_APPLY a",
            "WHERE a.REVIEW_USER_ID = #{uid}",
            "  AND a.STATUS IN ('APPROVED','REJECTED')",
            "ORDER BY NVL(a.REVIEWED_AT, a.UPDATED_AT) DESC"
    })
    List<SiteApplyDO> selectReviewedByReviewerDO(@Param("uid") String reviewerUserId);


    /**
     * 用户查看自己提交且已通过的站点（已发布）
     * 直接 JOIN 到 OS_SITE 并过滤未删除
     */
    @Select({
            "SELECT s.*",
            "FROM OS_SITE s",
            "JOIN OS_SITE_APPLY a ON a.TARGET_SITE_ID = s.ID",
            "WHERE a.SUBMIT_USER_ID = #{uid}",
            "  AND a.STATUS = 'APPROVED'",
            "  AND NVL(s.IS_DELETE, 0) = 0",
            "ORDER BY a.REVIEWED_AT DESC"
    })
    List<OsSiteDO> selectApprovedSitesBySubmitter(@Param("uid") String submitUserId);

    /**
     * 用户查看自己提交的申请，支持按状态筛选（为空则查全部），按创建时间倒序
     */
// SiteApplyMapper.java
    @Select({
            "<script>",
            "SELECT",
            "  a.ID,",
            "  a.TARGET_SITE_ID,",
            "  a.ACTION_TYPE,",
            "  a.SITE_NAME,",
            "  a.URL,",
            "  a.PROVIDER,",
            "  a.CHANNEL,",
            "  a.SUMMARY,",
            "  a.KEYWORDS_TEXT,",
            "  a.REMARK,",
            "  a.MAIN_COUNTRY_CODE,",
            "  a.THEME_IDS_TEXT,",
            "  a.SCOPE_COUNTRY_CODES_TEXT,",
            "  a.STATUS,",
            "  a.SUBMIT_USER_ID,",
            "  a.SUBMIT_USER_NAME,",
            "  a.REVIEW_USER_ID,",
            "  a.REVIEW_USER_NAME,",
            "  a.REVIEW_REASON,",
            "  a.CREATED_AT,",
            "  a.UPDATED_AT,",
            "  a.REVIEWED_AT",
            "FROM OS_SITE_APPLY a",
            "WHERE a.SUBMIT_USER_ID = #{uid}",
            "<if test='status != null and status != \"\"'>",
            "  AND a.STATUS = #{status}",
            "</if>",
            "ORDER BY NVL(a.UPDATED_AT, a.CREATED_AT) DESC",
            "</script>"
    })
    List<SiteApplyDO> selectBySubmitterAndStatus(@Param("uid") String submitUserId,
                                                 @Param("status") String status);


    /**
     * 覆盖草稿/被驳回内容（仅在 DRAFT/REJECTED 可修改）
     */
    @Update({
            "<script>",
            "UPDATE OS_SITE_APPLY",
            "   SET ",
            "       SITE_NAME = #{siteName},",
            "       URL = #{url},",
            "       PROVIDER = #{provider},",
            "       CHANNEL = #{channel},",
            "       SUMMARY = #{summary},",
            "       KEYWORDS_TEXT = #{keywordsText},",
            "       REMARK = #{remark},",
            "       MAIN_COUNTRY_CODE = #{mainCountryCode},",
            "       THEME_IDS_TEXT = #{themeIdsText},",
            "       SCOPE_COUNTRY_CODES_TEXT = #{scopeCountryCodesText},",
            "       UPDATED_AT = SYSDATE",
            " WHERE ID = #{applyId}",
            "   AND STATUS IN ('DRAFT','REJECTED')",
            "</script>"
    })
    int updateDraftContent(@Param("applyId") String applyId,
                           @Param("siteName") String siteName,
                           @Param("url") String url,
                           @Param("provider") String provider,
                           @Param("channel") String channel,
                           @Param("summary") String summary,
                           @Param("keywordsText") String keywordsText,
                           @Param("remark") String remark,
                           @Param("mainCountryCode") String mainCountryCode,
                           @Param("themeIdsText") String themeIdsText,
                           @Param("scopeCountryCodesText") String scopeCountryCodesText);

    /**
     * 物理删除申请（仅用于 DRAFT / REJECTED）
     */
    @Delete("DELETE FROM OS_SITE_APPLY WHERE ID = #{applyId}")
    int deleteByIdStrict(@Param("applyId") String applyId);


}
