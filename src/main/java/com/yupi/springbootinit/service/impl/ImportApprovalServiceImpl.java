package com.yupi.springbootinit.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.enums.ImportApplyStatus;
import com.yupi.springbootinit.enums.ImportItemValidStatus;
import com.yupi.springbootinit.enums.ImportJobStatus;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.mapper.*;
import com.yupi.springbootinit.model.dto.ImportApplyBatchDTO;
import com.yupi.springbootinit.model.dto.SiteSaveDTO;
import com.yupi.springbootinit.model.entity.ImportItemDO;
import com.yupi.springbootinit.model.entity.ImportJobDO;
import com.yupi.springbootinit.model.entity.SiteImportApplyDO;
import com.yupi.springbootinit.model.vo.CountryScopeVO;
import com.yupi.springbootinit.model.vo.ImportApplyVO;
import com.yupi.springbootinit.model.vo.ImportJobOverviewVO;
import com.yupi.springbootinit.model.vo.ImportJobWithStatsVO;
import com.yupi.springbootinit.service.ImportApprovalService;
import com.yupi.springbootinit.service.SiteService;
import com.yupi.springbootinit.utils.SqlLikeUtils;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@DS("dm8")
@Api(tags = "导入审批服务")
public class ImportApprovalServiceImpl implements ImportApprovalService {

    private final SiteImportApplyMapper importApplyMapper;
    private final SiteService siteService; // 复用现有的落主表逻辑
    private final ImportItemMapper itemMapper;
    private final ImportJobMapper jobMapper;

    private final SiteApprovalLogMapper approvalLogMapper;

    // 新增：字典/国家映射依赖（与普通审核一致）
    private final DictThemeMapper dictThemeMapper;
    private final CountryDictMapper countryDictMapper;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional(readOnly = true)
    public Page<ImportApplyVO> pagePendingByJob(String jobId, long current, long size) {
        if (!StringUtils.hasText(jobId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "jobId 不能为空");
        }
        Page<SiteImportApplyDO> page = new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size);
        Page<SiteImportApplyDO> poPage = importApplyMapper.selectPage(page,
                new LambdaQueryWrapper<SiteImportApplyDO>()
                        .eq(SiteImportApplyDO::getImportJobId, jobId)
                        .eq(SiteImportApplyDO::getStatus, ImportApplyStatus.PENDING.name())
                        .orderByAsc(SiteImportApplyDO::getCreatedAt));

        Page<ImportApplyVO> voPage = new Page<>(poPage.getCurrent(), poPage.getSize(), poPage.getTotal());
        voPage.setRecords(poPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(readOnly = true)
    public ImportApplyVO getDetail(String applyId) {
        SiteImportApplyDO po = importApplyMapper.selectById(applyId);
        if (po == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "申请不存在");
        return toVODetail(po);  // 详情返回富化后的 VO
    }

    @Override
    @Transactional(
            propagation = NOT_SUPPORTED)
    public int approveBatch(ImportApplyBatchDTO dto) {
        List<SiteImportApplyDO> list = loadTargets(dto);
        Set<String> touchedJobIds = list.stream().map(SiteImportApplyDO::getImportJobId).collect(Collectors.toSet());

        TransactionTemplate txNew =
                new TransactionTemplate(transactionManager);
        txNew.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);

        int success = 0;
        for (SiteImportApplyDO po : list) {
            if (!Objects.equals(po.getStatus(), ImportApplyStatus.PENDING.name())) continue;

            try {
                txNew.execute(status -> {
                    String siteId = siteService.saveSiteFromApproval(toSiteSaveDTO(po)).getId();

                    po.setStatus(ImportApplyStatus.APPROVED.name());
                    po.setReviewReason(StringUtils.hasText(dto.getReason()) ? dto.getReason() : null);
                    po.setReviewedAt(LocalDateTime.now());
                    po.setTargetSiteId(siteId);
                    importApplyMapper.updateById(po);

                    if (StringUtils.hasText(po.getImportItemId())) {
                        ImportItemDO it = itemMapper.selectById(po.getImportItemId());
                        if (it != null) {
                            it.setValidStatus(ImportItemValidStatus.SUBMITTED.name());
                            it.setValidMsg(null);  // 通过后不保留提示
                            itemMapper.updateById(it);
                        }
                    }
                    approvalLogMapper.logImport(po.getId(), "IMPORT_APPROVED",
                            StringUtils.hasText(dto.getReason()) ? dto.getReason() : "批量审批通过");
                    return null;
                });
                success++;
            } catch (RuntimeException ex) {
                final String readable = buildReadableMsg(ex); // ← 规范化异常消息
                txNew.execute(status -> {
                    // 审批记录：明确驳回原因（动作文案 + 业务原因）
                    String rr = (StringUtils.hasText(dto.getReason()) ? dto.getReason() : "批量驳回") + "；原因：" + readable;
                    po.setStatus(ImportApplyStatus.REJECTED.name());
                    po.setReviewReason(rr);
                    po.setReviewedAt(LocalDateTime.now());
                    importApplyMapper.updateById(po);
                    approvalLogMapper.logImport(po.getId(), "IMPORT_AUTO_REJECT", rr);

                    // 导入明细：给用户一个“可修改”的短提示（只放业务原因，不放动作文案）
                    if (StringUtils.hasText(po.getImportItemId())) {
                        ImportItemDO it = itemMapper.selectById(po.getImportItemId());
                        if (it != null) {
                            it.setValidStatus(ImportItemValidStatus.INVALID.name());
                            it.setValidMsg(cutLen(readable, 256)); // 避免提示过长
                            itemMapper.updateById(it);
                        }
                    }
                    return null;
                });
            }
        }
        touchedJobIds.forEach(this::refreshJobStatusByItems);
        return success;
    }


    @Override
    public int rejectBatch(ImportApplyBatchDTO dto) {
        List<SiteImportApplyDO> list = loadTargets(dto);
        Set<String> touchedJobIds = list.stream().map(SiteImportApplyDO::getImportJobId).collect(Collectors.toSet());
        int success = 0;
        for (SiteImportApplyDO po : list) {
            if (!Objects.equals(po.getStatus(), ImportApplyStatus.PENDING.name())) continue;
            po.setStatus(ImportApplyStatus.REJECTED.name());
            po.setReviewReason(StringUtils.hasText(dto.getReason()) ? dto.getReason() : "批量驳回");
            po.setReviewedAt(LocalDateTime.now());
            importApplyMapper.updateById(po);
            approvalLogMapper.logImport(po.getId(), "IMPORT_REJECTED", po.getReviewReason());
            // 解冻导入明细以便修改再提：置回 VALID；提示信息不复用审核结论
            if (StringUtils.hasText(po.getImportItemId())) {
                ImportItemDO it = itemMapper.selectById(po.getImportItemId());
                if (it != null) {
                    it.setValidStatus(ImportItemValidStatus.VALID.name());
                    it.setValidMsg(null);
                    itemMapper.updateById(it);
                }
            }
            success++;
        }
        touchedJobIds.forEach(this::refreshJobStatusByItems);
        return success;
    }

    // ==================== 新增：管理员任务聚合查询 ====================
    @Override
    @Transactional(readOnly = true)
    public Page<ImportJobWithStatsVO> pageJobsWithPending(long current, long size, String submitUserName, String keyword) {
        // 1) 按“提交人姓名”模糊匹配，先拿到待审申请
        String namePattern = SqlLikeUtils.likeContainsLiteral(submitUserName);
        List<SiteImportApplyDO> pendingApplies = importApplyMapper.selectList(
                new LambdaQueryWrapper<SiteImportApplyDO>()
                        .eq(SiteImportApplyDO::getStatus, ImportApplyStatus.PENDING.name())
                        .apply(StringUtils.hasText(submitUserName),
                                "SUBMIT_USER_NAME LIKE {0} ESCAPE '" + SqlLikeUtils.ESC + "'",
                                namePattern)
                        .orderByDesc(SiteImportApplyDO::getCreatedAt)
        );

        // 2) 唯一化 jobId（保持时间倒序出现顺序）
        LinkedHashSet<String> jobIds = pendingApplies.stream()
                .map(SiteImportApplyDO::getImportJobId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (jobIds.isEmpty()) {
            long c = current <= 0 ? 1 : current;
            long sz = size <= 0 ? 10 : size;
            return new Page<>(c, sz, 0);
        }

        // 3) 载入 Job，并按 jobIds 顺序稳定排序
        List<ImportJobDO> jobsRaw = jobMapper.selectBatchIds(jobIds);
        Map<String, ImportJobDO> id2job = jobsRaw.stream()
                .collect(Collectors.toMap(ImportJobDO::getId, j -> j, (a, b) -> a));
        List<ImportJobDO> jobsOrdered = jobIds.stream()
                .map(id2job::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 4) 关键字过滤（文件名/任务拥有者）
        List<ImportJobDO> filtered = jobsOrdered.stream().filter(j -> {
            if (!StringUtils.hasText(keyword)) return true;
            String k = keyword.trim();
            return (StringUtils.hasText(j.getFileName()) && j.getFileName().contains(k))
                    || (StringUtils.hasText(j.getOwnerUserName()) && j.getOwnerUserName().contains(k));
        }).collect(Collectors.toList());

        // 5) 手动分页
        long c = current <= 0 ? 1 : current;
        long sz = size <= 0 ? 10 : size;
        int from = (int) ((c - 1) * sz);
        int to = Math.min(from + (int) sz, filtered.size());
        List<ImportJobDO> pageJobs = (from >= to) ? Collections.emptyList() : filtered.subList(from, to);

        // 6) 组装 VO 并填充统计（关键：计数非 0）
        List<ImportJobWithStatsVO> records = pageJobs.stream()
                .map(this::toJobWithStats)     // 确保内部调用 fillStats
                .collect(Collectors.toList());

        Page<ImportJobWithStatsVO> page = new Page<>(c, sz, filtered.size());
        page.setRecords(records);
        return page;
    }


    @Override
    @Transactional(readOnly = true)
    public ImportJobOverviewVO getJobOverview(String jobId) {
        ImportJobDO job = jobMapper.selectById(jobId);
        if (job == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "导入任务不存在");
        ImportJobOverviewVO vo = new ImportJobOverviewVO();
        BeanUtils.copyProperties(job, vo);
        fillStats(job.getId(), vo);
        return vo;
    }

    /* ------------ 私有工具 ------------ */
    // 新增：详情富化
    private ImportApplyVO toVODetail(SiteImportApplyDO a) {
        ImportApplyVO vo = new ImportApplyVO();
        BeanUtils.copyProperties(a, vo);

        // 1) 解析 CSV
        List<String> themeIds = parseCsv(a.getThemeIdsText());
        List<String> scopeCodes = parseCsv(a.getScopesText());

        // 2) 批量字典映射
        Map<String, String> tid2name = themeIds.isEmpty()
                ? Collections.emptyMap()
                : dictThemeMapper.selectIdNameMapByIds(new HashSet<>(themeIds));

        Set<String> countryCodes = new HashSet<>(scopeCodes);
        if (StringUtils.hasText(a.getMainCountryCode())) {
            countryCodes.add(a.getMainCountryCode().trim().toUpperCase());
        }
        Map<String, Map<String, String>> code2names = countryCodes.isEmpty()
                ? Collections.emptyMap()
                : countryDictMapper.selectNameMapBatch(
                countryCodes.stream().map(s -> s.trim().toUpperCase()).collect(Collectors.toSet())
        );

        // 3) 主题名
        List<String> themeNames = themeIds.stream()
                .map(tid2name::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        vo.setThemeNames(themeNames);

        // 4) 主国家中英文名
        if (StringUtils.hasText(a.getMainCountryCode())) {
            Map<String, String> nm = code2names.get(a.getMainCountryCode().trim().toUpperCase());
            if (nm != null) {
                vo.setMainCountryNameZh(nm.get("ZH"));
                vo.setMainCountryNameEn(nm.get("EN"));
            }
        }

        // 5) 覆盖国家列表
        List<CountryScopeVO> scopes = new ArrayList<>(scopeCodes.size());
        for (String code : scopeCodes) {
            String c = StringUtils.hasText(code) ? code.trim().toUpperCase() : null;
            Map<String, String> nm = (c == null) ? null : code2names.get(c);
            CountryScopeVO one = CountryScopeVO.builder()
                    .siteId(null)
                    .countryCode(c)
                    .countryNameZh(nm == null ? null : nm.get("ZH"))
                    .countryNameEn(nm == null ? null : nm.get("EN"))
                    .build();
            scopes.add(one);
        }
        vo.setScopes(scopes);

        return vo;
    }

    private List<String> parseCsv(String s) {
        if (!StringUtils.hasText(s)) return Collections.emptyList();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    /** 取“用户可理解”的异常文本（BusinessException / 唯一约束 / 其它） */
    private String buildReadableMsg(Throwable ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        if (root instanceof BusinessException) {
            return Optional.ofNullable(root.getMessage()).orElse("业务校验失败");
        }
        if (root instanceof org.springframework.dao.DataIntegrityViolationException) {
            return "网站地址或名称已存在（唯一约束冲突）";
        }
        String msg = root.getMessage();
        return StringUtils.hasText(msg) ? msg : "入库失败";
    }

    private String cutLen(String s, int n) { return (s != null && s.length() > n) ? s.substring(0, n) : s; }

    private List<SiteImportApplyDO> loadTargets(ImportApplyBatchDTO dto) {
        if (!StringUtils.hasText(dto.getJobId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "jobId 不能为空");
        }
        List<SiteImportApplyDO> list;
        if (!CollectionUtils.isEmpty(dto.getApplyIds())) {
            list = dto.getApplyIds().stream()
                    .map(importApplyMapper::selectById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            list = importApplyMapper.selectList(new LambdaQueryWrapper<SiteImportApplyDO>()
                    .eq(SiteImportApplyDO::getImportJobId, dto.getJobId())
                    .eq(SiteImportApplyDO::getStatus, ImportApplyStatus.PENDING.name()));
        }
        return list;
    }

    private ImportApplyVO toVO(SiteImportApplyDO po) {
        ImportApplyVO vo = new ImportApplyVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }

    private SiteSaveDTO toSiteSaveDTO(SiteImportApplyDO po) {
        SiteSaveDTO s = new SiteSaveDTO();
        s.setId(null); // CREATE 或 UPDATE 由 saveSite 里的“更新策略”决定
        s.setSiteName(po.getSiteName());
        s.setUrl(po.getUrl());
        s.setMainCountryCode(po.getMainCountryCode());
        s.setThemeIds(splitCsv(po.getThemeIdsText()));
        s.setProvider(po.getProvider());
        s.setChannel(po.getChannel());
        s.setSummary(po.getSummary());
        s.setKeywordsText(po.getKeywordsText());
        s.setRemark(po.getRemark());
        s.setScopes(splitCsv(po.getScopesText()));
        return s;
    }

    private List<String> splitCsv(String s) {
        if (!StringUtils.hasText(s)) return null;
        return Arrays.stream(s.split(","))
                .map(String::trim).filter(StringUtils::hasText).distinct()
                .collect(Collectors.toList());
    }

    /**
     * 审批后刷新任务提交阶段状态：还有 VALID ⇒ PARTIAL；否则 DONE（保留 CANCELED/FAILED 原状态）
     */
    private void refreshJobStatusByItems(String jobId) {
        if (!StringUtils.hasText(jobId)) return;
        ImportJobDO job = jobMapper.selectById(jobId);
        if (job == null) return;
        ImportJobStatus js = ImportJobStatus.from(job.getStatus());
        if (ImportJobStatus.CANCELED.equals(js) || ImportJobStatus.FAILED.equals(js)) return;

        long total = itemMapper.selectCount(new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, jobId));
        long valid = itemMapper.selectCount(new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, jobId)
                .eq(ImportItemDO::getValidStatus, ImportItemValidStatus.VALID.name()));
        long pendingApplies = importApplyMapper.countLatestByJobAndStatus(
                jobId, ImportApplyStatus.PENDING.name());

        ImportJobStatus newStatus;
        if (total == 0) {
            newStatus = ImportJobStatus.NEW;
        } else if (pendingApplies > 0) {
            newStatus = ImportJobStatus.PARTIAL;
        } else if (valid > 0) {
            newStatus = ImportJobStatus.READY;
        } else {
            newStatus = ImportJobStatus.DONE;
        }

        job.setStatus(newStatus.name());
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
    }


    /**
     * 生成带统计字段的 Job VO
     */
    private ImportJobWithStatsVO toJobWithStats(ImportJobDO job) {
        ImportJobWithStatsVO vo = new ImportJobWithStatsVO();
        BeanUtils.copyProperties(job, vo);
        fillStats(job.getId(), vo);
        return vo;
    }

    /**
     * 根据 jobId 填充申请与明细的统计（VO 需包含对应字段）
     */
    private void fillStats(String jobId, Object targetVo) {
        // 申请统计
        long pending = importApplyMapper.countLatestByJobAndStatus(jobId, ImportApplyStatus.PENDING.name());
        long approved = importApplyMapper.countLatestByJobAndStatus(jobId, ImportApplyStatus.APPROVED.name());
        long rejected = importApplyMapper.countLatestByJobAndStatus(jobId, ImportApplyStatus.REJECTED.name());


        // 明细统计
        Long valid = itemMapper.selectCount(new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, jobId)
                .eq(ImportItemDO::getValidStatus, ImportItemValidStatus.VALID.name()));
        Long invalid = itemMapper.selectCount(new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, jobId)
                .eq(ImportItemDO::getValidStatus, ImportItemValidStatus.INVALID.name()));
        Long submitted = itemMapper.selectCount(new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, jobId)
                .eq(ImportItemDO::getValidStatus, ImportItemValidStatus.SUBMITTED.name()));

        if (targetVo instanceof ImportJobWithStatsVO) {
            ImportJobWithStatsVO v = (ImportJobWithStatsVO) targetVo;
            v.setAppliesPendingCount((int) pending);
            v.setAppliesApprovedCount((int) approved);
            v.setAppliesRejectedCount((int) rejected);
            v.setItemsValidCount(valid.intValue());
            v.setItemsInvalidCount(invalid.intValue());
            v.setItemsSubmittedCount(submitted.intValue());
            v.setHasPending(pending > 0);
        } else if (targetVo instanceof ImportJobOverviewVO) {
            ImportJobOverviewVO v = (ImportJobOverviewVO) targetVo;
            v.setAppliesPendingCount((int) pending);
            v.setAppliesApprovedCount((int) approved);
            v.setAppliesRejectedCount((int) rejected);
            v.setItemsValidCount(valid.intValue());
            v.setItemsInvalidCount(invalid.intValue());
            v.setItemsSubmittedCount(submitted.intValue());
            v.setHasPending(pending > 0);
        }
    }
}
