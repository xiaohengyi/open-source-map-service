package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.annotation.RequireUser;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.enums.CommitMode;
import com.yupi.springbootinit.model.dto.SiteSaveDTO;
import com.yupi.springbootinit.model.vo.MyApprovedSiteItemVO;
import com.yupi.springbootinit.model.vo.ReviewedApplyItemVO;
import com.yupi.springbootinit.model.vo.SiteApplyVO;
import com.yupi.springbootinit.model.vo.SiteVO;
import com.yupi.springbootinit.service.ApprovalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "审批相关内容")
@RestController
@RequestMapping("/apply")
@RequiredArgsConstructor
public class ApplyQueryController {

    private final ApprovalService approvalService;

    /**
     * 1) 管理员查看自己已审批的申请（通过/拒绝），按审核时间倒序
     */
    @GetMapping("/reviewed/my")
    @ApiOperation("管理员查看自己已审批列表（通过/拒绝）")
    @RequireUser(admin = true)
    public BaseResponse<List<ReviewedApplyItemVO>> myReviewed() {
        return ResultUtils.success(approvalService.myReviewed());
    }

    /**
     * 2) 用户查看自己“已通过并已发布”的数据源列表（含管理员）
     */
    @GetMapping("/approved/my-sites")
    @ApiOperation("我提交且已通过的数据源（已发布）")
    @RequireUser
    public BaseResponse<List<MyApprovedSiteItemVO>> myApprovedSites() {
        return ResultUtils.success(approvalService.myApprovedSites());
    }

    /**
     * 3) 用户查看自己提交的申请（可按状态筛选：PENDING/APPROVED/REJECTED），按创建时间倒序
     */
    @GetMapping("/my")
    @ApiOperation("我提交的申请（支持按状态筛选）")
    @RequireUser
    public BaseResponse<List<SiteApplyVO>> myApplies(@RequestParam(required = false) String status) {
        return ResultUtils.success(approvalService.myApplies(status));
    }

    @PostMapping("/submit")
    @RequireUser
    @ApiOperation("普通用户提交数据源审核")
    public BaseResponse<String> submit(@RequestBody @Valid SiteSaveDTO dto) {
        String applyId = approvalService.submitApply(dto);
        return ResultUtils.success(applyId);
    }

    @PostMapping("/approve")
    @RequireUser(admin = true)
    @ApiOperation("管理员通过数据审核")
    public BaseResponse<String> approve(@RequestParam String id,
                                        @RequestParam(required = false) String remark) {
        String siteId = approvalService.approve(id, remark);
        return ResultUtils.success(siteId);
    }

    @PostMapping("/reject")
    @RequireUser(admin = true)
    @ApiOperation("管理员驳回用户申请")
    public BaseResponse<String> reject(@RequestParam String id,
                                       @RequestParam String reason) {
        approvalService.reject(id, reason);
        return ResultUtils.success();
    }

    @GetMapping("/pending")
    @ApiOperation("管理员查看所有待审核申请")
    @RequireUser(admin = true)
    public BaseResponse<List<SiteApplyVO>> pending() {
        return ResultUtils.success(approvalService.pending());
    }


    @PostMapping("/cancel")
    @ApiOperation("撤销我的待审申请（PENDING→DRAFT）")
    @RequireUser
    public BaseResponse<Void> cancel(@RequestParam String id) {
        approvalService.cancel(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/draft/save")
    @ApiOperation("保存草稿（新建或更新；支持新建草稿/编辑已有草稿/创建编辑草稿）")
    @RequireUser
    public BaseResponse<String> saveDraft(@RequestParam(required = false) String targetSiteId,
                                          @Valid @RequestBody SiteSaveDTO dto) {
        return ResultUtils.success(approvalService.saveDraft(dto, targetSiteId));
    }

    @PostMapping("/resubmit")
    @ApiOperation("编辑并重新提交（DRAFT/REJECTED→PENDING）")
    @RequireUser
    public BaseResponse<String> resubmit(@RequestParam String applyId, @Valid @RequestBody SiteSaveDTO dto) {
        return ResultUtils.success(approvalService.resubmit(applyId, dto));
    }

    @GetMapping("/drafts/my")
    @ApiOperation("我的草稿列表（按更新时间倒序）")
    @RequireUser
    public BaseResponse<List<SiteApplyVO>> myDrafts() {
        return ResultUtils.success(approvalService.myDrafts());
    }

    @PostMapping("/delete")
    @ApiOperation("删除我的草稿/被驳回申请")
    @RequireUser
    public BaseResponse<Boolean> deleteMyApply(@RequestParam String id) {
        approvalService.deleteMyApply(id);
        return ResultUtils.success(true);
    }

    @PostMapping("/publish")
    @RequireUser(admin = true)
    @ApiOperation("管理员直接发布（跳过审批）")
    public BaseResponse<String> publish(@RequestParam String applyId,
                                        @Valid @RequestBody(required = false) SiteSaveDTO dto) {
        return ResultUtils.success(approvalService.publish(applyId, dto));
    }


}
