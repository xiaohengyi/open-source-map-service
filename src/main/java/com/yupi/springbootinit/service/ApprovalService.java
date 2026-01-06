package com.yupi.springbootinit.service;

import com.yupi.springbootinit.enums.CommitMode;
import com.yupi.springbootinit.model.dto.SiteSaveDTO;
import com.yupi.springbootinit.model.vo.MyApprovedSiteItemVO;
import com.yupi.springbootinit.model.vo.ReviewedApplyItemVO;
import com.yupi.springbootinit.model.vo.SiteApplyVO;
import com.yupi.springbootinit.model.vo.SiteVO;

import java.util.List;

public interface ApprovalService {

    /**
     * 普通用户提交，返回申请ID
     */
    String submitApply(SiteSaveDTO dto);

    /**
     * 管理员审批：通过
     */
    String approve(String applyId, String remark);

    /**
     * 管理员审批：驳回
     */
    void reject(String applyId, String reason);

    /**
     * 列出全部待审核申请（管理员查看）
     */
    List<SiteApplyVO> pending();

    /**
     * 管理员查看自己已审批（通过或拒绝）的申请
     */
    List<ReviewedApplyItemVO> myReviewed();

    /**
     * 用户查看自己提交且已通过（已发布）的站点列表
     */
    List<MyApprovedSiteItemVO> myApprovedSites();

    /**
     * 用户查看自己提交的申请，可按状态筛选（PENDING / APPROVED / REJECTED）
     */
    List<SiteApplyVO> myApplies(String status);

    /**
     * 用户撤销自己的待审核申请
     */
    void cancel(String applyId);

    /**
     * 用户保存数据源草稿
     */
    String saveDraft(SiteSaveDTO dto, String targetSiteId);

    /**
     * 用户重新提交自己被驳回的申请
     */
    String resubmit(String applyId, SiteSaveDTO dto);

    /**
     * 用户删除自己的草稿或者是被驳回的申请
     */
    void deleteMyApply(String applyId);

    /**
     * 用户查看自己的草稿列表
     */
    List<SiteApplyVO> myDrafts();

    /**
     * 管理员直接发布草稿，跳过审批
     */
    String publish(String applyId, SiteSaveDTO dto);


}
