package com.yupi.springbootinit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.annotation.RequireUser;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.model.dto.ImportApplyBatchDTO;
import com.yupi.springbootinit.model.vo.ImportApplyVO;
import com.yupi.springbootinit.model.vo.ImportJobOverviewVO;
import com.yupi.springbootinit.model.vo.ImportJobWithStatsVO;
import com.yupi.springbootinit.service.ImportApprovalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.validation.constraints.NotBlank;

@Api(tags = "导入审批（按任务维度）")
@RestController
@RequestMapping("/import/apply")
@RequiredArgsConstructor
public class ImportApprovalController {

    private final ImportApprovalService importApprovalService;

    @GetMapping("/pending/page")
    @RequireUser(admin = true)
    @ApiOperation("分页查询：指定任务下的待审核申请")
    public BaseResponse<Page<ImportApplyVO>> pagePending(
            @RequestParam @NotBlank String jobId,
            @RequestParam(required = false, defaultValue = "1") long current,
            @RequestParam(required = false, defaultValue = "10") long size) {
        return ResultUtils.success(importApprovalService.pagePendingByJob(jobId, current, size));
    }

    @GetMapping("/detail")
    @RequireUser(admin = true)
    @ApiOperation("导入申请详情")
    public BaseResponse<ImportApplyVO> detail(@RequestParam @NotBlank String applyId) {
        return ResultUtils.success(importApprovalService.getDetail(applyId));
    }

    @PostMapping("/approve/batch")
    @RequireUser(admin = true)
    @ApiOperation("批量通过：传 applyIds 则按选中处理；不传则对 job 下全部 PENDING")
    public BaseResponse<Integer> approveBatch(@RequestBody ImportApplyBatchDTO dto) {
        return ResultUtils.success(importApprovalService.approveBatch(dto));
    }

    @PostMapping("/reject/batch")
    @RequireUser(admin = true)
    @ApiOperation("批量驳回：传 applyIds 则按选中处理；不传则对 job 下全部 PENDING")
    public BaseResponse<Integer> rejectBatch(@RequestBody ImportApplyBatchDTO dto) {
        return ResultUtils.success(importApprovalService.rejectBatch(dto));
    }

    @GetMapping("/admin/jobs/pending/page")
    @RequireUser(admin = true)
    @ApiOperation("分页列出包含待审申请的导入任务（可选筛选：提交人姓名模糊、关键字）")
    public BaseResponse<Page<ImportJobWithStatsVO>> pageJobsWithPending(
            @RequestParam(required = false, defaultValue = "1") long current,
            @RequestParam(required = false, defaultValue = "10") long size,
            @RequestParam(required = false) String submitUserName,
            @RequestParam(required = false) String keyword) {
        return ResultUtils.success(importApprovalService.pageJobsWithPending(current, size, submitUserName, keyword));
    }

    @GetMapping("/admin/job/overview")
    @RequireUser(admin = true)
    @ApiOperation("单个任务的审批总览（计数汇总）")
    public BaseResponse<ImportJobOverviewVO> jobOverview(@RequestParam @NotBlank String jobId) {
        return ResultUtils.success(importApprovalService.getJobOverview(jobId));
    }
}
