package com.yupi.springbootinit.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.enums.ImportApplyStatus;
import com.yupi.springbootinit.enums.ImportItemValidStatus;
import com.yupi.springbootinit.enums.ImportJobStatus;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.mapper.*;
import com.yupi.springbootinit.model.dto.*;
import com.yupi.springbootinit.model.entity.*;
import com.yupi.springbootinit.model.vo.ImportItemVO;
import com.yupi.springbootinit.model.vo.ImportJobVO;
import com.yupi.springbootinit.security.UserContext;
import com.yupi.springbootinit.security.UserContextHolder;
import com.yupi.springbootinit.service.DictThemeService;
import com.yupi.springbootinit.service.ImportApprovalService;
import com.yupi.springbootinit.service.SiteImportAsyncWorker;
import com.yupi.springbootinit.service.SiteImportService;
import com.yupi.springbootinit.utils.DataQualityUtils;
import com.yupi.springbootinit.utils.IdUtil;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 站点批量导入 - 服务实现
 * 解析Excel -> OS_IMPORT_JOB/ITEM -> 轻校验/去重 -> 统计
 * 提交：生成 SITE_IMPORT_APPLY（PENDING），不走普通申请表
 * <p>
 * 主题映射：在提交时将 THEME(名称) 映射为 主题ID（仅针对启用中的主题）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@Api(tags = "站点批量导入-服务实现")
@DS("dm8")
public class SiteImportServiceImpl implements SiteImportService {

    private final ImportJobMapper jobMapper;
    private final ImportItemMapper itemMapper;
    private final SiteImportApplyMapper importApplyMapper;
    private final DictThemeService dictThemeService; // ← 新增：主题字典服务（用于名称→ID 映射）
    private final CountryDictMapper countryDictMapper;
    private final SiteImportAsyncWorker asyncWorker;
    private final SiteApprovalLogMapper approvalLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ImportApprovalService importApprovalService;

    // =========================== 对外接口 ===========================

    @Override
    public ImportJobVO uploadAndParseAsync(MultipartFile file) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));

        // 1) 先建任务（NEW）
        ImportJobDO job = new ImportJobDO();
        job.setId(IdUtil.urlSafeUuid());
        job.setFileName(file.getOriginalFilename());
        job.setFileSize(file.getSize());
        job.setOwnerUserId(uc.getUserId());
        job.setOwnerUserName(uc.getUserName());
        job.setRowsTotal(0);
        job.setRowsReady(0);
        job.setRowsSkipped(0);
        job.setRowsDup(0);
        job.setStatus(ImportJobStatus.NEW.name());
        jobMapper.insert(job);

        // 2) 读取字节
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            job.setStatus(ImportJobStatus.FAILED.name());
            jobMapper.updateById(job);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件读取失败：" + e.getMessage());
        }

        log.info("[IMPORT] uploadAsync jobId={}, fileName={}, size={}, bytes.length={}",
                job.getId(), file.getOriginalFilename(), file.getSize(), bytes.length);

        //  关键改动：事务提交后再启动异步解析
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("[IMPORT] afterCommit, start async parse jobId={}", job.getId());
                asyncWorker.parseAndPersist(job.getId(), bytes);
            }
        });

        // 3) 立即返回 job 信息
        return toJobVO(job);
    }


    @Override
    @Transactional(readOnly = true)
    public ImportJobVO getJob(String jobId) {
        ImportJobDO job = jobMapper.selectById(jobId);
        if (job == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "导入任务不存在");
        return toJobVO(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImportItemVO> pageItems(ImportItemQueryDTO dto) {
        final long current = dto.getCurrent() == null ? 1L : dto.getCurrent();
        final long size = dto.getSize() == null ? 10L : dto.getSize();

        // 如果未指定审批状态，沿用你原有的“MP 分页 + 后置补全审批信息”的路径（简单高效）
        if (!StringUtils.hasText(dto.getApprovalStatus())) {
            LambdaQueryWrapper<ImportItemDO> qw = new LambdaQueryWrapper<ImportItemDO>()
                    .eq(ImportItemDO::getJobId, dto.getJobId());
            if (dto.getValidStatus() != null) {
                qw.eq(ImportItemDO::getValidStatus, dto.getValidStatus().name());
            }
            Page<ImportItemDO> page = new Page<>(current, size);
            Page<ImportItemDO> poPage = itemMapper.selectPage(page, qw.orderByAsc(ImportItemDO::getRowNo));

            List<ImportItemDO> items = poPage.getRecords();
            if (items.isEmpty()) {
                return new Page<>(poPage.getCurrent(), poPage.getSize(), poPage.getTotal());
            }

            List<String> itemIds = items.stream().map(ImportItemDO::getId).collect(Collectors.toList());
            Map<String, SimpleApplyStatusRow> latestMap = buildLatestMap(itemIds);

            Page<ImportItemVO> voPage = new Page<>(poPage.getCurrent(), poPage.getSize(), poPage.getTotal());
            voPage.setRecords(items.stream().map(po -> toItemVoWithLatest(po, latestMap.get(po.getId()))).collect(Collectors.toList()));
            return voPage;
        }

        // 指定了审批状态（含 NONE）=> 把“过滤 + 分页”上移到 Service
        // 1) 先取任务维度的所有 itemId（可选 validStatus），已按 row_no, id 排序
        final String valid = dto.getValidStatus() == null ? null : dto.getValidStatus().name();
        List<String> allIds = itemMapper.selectOrderedItemIdsByJob(dto.getJobId(), valid);
        if (allIds.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 2) 批量查询“最新审批状态”
        Map<String, SimpleApplyStatusRow> latestMap = buildLatestMap(allIds);

        // 3) 依据 approvalStatus 进行内存过滤（NONE = 不在 latestMap 中）
        final String target = dto.getApprovalStatus().trim().toUpperCase();
        List<String> filteredIds = allIds.stream().filter(id -> {
            SimpleApplyStatusRow st = latestMap.get(id);
            if ("NONE".equals(target)) return st == null;
            return st != null && target.equalsIgnoreCase(st.getApprovalStatus());
        }).collect(Collectors.toList());

        // 4) 计算分页窗口（保持与 MP Page 语义一致）
        long total = filteredIds.size();
        long fromIdx = Math.max(0, (current - 1) * size);
        long toIdx = Math.min(total, fromIdx + size);
        List<String> pageIds = fromIdx >= total ? Collections.emptyList() : filteredIds.subList((int) fromIdx, (int) toIdx);

        // 5) 取该页完整明细并组装 VO（再次按 row_no 排序保证稳定）
        List<ImportItemVO> records;
        if (pageIds.isEmpty()) {
            records = Collections.emptyList();
        } else {
            List<ImportItemDO> pageItems = itemMapper.selectItemsByIdsOrdered(pageIds);
            records = pageItems.stream()
                    .map(po -> toItemVoWithLatest(po, latestMap.get(po.getId())))
                    .collect(Collectors.toList());
        }

        Page<ImportItemVO> voPage = new Page<>(current, size, total);
        voPage.setRecords(records);
        return voPage;
    }



    public ImportJobVO commitItems(ImportCommitDTO dto) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));
        if (uc.isAdmin()) dto.setAutoApprove(true);
        ImportJobDO job = jobMapper.selectById(dto.getJobId());
        if (job == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "导入任务不存在");

        boolean autoApprove = Boolean.TRUE.equals(dto.getAutoApprove()); // 先按入参为准
        List<String> applyIdsForAutoApprove = new ArrayList<>();         // 收集本次提交产生/复用的 PENDING 申请

        // 任务状态校验（保持你原有口径）
        ImportJobStatus jobStatus = ImportJobStatus.from(job.getStatus());
        if (!ImportJobStatus.isSubmittable(jobStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前任务状态不可提交");
        }

        // 仅筛选“可提交”的明细：VALID & 非重复
        LambdaQueryWrapper<ImportItemDO> base = new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, job.getId())
                .eq(ImportItemDO::getValidStatus, ImportItemValidStatus.VALID.name())
                .eq(ImportItemDO::getDupFlag, 0);

        List<ImportItemDO> toCommit;
        if ("ALL_VALID".equalsIgnoreCase(dto.getMode()) || CollectionUtils.isEmpty(dto.getItemIds())) {
            toCommit = itemMapper.selectList(base);
        } else {
            toCommit = itemMapper.selectList(base.in(ImportItemDO::getId, dto.getItemIds()));
        }
        if (toCommit.isEmpty()) return toJobVO(job);

        // 主题名称 → ID 映射缓存
        Map<String, String> themeNameIdMap = buildThemeNameIdMap();

        int success = 0, failed = 0;
        for (ImportItemDO it : toCommit) {
            try {
                //  只在“已有 PENDING 申请”时跳过；REJECTED/APPROVED 允许再次提交
                long pendingCnt = importApplyMapper.selectCount(new LambdaQueryWrapper<SiteImportApplyDO>()
                        .eq(SiteImportApplyDO::getImportItemId, it.getId())
                        .eq(SiteImportApplyDO::getStatus, ImportApplyStatus.PENDING.name()));
                if (pendingCnt > 0) continue;

                Map<String, Object> row = readRowMap(it.getRawJson());

                String normalizedQuality = DataQualityUtils.normalizeOrDefault(
                        row.get("dataQuality") == null ? null : row.get("dataQuality").toString());
                if (normalizedQuality == null) {
                    it.setValidStatus(ImportItemValidStatus.INVALID.name());
                    it.setValidMsg("数据质量取值非法（仅支持：一般/重要/非常重要）");
                    itemMapper.updateById(it);
                    continue;
                }
                row.put("dataQuality", normalizedQuality);

                // 1) 主题名称→ID（仅当 themeIdsText 为空且存在 themeNamesText）
                String themeNamesText = trimToNull((String) row.get("themeNamesText"));
                if (StringUtils.hasText(themeNamesText) && !StringUtils.hasText((String) row.get("themeIdsText"))) {
                    ResolveResult rr = resolveThemeIdsFromNames(themeNamesText, themeNameIdMap);
                    if (!rr.unmatched.isEmpty()) {
                        it.setValidStatus(ImportItemValidStatus.INVALID.name());
                        it.setValidMsg("主题不存在或未启用：" + String.join(", ", rr.unmatched));
                        itemMapper.updateById(it);
                        continue;
                    }
                    row.put("themeIdsText", joinComma(rr.themeIds));
                }

                // 2) 国家代码校验（main + scopes）
                String mainCountry = trimToNull((String) row.get("mainCountryCode"));
                List<String> scopes = splitAndUpper((String) row.get("scopesText"));
                List<String> allCodes = new ArrayList<>();
                if (StringUtils.hasText(mainCountry)) {
                    allCodes.add(mainCountry.toUpperCase());
                }
                if (scopes != null) {
                    allCodes.addAll(scopes);
                }
                allCodes = allCodes.stream().distinct().collect(Collectors.toList());
                if (!allCodes.isEmpty()) {
                    int ok = countryDictMapper.countByCodes(allCodes);
                    if (ok != allCodes.size()) {
                        it.setValidStatus(ImportItemValidStatus.INVALID.name());
                        it.setValidMsg("存在非法国家代码：" + String.join(",", allCodes));
                        itemMapper.updateById(it);
                        continue;
                    }
                }

                // 3) 构建导入申请
                it.setRawJson(writeRowJson(row)); // 回写可能更新过的 themeIdsText
                itemMapper.updateById(it);

                // 先锁定该条目“最新一条申请”
                SiteImportApplyDO existing = importApplyMapper.selectLatestByItemForUpdate(it.getId());
                if (existing == null) {
                    SiteImportApplyDO apply = buildApplyFromRow(row, dto.getActionType(), job, it);
                    importApplyMapper.insert(apply);
                    approvalLogMapper.logImport(apply.getId(), "IMPORT_SUBMIT", "批量导入提交");
                    applyIdsForAutoApprove.add(apply.getId());
                } else if (ImportApplyStatus.PENDING.name().equals(existing.getStatus())) {
                    // 已有待审，跳过（避免重复提交）
                    applyIdsForAutoApprove.add(existing.getId());
                    continue;
                } else if (ImportApplyStatus.REJECTED.name().equals(existing.getStatus())) {
                    // 被驳回后“更新同一条记录”为 PENDING，并覆盖快照字段（不再新增）
                    existing.setActionType(StringUtils.hasText(dto.getActionType()) ? dto.getActionType() : existing.getActionType());
                    existing.setSiteName(trimToNull((String) row.get("siteName")));
                    existing.setUrl(trimToNull((String) row.get("url")));
                    existing.setMainCountryCode(trimToNull((String) row.get("mainCountryCode")));
                    existing.setThemeIdsText(trimToNull((String) row.get("themeIdsText")));
                    existing.setScopesText(trimToNull((String) row.get("scopesText")));
                    existing.setProvider(trimToNull((String) row.get("provider")));
                    existing.setChannel(trimToNull((String) row.get("channel")));
                    existing.setSummary(trimToNull((String) row.get("summary")));
                    existing.setKeywordsText(trimToNull((String) row.get("keywordsText")));
                    existing.setRemark(trimToNull((String) row.get("remark")));
                    String dataQuality = DataQualityUtils.normalizeOrDefault(trimToNull((String) row.get("dataQuality")));
                    existing.setDataQuality(dataQuality == null ? DataQualityUtils.QUALITY_NORMAL : dataQuality);
                    existing.setStatus(ImportApplyStatus.PENDING.name());
                    // 清理审核痕迹（用作重提）
                    existing.setReviewReason(null);
                    existing.setReviewedAt(null);
                    existing.setTargetSiteId(null);
                    existing.setUpdatedAt(LocalDateTime.now());
                    importApplyMapper.updateById(existing);
                    approvalLogMapper.logImport(existing.getId(), "IMPORT_RESUBMIT", "被驳回重新提交");
                    applyIdsForAutoApprove.add(existing.getId());
                } else if (ImportApplyStatus.APPROVED.name().equals(existing.getStatus())) {
                    // 已通过：该明细已归档为 SUBMITTED，不允许再次提交
                    continue;
                }
                //  关键：提交后将该明细置为 SUBMITTED（冻结），避免仍被统计为 VALID
                it.setValidStatus(ImportItemValidStatus.SUBMITTED.name());
                it.setValidMsg(null);
                itemMapper.updateById(it);
                success++;
            } catch (Exception ex) {
                failed++;
                log.warn("生成导入申请失败 itemId={}, err={}", it.getId(), ex.getMessage());
            }
        }

        // ：管理员直通 → 直接触发批量审批（逐条 REQUIRES_NEW，失败不影响其它）**
        if (autoApprove && !applyIdsForAutoApprove.isEmpty()) {
            ImportApplyBatchDTO abd = new ImportApplyBatchDTO();
            abd.setJobId(job.getId());
            abd.setApplyIds(applyIdsForAutoApprove);
            abd.setReason("管理员直通");

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        // 这里已经完成了外层事务的 COMMIT，审批能读到刚刚插入的申请
                        importApprovalService.approveBatch(abd);
                    } catch (Exception ex) {
                        // 这里失败不会回滚提交好的导入/申请数据，记录日志或落库做补偿
                        log.error("Auto-approve failed after commit, jobId={}, applyIds={}",
                                job.getId(), applyIdsForAutoApprove, ex);
                    }
                }
            });
        }

        //  统一口径：循环外一次性刷新任务计数与状态（“待审优先，其次 VALID”）
        refreshJobCountersAndStatus(job);

        log.info("提交完成：success={}, failed={}, autoApprove={}, applies={}",
                success, failed, autoApprove, applyIdsForAutoApprove.size());
        return toJobVO(job);
    }


    @Override
    public void cancelJob(String jobId) {
        ImportJobDO job = jobMapper.selectById(jobId);
        if (job == null) {
            return; // 幂等
        }

        ImportJobStatus jobStatus = ImportJobStatus.from(job.getStatus());
        // 仅允许 READY 取消（其他全部禁止）
        if (!ImportJobStatus.READY.equals(jobStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅允许在 READY 状态取消任务");
        }

        // —— 安全校验：确保没有任何已经跨权限的内容 —— //
        // 1) 不得存在待审申请
        long pendingApplies = importApplyMapper.countLatestByJobAndStatus(
                jobId, ImportApplyStatus.PENDING.name());
        if (pendingApplies > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在待审申请，不能取消任务");
        }

        // 2) 不得存在已通过申请
        long approvedApplies = importApplyMapper.countLatestByJobAndStatus(
                jobId, ImportApplyStatus.APPROVED.name());
        if (approvedApplies > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在已通过的申请，不能取消任务");
        }

        // 3) 不得存在已提交的明细（提交时会把明细标记为 SUBMITTED）
        long submittedItems = itemMapper.selectCount(new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, jobId)
                .eq(ImportItemDO::getValidStatus, ImportItemValidStatus.SUBMITTED.name()));
        if (submittedItems > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "检测到已提交的明细，不能取消任务");
        }

        // 满足条件：将任务标记为 CANCELED（不清理任何已存在的驳回记录/历史痕迹）
        job.setStatus(ImportJobStatus.CANCELED.name());
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ImportJobVO> pageMyJobs(ImportJobQueryDTO dto) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));

        long current = dto.getCurrent() == null || dto.getCurrent() < 1 ? 1 : dto.getCurrent();
        long size = dto.getSize() == null || dto.getSize() < 1 ? 10 : dto.getSize();

        LambdaQueryWrapper<ImportJobDO> qw = new LambdaQueryWrapper<ImportJobDO>()
                .eq(ImportJobDO::getOwnerUserId, uc.getUserId());
        if (dto.getStatus() != null) {
            qw.eq(ImportJobDO::getStatus, dto.getStatus().toValue());
        }
        Page<ImportJobDO> page = new Page<>(current, size);
        Page<ImportJobDO> poPage = jobMapper.selectPage(page, qw.orderByDesc(ImportJobDO::getCreatedAt));

        Page<ImportJobVO> voPage = new Page<>(poPage.getCurrent(), poPage.getSize(), poPage.getTotal());
        voPage.setRecords(poPage.getRecords().stream().map(SiteImportServiceImpl::toJobVO).collect(Collectors.toList()));
        return voPage;
    }


    @Override
    public ImportItemVO updateItem(ImportItemUpdateDTO dto) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));
        if (!StringUtils.hasText(dto.getItemId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "itemId 不能为空");
        }
        ImportItemDO it = itemMapper.selectById(dto.getItemId());
        if (it == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "明细不存在");

        ImportJobDO job = jobMapper.selectById(it.getJobId());
        if (job == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "导入任务不存在");
        if (!Objects.equals(job.getOwnerUserId(), uc.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权编辑该明细");
        }
        // 仅作废任务禁止编辑
        if (ImportJobStatus.CANCELED.name().equals(job.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该任务已作废，不允许编辑明细");
        }
        // 若该条目存在“待审申请”，禁止编辑
        long pending = importApplyMapper.selectCount(new LambdaQueryWrapper<SiteImportApplyDO>()
                .eq(SiteImportApplyDO::getImportItemId, it.getId())
                .eq(SiteImportApplyDO::getStatus, ImportApplyStatus.PENDING.name()));
        if (pending > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该条目正在审批中，暂不可编辑");
        }

        // 1) 合并 rawJson
        Map<String, Object> row = readRowMap(it.getRawJson());
        mergeIfPresent(row, "siteName", dto.getSiteName());
        mergeIfPresent(row, "url", dto.getUrl());
        mergeIfPresent(row, "mainCountryCode", upperOrNull(dto.getMainCountryCode()));
        mergeIfPresent(row, "provider", dto.getProvider());
        mergeIfPresent(row, "channel", dto.getChannel());
        mergeIfPresent(row, "summary", dto.getSummary());
        mergeIfPresent(row, "keywordsText", dto.getKeywordsText());
        mergeIfPresent(row, "remark", dto.getRemark());
        mergeIfPresent(row, "dataQuality", dto.getDataQuality());
        mergeIfPresent(row, "scopesText", normalizeCsvUpper(dto.getScopesText()));

        // 主题：可传名称串、也可直接给ID串；两者都给时以 ID 为准
        if (StringUtils.hasText(dto.getThemeIdsText())) {
            row.put("themeIdsText", dto.getThemeIdsText().trim());
        } else if (StringUtils.hasText(dto.getThemeNamesText())) {
            row.put("themeNamesText", joinComma(splitAndTrim(dto.getThemeNamesText())));
            // 不在此处做名称→ID 映射；提交时统一处理
        }

        // 2) 重新轻校验（必填、长度）
        List<String> errs = new ArrayList<>();
        String siteName = trimToNull((String) row.get("siteName"));
        String url = trimToNull((String) row.get("url"));
        String main = trimToNull((String) row.get("mainCountryCode"));
        String themeNamesText = trimToNull((String) row.get("themeNamesText"));
        String normalizedQuality = DataQualityUtils.normalizeOrDefault(
                row.get("dataQuality") == null ? null : row.get("dataQuality").toString());

        if (isBlank(siteName)) errs.add("网站名称为空");
        if (isBlank(url)) errs.add("网站地址为空");
        if (isBlank(main)) errs.add("主覆盖国家为空");
        if (isBlank(themeNamesText)) errs.add("国家主题为空");
        if (siteName != null && siteName.length() > 40) errs.add("名称长度>40");
        if (url != null && url.length() > 512) errs.add("URL长度>512");
        if (normalizedQuality == null) errs.add("数据质量取值非法（仅支持：一般/重要/非常重要）");

        if (normalizedQuality != null) {
            row.put("dataQuality", normalizedQuality);
        }

        it.setRawJson(writeRowJson(row));

        if (!errs.isEmpty()) {
            it.setValidStatus(ImportItemValidStatus.INVALID.name());
            it.setValidMsg(String.join("; ", errs));
        } else {
            it.setValidStatus(ImportItemValidStatus.VALID.name());
            it.setValidMsg(null);
        }

        // 3) 刷新“文件内 URL 重复”标志（与同 job 其它行比较）
        try {
            List<ImportItemDO> all = itemMapper.selectList(new LambdaQueryWrapper<ImportItemDO>()
                    .eq(ImportItemDO::getJobId, it.getJobId()));
            String u = url == null ? "" : url;
            boolean dup = all.stream()
                    .filter(x -> !x.getId().equals(it.getId()))
                    .map(x -> {
                        Map<String, Object> m = readRowMap(x.getRawJson());
                        return trimToNull((String) m.get("url"));
                    })
                    .filter(Objects::nonNull)
                    .anyMatch(u::equals);
            it.setDupFlag(dup ? 1 : 0);
        } catch (Exception ex) {
            log.warn("刷新重复标记失败 itemId={}, err={}", it.getId(), ex.getMessage());
        }
        itemMapper.updateById(it);
        // 新增：编辑完成后根据最新明细重算任务状态（可能把 DONE 拉回 READY）
        refreshJobCountersAndStatus(job);
        return toItemVO(it);
    }

    @Override
    public void deleteItem(String itemId) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));
        if (!StringUtils.hasText(itemId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "itemId 不能为空");
        }
        ImportItemDO it = itemMapper.selectById(itemId);
        if (it == null) {
            // 幂等：直接返回
            return;
        }
        ImportJobDO job = jobMapper.selectById(it.getJobId());
        if (job == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "导入任务不存在");
        }
        // 仅任务拥有者可删
        if (!Objects.equals(job.getOwnerUserId(), uc.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该明细");
        }
        if (ImportJobStatus.CANCELED.name().equals(job.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该任务已作废，不允许删除明细");
        }
        // 若条目存在“待审申请”，禁止删除（保留原逻辑）
        long pending = importApplyMapper.selectCount(new LambdaQueryWrapper<SiteImportApplyDO>()
                .eq(SiteImportApplyDO::getImportItemId, it.getId())
                .eq(SiteImportApplyDO::getStatus, ImportApplyStatus.PENDING.name()));
        if (pending > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该条目正在审批中，暂不可删除");
        }
        // 已提交（SUBMITTED）禁止删除（保留原逻辑）
        if (ImportItemValidStatus.SUBMITTED.name().equals(it.getValidStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已提交的明细不允许删除");
        }
        // —— 新增：清理该 item 的导入审批记录（只可能是 REJECTED/历史快照），先记日志再删 —— //
        List<SiteImportApplyDO> applies = importApplyMapper.selectList(
                new LambdaQueryWrapper<SiteImportApplyDO>()
                        .eq(SiteImportApplyDO::getImportItemId, it.getId()));
        if (!applies.isEmpty()) {
            for (SiteImportApplyDO a : applies) {
                // 审计留痕：记录“因删除明细清理申请”的动作
                approvalLogMapper.logImport(a.getId(), "IMPORT_APPLY_PURGE", "用户删除导入明细，清理关联导入申请");
            }
            importApplyMapper.deleteByImportItemId(it.getId());
        }

        // 删除该条明细
        itemMapper.deleteById(itemId);
        // 删除后：刷新重复标记（URL 维度） & 任务计数/状态
        refreshDupFlags(it.getJobId());
        refreshJobCountersAndStatus(job);
    }


    // =========================== 私有：业务装配 / 映射 ===========================

    /**
     * 删除/编辑后重算重复标记（同一 job 内 URL 相同即视为重复）
     */
    private void refreshDupFlags(String jobId) {
        List<ImportItemDO> list = itemMapper.selectList(
                new LambdaQueryWrapper<ImportItemDO>().eq(ImportItemDO::getJobId, jobId)
        );
        // 分组：按 URL（rawJson.url），空值归为空串
        Map<String, List<ImportItemDO>> groups = new HashMap<>();
        for (ImportItemDO x : list) {
            String url = null;
            try {
                Map<String, Object> m = readRowMap(x.getRawJson());
                url = trimToNull((String) m.get("url"));
            } catch (Exception ignore) {
            }
            String key = (url == null ? "" : url);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(x);
        }
        // 组内数量>1 即标记 dupFlag=1，否则 0
        for (List<ImportItemDO> g : groups.values()) {
            int flag = g.size() > 1 ? 1 : 0;
            for (ImportItemDO x : g) {
                if (x.getDupFlag() == null || x.getDupFlag() != flag) {
                    x.setDupFlag(flag);
                    itemMapper.updateById(x);
                }
            }
        }
    }

    /**
     * 刷新任务四个计数与状态：
     * rowsTotal、rowsReady(VALID&非重复)、rowsDup、rowsSkipped(= total - ready - submitted)
     * 状态规则：
     * - 若 submitted>0：ready>0 => PARTIAL；否则 DONE
     * - 若 submitted=0：total=0 => NEW；否则 READY
     */
    private void refreshJobCountersAndStatus(ImportJobDO job) {
        String jobId = job.getId();

        long total = itemMapper.selectCount(new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, jobId));
        long ready = itemMapper.selectCount(new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, jobId)
                .eq(ImportItemDO::getValidStatus, ImportItemValidStatus.VALID.name())
                .eq(ImportItemDO::getDupFlag, 0));
        long submitted = itemMapper.selectCount(new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, jobId)
                .eq(ImportItemDO::getValidStatus, ImportItemValidStatus.SUBMITTED.name()));
        long dup = itemMapper.selectCount(new LambdaQueryWrapper<ImportItemDO>()
                .eq(ImportItemDO::getJobId, jobId)
                .eq(ImportItemDO::getDupFlag, 1));
        long skipped = Math.max(0, total - ready - submitted);

        // 仅统计“每条明细的最新一条”为 PENDING 的数量
        long pendingApplies = importApplyMapper.countLatestByJobAndStatus(
                jobId, ImportApplyStatus.PENDING.name());

        job.setRowsTotal((int) total);
        job.setRowsReady((int) ready);
        job.setRowsDup((int) dup);
        job.setRowsSkipped((int) skipped);

        ImportJobStatus newStatus;
        if (total == 0) {
            newStatus = ImportJobStatus.NEW;
        } else if (pendingApplies > 0) {
            newStatus = ImportJobStatus.PARTIAL;
        } else if (ready > 0) {
            newStatus = ImportJobStatus.READY;
        } else {
            newStatus = ImportJobStatus.DONE;
        }

        job.setStatus(newStatus.name());
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
    }


    private Map<String, Object> readRowMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "RAW_JSON 解析失败：" + e.getMessage());
        }
    }

    private String writeRowJson(Map<String, Object> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "RAW_JSON 写入失败：" + e.getMessage());
        }
    }

    private SiteImportApplyDO buildApplyFromRow(Map<String, Object> row, String actionType, ImportJobDO job, ImportItemDO it) {
        SiteImportApplyDO po = new SiteImportApplyDO();
        po.setId(IdUtil.urlSafeUuid());
        po.setActionType(StringUtils.hasText(actionType) ? actionType : "CREATE");
        po.setSiteName(trimToNull((String) row.get("siteName")));
        po.setUrl(trimToNull((String) row.get("url")));
        po.setMainCountryCode(trimToNull((String) row.get("mainCountryCode")));
        po.setThemeIdsText(trimToNull((String) row.get("themeIdsText")));
        po.setScopesText(trimToNull((String) row.get("scopesText")));
        po.setProvider(trimToNull((String) row.get("provider")));
        po.setChannel(trimToNull((String) row.get("channel")));
        po.setSummary(trimToNull((String) row.get("summary")));
        po.setKeywordsText(trimToNull((String) row.get("keywordsText")));
        po.setRemark(trimToNull((String) row.get("remark")));
        String dataQuality = DataQualityUtils.normalizeOrDefault(
                row.get("dataQuality") == null ? null : row.get("dataQuality").toString());
        po.setDataQuality(dataQuality == null ? DataQualityUtils.QUALITY_NORMAL : dataQuality);

        po.setStatus(ImportApplyStatus.PENDING.name());
        po.setSubmitUserId(job.getOwnerUserId());
        po.setSubmitUserName(job.getOwnerUserName());
        po.setImportJobId(job.getId());
        po.setImportItemId(it.getId());
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        return po;
    }

    /**
     * 构建 主题名称(规范化后) -> 主题ID 的映射，仅取启用主题
     */
    private Map<String, String> buildThemeNameIdMap() {
        List<DictThemeDO> enabled = dictThemeService.listEnabled();
        Map<String, String> map = new HashMap<>(enabled.size() * 2);
        for (DictThemeDO t : enabled) {
            String key = normalizeName(t.getName());
            if (StringUtils.hasText(key)) {
                map.put(key, t.getId());
            }
        }
        return map;
    }

    /**
     * 将主题名称串映射为主题ID列表；返回匹配ID与未匹配名称集合
     */
    private ResolveResult resolveThemeIdsFromNames(String themeNamesText, Map<String, String> nameIdMap) {
        List<String> names = splitAndTrim(themeNamesText);
        List<String> ids = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        // 去重保序
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String n : names) {
            String norm = normalizeName(n);
            if (!seen.add(norm)) continue;
            String id = nameIdMap.get(norm);
            if (id != null) {
                ids.add(id);
            } else {
                unmatched.add(n);
            }
        }
        return new ResolveResult(ids, unmatched);
    }

    /** 将 ImportItemDO 与最新审批状态行合并为 VO */
    private ImportItemVO toItemVoWithLatest(ImportItemDO po, SimpleApplyStatusRow latest) {
        ImportItemVO vo = toItemVO(po); // 你已有方法
        if (latest == null) {
            vo.setApprovalStatus("NONE");
            vo.setApprovalApplyId(null);
            vo.setApprovalReason(null);
            vo.setApprovalReviewedAt(null);
        } else {
            vo.setApprovalStatus(latest.getApprovalStatus());
            vo.setApprovalApplyId(latest.getApplyId());
            vo.setApprovalReason(latest.getApprovalReason());
            vo.setApprovalReviewedAt(latest.getApprovalReviewedAt());
        }
        return vo;
    }

    /** 批量查询最新状态并构建 Map<itemId, SimpleApplyStatusRow> */
    private Map<String, SimpleApplyStatusRow> buildLatestMap(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return Collections.emptyMap();
        List<SimpleApplyStatusRow> rows = importApplyMapper.selectLatestStatusByItemIds(itemIds);
        if (rows == null || rows.isEmpty()) return Collections.emptyMap();
        return rows.stream().collect(Collectors.toMap(SimpleApplyStatusRow::getImportItemId, x -> x, (a, b) -> a));
    }

    private static String normalizeName(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }

    // =========================== 工具方法 ===========================

    private static String trimToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 使用逗号拼接列表；空列表返回 null，避免出现空字符串
     */
    private static String joinComma(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }

    private static String upperOrNull(String s) {
        return isBlank(s) ? null : s.trim().toUpperCase();
    }

    /**
     * 逗号分割并去空格/去空（保持大小写）；兼容中文逗号
     */
    private static List<String> splitAndTrim(String s) {
        if (isBlank(s)) return Collections.emptyList();
        s = s.replace('，', ',');
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 逗号分割并转大写、去空、去重；兼容中文逗号
     */
    private static List<String> splitAndUpper(String s) {
        if (isBlank(s)) return Collections.emptyList();
        s = s.replace('，', ',');
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(String::toUpperCase).distinct().collect(Collectors.toList());
    }

    private static ImportItemVO toItemVO(ImportItemDO po) {
        ImportItemVO vo = new ImportItemVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }

    private static ImportJobVO toJobVO(ImportJobDO po) {
        ImportJobVO vo = new ImportJobVO();
        BeanUtils.copyProperties(po, vo);
        return vo;
    }

    private static void mergeIfPresent(Map<String, Object> row, String key, String val) {
        if (val != null) row.put(key, trimToNull(val));
    }

    private static String normalizeCsvUpper(String s) {
        if (isBlank(s)) return null;
        return String.join(",", splitAndUpper(s));
    }


    // =========================== 小容器类 ===========================

    /**
     * 主题名称→ID 映射结果容器
     */
    private static class ResolveResult {
        final List<String> themeIds;
        final List<String> unmatched;

        ResolveResult(List<String> themeIds, List<String> unmatched) {
            this.themeIds = themeIds;
            this.unmatched = unmatched;
        }
    }
}
