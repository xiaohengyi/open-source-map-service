package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.model.dto.ImportApplyBatchDTO;
import com.yupi.springbootinit.model.vo.ImportApplyVO;
import com.yupi.springbootinit.model.vo.ImportJobOverviewVO;
import com.yupi.springbootinit.model.vo.ImportJobWithStatsVO;

public interface ImportApprovalService {

    /**
     * 分页查询：按任务维度查看 PENDING 导入申请
     */
    Page<ImportApplyVO> pagePendingByJob(String jobId, long current, long size);

    /**
     * 详情
     */
    ImportApplyVO getDetail(String applyId);

    /**
     * 批量通过（传 applyIds 则按列表；不传则对 job 下全部 PENDING）
     * 返回成功数
     */
    int approveBatch(ImportApplyBatchDTO dto);

    /**
     * 批量驳回（传 applyIds 则按列表；不传则对 job 下全部 PENDING）
     * 返回成功数
     */
    int rejectBatch(ImportApplyBatchDTO dto);

    /**
     * 管理员查看当前还存在待审核申请的任务列表
     */
    Page<ImportJobWithStatsVO> pageJobsWithPending(long current, long size, String ownerUserId, String keyword);

    /**
     *获取单个任务的审批概览
     */
     ImportJobOverviewVO getJobOverview(String jobId);
}
