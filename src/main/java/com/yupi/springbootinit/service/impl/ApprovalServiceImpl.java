package com.yupi.springbootinit.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.enums.ApplyAction;
import com.yupi.springbootinit.enums.ApplyStatus;
import com.yupi.springbootinit.enums.ReviewedExistenceStatus;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.mapper.*;
import com.yupi.springbootinit.model.dto.SiteSaveDTO;
import com.yupi.springbootinit.model.entity.OsSiteDO;
import com.yupi.springbootinit.model.entity.SiteApplyDO;
import com.yupi.springbootinit.model.entity.SiteApprovalLogDO;
import com.yupi.springbootinit.model.vo.*;
import com.yupi.springbootinit.security.UserContext;
import com.yupi.springbootinit.security.UserContextHolder;
import com.yupi.springbootinit.service.ApprovalService;
import com.yupi.springbootinit.service.SiteService;
import com.yupi.springbootinit.utils.DataQualityUtils;
import com.yupi.springbootinit.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@DS("dm8")
public class ApprovalServiceImpl extends ServiceImpl<SiteApplyMapper,SiteApplyDO> implements ApprovalService {

    private final SiteApplyMapper siteApplyMapper;
    private final SiteApprovalLogMapper approvalLogMapper;
    private final CountryDictMapper countryDictMapper;
    private final DictThemeMapper dictThemeMapper;
    private final SiteService siteService; // 用于落正式表

    private final OsSiteMapper osSiteMapper;

    @Override
    public String submitApply(SiteSaveDTO dto) {
        // 0) 登录校验
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));

        // 1) 基本参数校验与标准化
        if (!StringUtils.hasText(dto.getSiteName()) || !StringUtils.hasText(dto.getUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "站点名称与URL必填");
        }
        String siteName = dto.getSiteName().trim();
        String normUrl = UrlUtils.normalizeUrl(dto.getUrl()); // 统一规范化
        if (siteName.length() > 40) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "站点名称过长");
        }
        if (normUrl.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "网站地址过长");
        }
        if (!StringUtils.hasText(dto.getMainCountryCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "主覆盖国家必填");
        }
        String mainCode = dto.getMainCountryCode().trim().toUpperCase();
        // 允许 ALL（全球），否则必须是 2 位
        if (!"ALL".equalsIgnoreCase(mainCode) && mainCode.length() != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "主覆盖国家代码必须是'ALL'或2位（ISO-3166-1 alpha-2）");
        }


        // 2) 主题合法性（如传） —— 修正：用去重+非空后的 themeIds 来比对数量
        List<String> themeIds = null;
        if (!CollectionUtils.isEmpty(dto.getThemeIds())) {
            themeIds = dto.getThemeIds().stream()
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());
            int cnt = dictThemeMapper.countByIds(themeIds);
            if (cnt != themeIds.size()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在非法主题领域ID");
            }
        }

        // 3) 覆盖国家规范化与合法性（如传）
        List<String> scopes = Optional.ofNullable(dto.getScopes()).orElse(Collections.emptyList())
                .stream().filter(StringUtils::hasText)
                .map(s -> s.trim().toUpperCase())
                .distinct().collect(Collectors.toList());

        // 主覆盖国家合法
        if (countryDictMapper.countByCodes(Collections.singletonList(mainCode)) == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "主覆盖国家非法");
        }

        // 覆盖国家合法
        if (!scopes.isEmpty()) {
            int c = countryDictMapper.countByCodes(scopes);
            if (c != scopes.size()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在非法覆盖国家");
        }

        // 归一化：ALL 作为通配
        if ("ALL".equals(mainCode)) {
            scopes = new ArrayList<>();
            scopes.add("ALL");
        } else if (scopes.contains("ALL")) {
            scopes = new ArrayList<>();
            scopes.add("ALL");
        } else {
            // 普通场景：确保包含主覆盖
            if (!scopes.contains(mainCode)) {
                scopes.add(mainCode);
            }
        }

        // 4) 判断动作（关键改动）：以“数据库是否存在该ID”为准，禁止“自定义ID新增”
        boolean isUpdate = false;
        String targetSiteId;
        OsSiteDO exists = null;
        if (StringUtils.hasText(dto.getId())) {
            exists = siteService.getById(dto.getId());
            if (exists != null && (exists.getIsDelete() == null || exists.getIsDelete() == 0)) {
                // 确认是编辑
                isUpdate = true;
                targetSiteId = dto.getId();
            } else if (exists == null) {
                // 携带了一个不存在的ID → 禁止“自定义ID新增”
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止指定不存在的ID进行新增，请移除ID后再提交");
            } else {
                // 目标被逻辑删除，不允许在其上编辑
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "编辑目标站点不存在或已删除");
            }
        } else {
            // 新增：后端生成新ID
            targetSiteId = genId();
        }
        final String actionType = isUpdate ? ApplyAction.UPDATE.name() : ApplyAction.CREATE.name();

        // 5) 编辑场景：防重复提交同一站点的编辑申请
        if (isUpdate && siteApplyMapper.countPendingUpdateBySite(targetSiteId) > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该站点已存在待审编辑申请");
        }

        String dataQualityCandidate =
                !StringUtils.hasText(dto.getDataQuality()) && isUpdate
                        ? exists.getDataQuality()
                        : dto.getDataQuality();
        String dataQuality = DataQualityUtils.normalizeOrDefault(dataQualityCandidate);
        if (dataQuality == null) {
            if (StringUtils.hasText(dto.getDataQuality())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据质量仅支持：一般/重要/非常重要");
            }
            dataQuality = DataQualityUtils.QUALITY_NORMAL;
        }

        // 6) 唯一性防御
        if (!isUpdate) {
            // 新增：未删除站点中 URL/名称 不允许重复
            LambdaQueryWrapper<OsSiteDO> uq = Wrappers.<OsSiteDO>lambdaQuery()
                    .eq(OsSiteDO::getIsDelete, 0)
                    .and(w -> w.eq(OsSiteDO::getUrl, normUrl).or().eq(OsSiteDO::getSiteName, siteName));
            if (siteService.count(uq) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "已存在相同网址或名称的站点（未删除），请勿重复提交");
            }
            // 新增：防重复申请（PENDING 同URL或同名称）
            int pendDup = siteApplyMapper.countPendingCreateByUrlOrName(normUrl, siteName);
            if (pendDup > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已存在相同网址或名称的待审申请，请勿重复提交");
            }
        } else {
            // 编辑：未删除站点中 URL/名称 不允许与“非自身”冲突
            LambdaQueryWrapper<OsSiteDO> uqEdit = Wrappers.<OsSiteDO>lambdaQuery()
                    .eq(OsSiteDO::getIsDelete, 0)
                    .and(w -> w.eq(OsSiteDO::getUrl, normUrl).or().eq(OsSiteDO::getSiteName, siteName))
                    .ne(OsSiteDO::getId, targetSiteId);
            if (siteService.count(uqEdit) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改后的网址或名称与其他站点冲突");
            }
        }

        // 7) 生成申请ID（新增时 targetSiteId 已在上面生成；编辑则是既有ID）
        String applyId = genId();

        // 8) 组装并入库申请单（PENDING）
        SiteApplyDO row = SiteApplyDO.builder()
                .id(applyId)
                .targetSiteId(targetSiteId)
                .actionType(actionType)
                .siteName(siteName)
                .url(normUrl)
                .provider(StringUtils.hasText(dto.getProvider()) ? dto.getProvider().trim() : null)
                .channel(StringUtils.hasText(dto.getChannel()) ? dto.getChannel().trim() : null)
                .summary(StringUtils.hasText(dto.getSummary()) ? dto.getSummary().trim() : null)
                .keywordsText(StringUtils.trimWhitespace(Objects.toString(dto.getKeywordsText(), "")))
                .remark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null)
                .dataQuality(dataQuality)
                .mainCountryCode(mainCode)
                .themeIdsText(themeIds == null ? null : String.join(",", themeIds))
                .scopeCountryCodesText(scopes.isEmpty() ? null : String.join(",", scopes))
                .status(ApplyStatus.PENDING.name())
                .submitUserId(uc.getUserId())
                .submitUserName(uc.getUserName())
                .build();
        siteApplyMapper.insert(row);

        // 9) 记录申请日志（你已有的工具方法）
        logApproval(applyId, ApplyAction.SUBMIT.name(), isUpdate ? "用户提交编辑申请" : "用户提交创建申请");

        return applyId;
    }

    @Override
    public String approve(String applyId, String remark) {
        // 0) 管理员校验
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));
        if (!uc.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        // 1) 行级锁读取，确保并发安全；仅 PENDING 可审批
        SiteApplyDO apply = siteApplyMapper.selectByIdForUpdate(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请不存在");
        }
        if (!ApplyStatus.PENDING.name().equals(apply.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该申请已被他人处理");
        }

        // 2) 组装 DTO
        SiteSaveDTO dto = new SiteSaveDTO();
        final boolean isUpdate = ApplyAction.UPDATE.name().equals(apply.getActionType());
        final String submitUserId = apply.getSubmitUserId();
        final String reviewerUserId = uc.getUserId();
        dto.setId(apply.getTargetSiteId());
        dto.setSiteName(apply.getSiteName());
        dto.setUrl(apply.getUrl());
        dto.setProvider(apply.getProvider());
        dto.setChannel(apply.getChannel());
        dto.setSummary(apply.getSummary());
        dto.setKeywordsText(apply.getKeywordsText());
        dto.setRemark(apply.getRemark());
        dto.setDataQuality(apply.getDataQuality());
        dto.setMainCountryCode(apply.getMainCountryCode());
        dto.setThemeIds(parseCsv(apply.getThemeIdsText()));
        dto.setScopes(parseCsv(apply.getScopeCountryCodesText()));

        // 3) 审批落库前的最终防御性校验
        if (isUpdate) {
            // 编辑：目标站点仍需存在且未删除（提交到审批期间可能被删除）
            OsSiteDO target = siteService.getById(apply.getTargetSiteId());
            if (target == null || (target.getIsDelete() != null && target.getIsDelete() == 1)) {
                // 自动驳回并记录
                siteApplyMapper.updateReviewIfStatus(applyId, ApplyStatus.PENDING.name(), ApplyStatus.REJECTED.name(), uc.getUserId(), uc.getUserName(), "目标站点不存在或已删除");
                logApproval(applyId, ApplyStatus.REJECTED.name(), "审批自动驳回：目标站点不存在或已删除");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "目标站点不存在或已删除");
            }
            // 编辑：最终唯一性检查（未删除、非自身）
            LambdaQueryWrapper<OsSiteDO> uqEdit = Wrappers.<OsSiteDO>lambdaQuery()
                    .eq(OsSiteDO::getIsDelete, 0)
                    .and(w -> w.eq(OsSiteDO::getUrl, apply.getUrl())
                            .or().eq(OsSiteDO::getSiteName, apply.getSiteName()))
                    .ne(OsSiteDO::getId, apply.getTargetSiteId());
            if (siteService.count(uqEdit) > 0) {
                siteApplyMapper.updateReviewIfStatus(applyId, ApplyStatus.PENDING.name(), ApplyStatus.REJECTED.name(), uc.getUserId(), uc.getUserName(), "修改后的网址或名称与其他站点冲突");
                logApproval(applyId, ApplyStatus.REJECTED.name(), "审批自动驳回：修改后的网址或名称与其他站点冲突");
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改后的网址或名称与其他站点冲突");
            }
        } else {
            // 新增：最终唯一性检查（未删除）
            LambdaQueryWrapper<OsSiteDO> uq = Wrappers.<OsSiteDO>lambdaQuery()
                    .eq(OsSiteDO::getIsDelete, 0)
                    .and(w -> w.eq(OsSiteDO::getUrl, apply.getUrl())
                            .or().eq(OsSiteDO::getSiteName, apply.getSiteName()));
            if (siteService.count(uq) > 0) {
                siteApplyMapper.updateReviewIfStatus(applyId, ApplyStatus.PENDING.name(), ApplyStatus.REJECTED.name(), uc.getUserId(), uc.getUserName(), "网站地址或名称已存在");
                logApproval(applyId, ApplyStatus.REJECTED.name(), "审批自动驳回：网站地址或名称已存在");
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "网站地址或名称已存在");
            }
        }

        // 4) 尝试落正式表（可能抛出 BusinessException）
        try {
            SiteVO vo = siteService.saveSiteFromApproval(
                    dto,
                    submitUserId,
                    isUpdate ? reviewerUserId : null
            );

            // 5) 审批通过 & 记录日志
            siteApplyMapper.updateReviewIfStatus(applyId, ApplyStatus.PENDING.name(), ApplyStatus.APPROVED.name(), uc.getUserId(), uc.getUserName(), StringUtils.hasText(remark) ? remark : "同意审批");
            logApproval(applyId, ApplyStatus.APPROVED.name(), "审批通过：" + (remark == null ? "" : remark));

            // 新增返回新站点ID；编辑返回原站点ID
            return isUpdate ? apply.getTargetSiteId() : vo.getId();

        } catch (BusinessException be) {
            // 业务异常：自动驳回并记录原因（保持单据闭环）
            siteApplyMapper.updateReviewIfStatus(applyId, ApplyStatus.PENDING.name(), ApplyStatus.REJECTED.name(), uc.getUserId(), uc.getUserName(), be.getMessage());
            logApproval(applyId, ApplyStatus.REJECTED.name(), "审批自动驳回（业务校验失败）：" + be.getMessage());
            throw be; // 继续抛出给前端
        }
        // 其它系统异常不捕获：事务整体回滚，申请仍为 PENDING，便于后续人工处理或重试
    }


    @Override
    public void reject(String applyId, String reason) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));
        if (!uc.isAdmin()) throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");

        // 行级锁，避免并发修改
        SiteApplyDO apply = siteApplyMapper.selectByIdForUpdate(applyId);
        if (apply == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请不存在");
        if (!ApplyStatus.PENDING.name().equals(apply.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该申请已被他人处理");
        }
        siteApplyMapper.updateReviewIfStatus(applyId, ApplyStatus.PENDING.name(), ApplyStatus.REJECTED.name(), uc.getUserId(), uc.getUserName(), reason);
        logApproval(applyId, ApplyStatus.REJECTED.name(), "审批驳回：" + (reason == null ? "" : reason));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteApplyVO> pending() {
        // 拉取 PENDING 申请，按创建时间倒序
        List<SiteApplyDO> rows = siteApplyMapper.selectList(
                Wrappers.<SiteApplyDO>lambdaQuery()
                        .eq(SiteApplyDO::getStatus, ApplyStatus.PENDING.name())
                        .orderByDesc(SiteApplyDO::getCreatedAt)
        );
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        // 1) 收集本页所有需要的 themeId 与 国家 code（主覆盖 + scopes）
        Set<String> allThemeIds = new HashSet<>();
        Set<String> allCountryCodes = new HashSet<>();
        for (SiteApplyDO a : rows) {
            allThemeIds.addAll(parseCsv(a.getThemeIdsText()));
            if (StringUtils.hasText(a.getMainCountryCode())) {
                allCountryCodes.add(a.getMainCountryCode().trim().toUpperCase());
            }
            allCountryCodes.addAll(
                    parseCsv(a.getScopeCountryCodesText())
                            .stream().map(s -> s.trim().toUpperCase()).collect(Collectors.toList())
            );
        }

        // 2) 一次性批量拉取：主题名、国家名
        Map<String, String> themeNameMap = allThemeIds.isEmpty()
                ? Collections.emptyMap()
                : dictThemeMapper.selectIdNameMapByIds(allThemeIds);
        Map<String, Map<String, String>> countryNameMap = allCountryCodes.isEmpty()
                ? Collections.emptyMap()
                : countryDictMapper.selectNameMapBatch(allCountryCodes);

        // 3) 组装 VO（均为内存映射，无额外 SQL）
        List<SiteApplyVO> result = new ArrayList<>(rows.size());
        for (SiteApplyDO a : rows) {
            // 主题
            List<String> themeIds = parseCsv(a.getThemeIdsText());
            List<String> themeNames = themeIds.stream()
                    .map(themeNameMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 主覆盖国家名称
            String mainZh = null, mainEn = null;
            if (StringUtils.hasText(a.getMainCountryCode())) {
                Map<String, String> nm = countryNameMap.get(a.getMainCountryCode().trim().toUpperCase());
                if (nm != null) {
                    mainZh = nm.get("ZH");
                    mainEn = nm.get("EN");
                }
            }

            // 覆盖范围
            List<CountryScopeVO> scopes = parseCsv(a.getScopeCountryCodesText())
                    .stream()
                    .map(code -> {
                        String cc = code.trim().toUpperCase();
                        Map<String, String> nm = countryNameMap.get(cc);
                        return CountryScopeVO.builder()
                                .siteId(null)
                                .countryCode(cc)
                                .countryNameZh(nm == null ? null : nm.get("ZH"))
                                .countryNameEn(nm == null ? null : nm.get("EN"))
                                .build();
                    })
                    .collect(Collectors.toList());

            String dq = DataQualityUtils.normalizeOrDefault(a.getDataQuality());
            SiteApplyVO vo = SiteApplyVO.builder()
                    .id(a.getId())
                    .targetSiteId(a.getTargetSiteId())
                    .actionType(a.getActionType())
                    .siteName(a.getSiteName())
                    .url(a.getUrl())
                    .provider(a.getProvider())
                    .channel(a.getChannel())
                    .summary(a.getSummary())
                    .keywordsText(a.getKeywordsText())
                    .remark(a.getRemark())
                    .dataQuality(dq == null ? DataQualityUtils.QUALITY_NORMAL : dq)
                    .mainCountryCode(a.getMainCountryCode())
                    .mainCountryNameZh(mainZh)
                    .mainCountryNameEn(mainEn)
                    .themeIds(themeIds)
                    .themeNames(themeNames)
                    .scopes(scopes)
                    .status(a.getStatus())
                    .submitUserId(a.getSubmitUserId())
                    .submitUserName(a.getSubmitUserName())
                    .createdAt(a.getCreatedAt())
                    .updatedAt(a.getUpdatedAt())
                    .build();

            result.add(vo);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewedApplyItemVO> myReviewed() {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));
        if (!uc.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        // 1) 拉取“我审核过”的申请（APPROVED/REJECTED）
        List<SiteApplyDO> rows = siteApplyMapper.selectReviewedByReviewerDO(uc.getUserId());
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 先装配申请侧 VO（补主题名/国家名等），复用现有的方法
        List<SiteApplyVO> applies = assembleApplyVOs(rows);

        // 3) 批量取当前站点
        Set<String> ids = applies.stream()
                .map(SiteApplyVO::getTargetSiteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, OsSiteDO> id2site = ids.isEmpty()
                ? Collections.emptyMap()
                : osSiteMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(OsSiteDO::getId, x -> x, (a, b) -> a));

        // 4) 组装带有“存在性枚举”的新VO
        List<ReviewedApplyItemVO> result = new ArrayList<>(applies.size());
        for (SiteApplyVO a : applies) {
            OsSiteDO cur = (a.getTargetSiteId() == null) ? null : id2site.get(a.getTargetSiteId());
            boolean existsNow = cur != null && (cur.getIsDelete() == null || cur.getIsDelete() == 0);
            String snapshotQuality = DataQualityUtils.normalizeOrDefault(a.getDataQuality());
            String currentQuality = existsNow ? DataQualityUtils.normalizeOrDefault(cur.getDataQuality()) : null;

            ReviewedExistenceStatus label;

            // 用 ApplyStatus 枚举，避免硬编码
            if (!ApplyStatus.APPROVED.name().equals(a.getStatus())) {
                // 被驳回：视为从未落地（或不要求落地），标为 MISSING
                label = ReviewedExistenceStatus.MISSING;
            } else if (!existsNow) {
                // 审批通过但当前不存在
                label = (cur == null) ? ReviewedExistenceStatus.MISSING : ReviewedExistenceStatus.DELETED;
            } else {
                // 存在：判断是否与申请时一致（按需对比字段）
                boolean sameName = Objects.equals(s(a.getSiteName()), s(cur.getSiteName()));
                boolean sameUrl = Objects.equals(s(a.getUrl()), s(cur.getUrl()));
                label = (sameName && sameUrl)
                        ? ReviewedExistenceStatus.NORMAL
                        : ReviewedExistenceStatus.CHANGED;
            }

            result.add(ReviewedApplyItemVO.builder()
                    // 申请侧
                    .id(a.getId())
                    .targetSiteId(a.getTargetSiteId())
                    .actionType(a.getActionType())
                    .status(a.getStatus())
                    .siteName(a.getSiteName())
                    .url(a.getUrl())
                    .mainCountryCode(a.getMainCountryCode())
                    .themeIds(a.getThemeIds())
                    .themeNames(a.getThemeNames())
                    .scopes(a.getScopes())
                    .channel(a.getChannel())
                    .provider(a.getProvider())
                    .keywordsText(a.getKeywordsText())
                    .dataQuality(snapshotQuality == null ? DataQualityUtils.QUALITY_NORMAL : snapshotQuality)
                    .submitUserId(a.getSubmitUserId())
                    .submitUserName(a.getSubmitUserName())
                    .reviewUserId(a.getReviewUserId())
                    .reviewUserName(a.getReviewUserName())
                    .reviewReason(a.getReviewReason())
                    .createdAt(a.getCreatedAt())
                    .reviewedAt(a.getReviewedAt())
                    .updatedAt(a.getUpdatedAt())

                    // 当前站点
                    .existsNow(existsNow)
                    .currentSiteName(existsNow ? cur.getSiteName() : null)
                    .currentUrl(existsNow ? cur.getUrl() : null)
                    .currentDataQuality(!existsNow ? null : (currentQuality == null ? DataQualityUtils.QUALITY_NORMAL : currentQuality))
                    .currentMainCountryCode(existsNow ? cur.getMainCountryCode() : null)
                    .currentUpdatedAt(existsNow ? cur.getUpdatedAt() : null)

                    // 标签（枚举）
                    .existenceLabel(label)
                    .build());
        }
        return result;
    }

    // 字符串安全对比：null -> ""
    private static String s(String v) {
        return v == null ? "" : v;
    }


    @Override
    @Transactional(readOnly = true)
    public List<MyApprovedSiteItemVO> myApprovedSites() {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));

        // 1) 拉“我提交且已通过”的申请单（拿快照字段）——复用已有 mapper（避免再写 SQL）
        List<SiteApplyDO> applies = siteApplyMapper.selectBySubmitterAndStatus(
                uc.getUserId(), ApplyStatus.APPROVED.name());
        if (applies == null || applies.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 批量收集主题ID、国家码（申请快照）——与 assembleApplyVOs 同步
        Set<String> allThemeIds = new HashSet<>();
        Set<String> allCountryCodes = new HashSet<>();
        Set<String> mainCountryCodes = new HashSet<>();
        Set<String> targetIds = new HashSet<>();
        for (SiteApplyDO a : applies) {
            targetIds.add(a.getTargetSiteId());
            if (StringUtils.hasText(a.getThemeIdsText())) {
                allThemeIds.addAll(parseCsv(a.getThemeIdsText()));
            }
            if (StringUtils.hasText(a.getScopeCountryCodesText())) {
                allCountryCodes.addAll(parseCsv(a.getScopeCountryCodesText()));
            }
            if (StringUtils.hasText(a.getMainCountryCode())) {
                mainCountryCodes.add(a.getMainCountryCode().trim().toUpperCase());
            }
        }

        Map<String, String> tid2name = allThemeIds.isEmpty()
                ? Collections.emptyMap()
                : dictThemeMapper.selectIdNameMapByIds(allThemeIds);

        Set<String> allCodesForName = new HashSet<>();
        allCodesForName.addAll(allCountryCodes);
        allCodesForName.addAll(mainCountryCodes);
        Map<String, Map<String, String>> code2names = allCodesForName.isEmpty()
                ? Collections.emptyMap()
                : countryDictMapper.selectNameMapBatch(
                allCodesForName.stream().map(c -> c.trim().toUpperCase()).collect(Collectors.toSet())
        );

        // 3) 批量拉主表站点，避免 N+1
        Map<String, OsSiteDO> siteNowMap = Collections.emptyMap();
        if (!targetIds.isEmpty()) {

            List<OsSiteDO> siteNowList = siteService.listByIds(new ArrayList<>(targetIds));
            siteNowMap = siteNowList.stream().collect(Collectors.toMap(OsSiteDO::getId, x -> x, (a,b)->a));
        }

        // 4) 逐条装配 VO + 计算存在性标签
        List<MyApprovedSiteItemVO> result = new ArrayList<>(applies.size());
        for (SiteApplyDO a : applies) {
            // 申请侧：主题与范围
            List<String> themeIds = parseCsv(a.getThemeIdsText());
            List<String> themeNames = themeIds.stream()
                    .map(tid2name::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            String mainZh = null, mainEn = null;
            if (StringUtils.hasText(a.getMainCountryCode())) {
                Map<String, String> nm = code2names.get(a.getMainCountryCode().trim().toUpperCase());
                if (nm != null) {
                    mainZh = nm.get("ZH");
                    mainEn = nm.get("EN");
                }
            }

            List<String> scopeCodes = parseCsv(a.getScopeCountryCodesText());
            List<CountryScopeVO> scopes = new ArrayList<>(scopeCodes.size());
            for (String code : scopeCodes) {
                Map<String, String> nm = code2names.get(code.trim().toUpperCase());
                scopes.add(CountryScopeVO.builder()
                        .siteId(null)
                        .countryCode(code)
                        .countryNameZh(nm == null ? null : nm.get("ZH"))
                        .countryNameEn(nm == null ? null : nm.get("EN"))
                        .build());
            }

            // 当前主表对照
            OsSiteDO now = siteNowMap.get(a.getTargetSiteId());
            boolean existsNow = now != null && (now.getIsDelete() == null || now.getIsDelete() == 0);

            // 计算存在性标签：仅比较核心字段（名称、URL），可按需扩展
            ReviewedExistenceStatus label;
            if (now == null) {
                label = ReviewedExistenceStatus.MISSING;
            } else if (now.getIsDelete() != null && now.getIsDelete() == 1) {
                label = ReviewedExistenceStatus.DELETED;
            } else {
                boolean sameName = Objects.equals(
                        StringUtils.trimAllWhitespace(a.getSiteName()),
                        StringUtils.trimAllWhitespace(now.getSiteName()));
                boolean sameUrl = Objects.equals(
                        StringUtils.trimAllWhitespace(a.getUrl()),
                        StringUtils.trimAllWhitespace(now.getUrl()));
                label = (sameName && sameUrl) ? ReviewedExistenceStatus.NORMAL : ReviewedExistenceStatus.CHANGED;
            }

            String snapshotQuality = DataQualityUtils.normalizeOrDefault(a.getDataQuality());
            String currentQuality = existsNow ? DataQualityUtils.normalizeOrDefault(now.getDataQuality()) : null;

            MyApprovedSiteItemVO vo = MyApprovedSiteItemVO.builder()
                    // —— 申请快照（字段名与 SiteApplyDO 一致）——
                    .id(a.getId())
                    .targetSiteId(a.getTargetSiteId())
                    .actionType(a.getActionType())
                    .siteName(a.getSiteName())
                    .url(a.getUrl())
                    .provider(a.getProvider())
                    .channel(a.getChannel())
                    .summary(a.getSummary())
                    .keywordsText(a.getKeywordsText())
                    .remark(a.getRemark())
                    .dataQuality(snapshotQuality == null ? DataQualityUtils.QUALITY_NORMAL : snapshotQuality)
                    .mainCountryCode(a.getMainCountryCode())
                    .themeIds(themeIds)
                    .themeNames(themeNames)
                    .scopes(scopes)
                    .status(a.getStatus())
                    .submitUserId(a.getSubmitUserId())
                    .submitUserName(a.getSubmitUserName())
                    .reviewUserId(a.getReviewUserId())
                    .reviewUserName(a.getReviewUserName())
                    .reviewReason(a.getReviewReason())
                    .createdAt(a.getCreatedAt())
                    .updatedAt(a.getUpdatedAt())
                    .reviewedAt(a.getReviewedAt())
                    .mainCountryNameZh(mainZh)
                    .mainCountryNameEn(mainEn)

                    // —— 当前主表对照 ——
                    .existsNow(existsNow)
                    .currentSiteName(existsNow ? now.getSiteName() : null)
                    .currentUrl(existsNow ? now.getUrl() : null)
                    .currentDataQuality(!existsNow ? null : (currentQuality == null ? DataQualityUtils.QUALITY_NORMAL : currentQuality))
                    .currentMainCountryCode(existsNow ? now.getMainCountryCode() : null)
                    .currentUpdatedAt(existsNow ? now.getUpdatedAt() : null)

                    // —— 标签 ——
                    .existenceLabel(label)
                    .build();

            result.add(vo);
        }

        return result;
    }


    @Override
    @Transactional(readOnly = true)
    public List<SiteApplyVO> myApplies(String status) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));
        String norm = null;
        if (StringUtils.hasText(status)) {
            norm = status.trim().toUpperCase();
            if (!Arrays.asList(ApplyStatus.PENDING.name(),
                    ApplyStatus.APPROVED.name(),
                    ApplyStatus.REJECTED.name()).contains(norm)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法状态参数");
            }
        }
        List<SiteApplyDO> rows = siteApplyMapper.selectBySubmitterAndStatus(uc.getUserId(), norm);
        return assembleApplyVOs(rows);
    }

    /**
     * 用户撤销自己的待审核申请：PENDING -> DRAFT
     * 并发与越权防护：
     * 1) 使用 selectByIdForUpdate 行级锁，避免并发审批/撤销造成竞态
     * 2) 仅申请提交人可撤销
     * 3) 仅 PENDING 状态可撤销
     */
    @Override
    public void cancel(String applyId) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));
        SiteApplyDO row = siteApplyMapper.selectByIdForUpdate(applyId);
        if (row == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请不存在");
        }
        if (!Objects.equals(row.getSubmitUserId(), uc.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作他人的申请");
        }
        if (!ApplyStatus.PENDING.name().equals(row.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅待审核申请可撤销");
        }

        // 仅当当前状态仍是 PENDING 才能撤销为 DRAFT
        int n = siteApplyMapper.updateReviewIfStatus(
                applyId, ApplyStatus.PENDING.name(), ApplyStatus.DRAFT.name(),
                uc.getUserId(), uc.getUserName(), "用户撤销为草稿");
        if (n != 1) throw new BusinessException(ErrorCode.OPERATION_ERROR, "该申请已被他人处理，请刷新后重试");

        // 审批日志
        logApproval(applyId, ApplyAction.CANCEL.name(), "撤销申请，转为草稿");
    }

    /**
     * 用户保存数据源草稿（支持新建草稿或编辑已有草稿/被驳回申请的内容）
     * 约束：
     * 1) 新建草稿：生成新的申请ID（状态 DRAFT）
     * 2) 编辑草稿：仅 DRAFT / REJECTED 状态允许覆盖内容
     * 3) 不做严格业务校验（草稿阶段可不完整），仅做基本清洗
     */
    @Override
    public String saveDraft(SiteSaveDTO dto, String targetSiteId) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));

        final String siteName = StringUtils.hasText(dto.getSiteName()) ? dto.getSiteName().trim() : null;
        final String url = StringUtils.hasText(dto.getUrl()) ? dto.getUrl().trim() : null;
        final String mainCode = StringUtils.hasText(dto.getMainCountryCode()) ? dto.getMainCountryCode().trim().toUpperCase() : null;
        final String themeCsv = joinCsv(dto.getThemeIds());
        final String scopesCsv = joinCsvUpper(dto.getScopes());
        final String keywordsText = trimToNull(dto.getKeywordsText());

        // A) 编辑已有草稿 / 被驳回申请（dto.id = 申请单ID）
        if (StringUtils.hasText(dto.getId())) {
            // 编辑已有草稿/被驳回申请
            SiteApplyDO row = siteApplyMapper.selectByIdForUpdate(dto.getId());
            if (row == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "草稿不存在");
            }
            if (!Objects.equals(row.getSubmitUserId(), uc.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权编辑他人的草稿");
            }
            if (!(ApplyStatus.DRAFT.name().equals(row.getStatus()) || ApplyStatus.REJECTED.name().equals(row.getStatus()))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅草稿/被驳回申请可编辑");
            }

            String dataQuality = normalizeDataQualityOrThrow(dto.getDataQuality(), row.getDataQuality());
            int n = siteApplyMapper.updateDraftContent(
                    dto.getId(),
                    siteName, url,
                    safe(dto.getProvider()), safe(dto.getChannel()),
                    safe(dto.getSummary()), keywordsText, safe(dto.getRemark()),
                    dataQuality, mainCode, themeCsv, scopesCsv
            );
            if (n != 1) throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存草稿失败，请重试");

            logApproval(dto.getId(), ApplyAction.DRAFT_SAVE.name(), "保存草稿");
            return dto.getId();
        }

        // B) 新建草稿：根据 targetSiteId 判断 CREATE / UPDATE 意图
        final String applyId = genId();
        final String actionType;
        final String finalTargetSiteId;
        final String dataQuality = normalizeDataQualityOrThrow(dto.getDataQuality(), null);

        if (StringUtils.hasText(targetSiteId)) {
            // —— 这是“编辑某个已有站点”的草稿（UPDATE）
            OsSiteDO target = siteService.getById(targetSiteId);
            if (target == null || (target.getIsDelete() != null && target.getIsDelete() == 1)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "要编辑的目标站点不存在或已删除");
            }
            actionType = ApplyAction.UPDATE.name();
            finalTargetSiteId = targetSiteId;
        } else {
            // —— 这是“新建站点”的草稿（CREATE）
            actionType = ApplyAction.CREATE.name();
            finalTargetSiteId = genId(); // 给未来新站点预分配一个 ID
        }

        SiteApplyDO row = SiteApplyDO.builder()
                .id(applyId)
                .targetSiteId(finalTargetSiteId)
                .actionType(actionType)
                .siteName(siteName)
                .url(url)
                .provider(safe(dto.getProvider()))
                .channel(safe(dto.getChannel()))
                .summary(safe(dto.getSummary()))
                .keywordsText(keywordsText)
                .remark(safe(dto.getRemark()))
                .dataQuality(dataQuality)
                .mainCountryCode(mainCode)
                .themeIdsText(themeCsv)
                .scopeCountryCodesText(scopesCsv)
                .status(ApplyStatus.DRAFT.name())
                .submitUserId(uc.getUserId())
                .submitUserName(uc.getUserName())
                .build();
        siteApplyMapper.insert(row);

        logApproval(applyId, ApplyAction.DRAFT_CREATE.name(),
                "新建草稿（" + actionType + "，targetSiteId=" + finalTargetSiteId + "）");
        return applyId;

    }


    /**
     * 用户重新提交：DRAFT / REJECTED -> PENDING
     * 注意：
     * 1) 重新提交前允许覆盖内容
     * 2) 只允许申请提交人操作
     */
    @Override
    public String resubmit(String applyId, SiteSaveDTO dto) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));

        SiteApplyDO row = siteApplyMapper.selectByIdForUpdate(applyId);
        if (row == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请不存在");
        if (!Objects.equals(row.getSubmitUserId(), uc.getUserId()))
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作他人的申请");
        if (!(ApplyStatus.DRAFT.name().equals(row.getStatus()) || ApplyStatus.REJECTED.name().equals(row.getStatus())))
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅草稿/被驳回申请可重新提交");

        // 覆盖最新内容（允许不完整，仅做清洗）
        String dataQuality = normalizeDataQualityOrThrow(dto.getDataQuality(), row.getDataQuality());
        int n1 = siteApplyMapper.updateDraftContent(
                applyId,
                safe(dto.getSiteName()), safe(dto.getUrl()),
                safe(dto.getProvider()), safe(dto.getChannel()),
                safe(dto.getSummary()), trimToNull(dto.getKeywordsText()), safe(dto.getRemark()),
                dataQuality,
                upper2(dto.getMainCountryCode()),
                joinCsv(dto.getThemeIds()),
                joinCsvUpper(dto.getScopes())
        );
        if (n1 != 1) throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新内容失败，请重试");

        // 仅当当前状态仍在 DRAFT/REJECTED 中，才置为 PENDING
        int n2 = siteApplyMapper.resubmit(
                applyId, ApplyStatus.PENDING.name(),
                uc.getUserId(), uc.getUserName(), "用户重新提交");
        if (n2 != 1) throw new BusinessException(ErrorCode.OPERATION_ERROR, "该申请已被他人处理，请刷新后重试");

        logApproval(applyId, ApplyAction.RESUBMIT.name(), "草稿/被驳回重新提交审核");

        return applyId;
    }

    /**
     * 用户删除自己的草稿 或 被驳回的申请（物理删除）
     * 安全与并发：
     * 1) 行级锁 + 身份校验
     * 2) 仅 DRAFT/REJECTED 可删除，避免误删在审或已归档记录
     */
    @Override
    public void deleteMyApply(String applyId) {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));

        SiteApplyDO row = siteApplyMapper.selectByIdForUpdate(applyId);
        if (row == null) return; // 幂等：视为删除成功
        if (!Objects.equals(row.getSubmitUserId(), uc.getUserId()))
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除他人的申请");
        if (!(ApplyStatus.DRAFT.name().equals(row.getStatus()) || ApplyStatus.REJECTED.name().equals(row.getStatus())))
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅草稿/被驳回申请可删除");

        int n = siteApplyMapper.deleteByIdStrict(applyId);
        if (n != 1) throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败，请重试");

        logApproval(applyId, ApplyAction.DELETE.name(), "物理删除草稿/被驳回申请");
    }

    /**
     * 用户查看自己的草稿列表（状态=DRAFT）
     * 说明：
     * 1) 仅返回自己的草稿
     * 2) 列表按创建时间倒序
     */
    @Override
    @Transactional(readOnly = true)
    public List<SiteApplyVO> myDrafts() {
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));

        // ★ 直接复用“按状态查询”的 SQL，状态固定为 DRAFT
        List<SiteApplyDO> rows = siteApplyMapper.selectBySubmitterAndStatus(
                uc.getUserId(), ApplyStatus.DRAFT.name());

        return assembleApplyVOs(rows); // ★ 统一装配
    }


    @Override
    public String publish(String applyId, SiteSaveDTO dto) {
        // 0) 管理员校验
        UserContext uc = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录"));
        if (!uc.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }
        if (!StringUtils.hasText(applyId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "applyId 不能为空");
        }

        // 1) 加行级锁读取草稿/被驳回单
        SiteApplyDO row = siteApplyMapper.selectByIdForUpdate(applyId);
        if (row == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请不存在");
        }
        if (!(ApplyStatus.DRAFT.name().equals(row.getStatus()) ||
                ApplyStatus.REJECTED.name().equals(row.getStatus()))) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅草稿/被驳回申请可直接发布");
        }

        // 2) （可选策略）是否允许管理员代发他人草稿
        if (!Objects.equals(row.getSubmitUserId(), uc.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权发布他人的草稿");
        }

        // 3) 若传入 dto，用其覆盖草稿内容（null/空视为不覆盖）
        if (dto != null) {
            String dataQuality = normalizeDataQualityOrThrow(dto.getDataQuality(), row.getDataQuality());
            siteApplyMapper.updateDraftContent(
                    applyId,
                    safe(dto.getSiteName()), safe(dto.getUrl()),
                    safe(dto.getProvider()), safe(dto.getChannel()),
                    safe(dto.getSummary()), trimToNull(dto.getKeywordsText()), safe(dto.getRemark()),
                    dataQuality, upper2(dto.getMainCountryCode()),
                    // 主题/范围入库同样走去重与大写规范
                    joinCsv(Optional.ofNullable(dto.getThemeIds()).orElse(Collections.emptyList())),
                    joinCsvUpper(Optional.ofNullable(dto.getScopes()).orElse(Collections.emptyList()))
            );
            // 重新取一遍最新草稿，确保后续合并数据一致
            row = siteApplyMapper.selectByIdForUpdate(applyId);
        }

        // 4) 组装“将要发布”的 DTO：以最新草稿为基准，若 dto 中对应字段非空则覆盖
        SiteSaveDTO toPublish = new SiteSaveDTO();
        // 关键：使用申请中预置的 targetSiteId 作为正式表主键（新建也保持与草稿一致）
        toPublish.setId(row.getTargetSiteId());

        toPublish.setSiteName(firstNonBlank(
                dto == null ? null : dto.getSiteName(), row.getSiteName()));
        toPublish.setUrl(firstNonBlank(
                dto == null ? null : dto.getUrl(), row.getUrl()));
        toPublish.setProvider(firstNonBlank(
                dto == null ? null : dto.getProvider(), row.getProvider()));
        toPublish.setChannel(firstNonBlank(
                dto == null ? null : dto.getChannel(), row.getChannel()));
        toPublish.setSummary(firstNonBlank(
                dto == null ? null : dto.getSummary(), row.getSummary()));
        toPublish.setKeywordsText(firstNonBlank(
                dto == null ? null : dto.getKeywordsText(), row.getKeywordsText()));
        toPublish.setRemark(firstNonBlank(
                dto == null ? null : dto.getRemark(), row.getRemark()));
        toPublish.setDataQuality(normalizeDataQualityOrThrow(
                dto == null ? null : dto.getDataQuality(),
                row.getDataQuality()));
        toPublish.setMainCountryCode(firstNonBlank(
                dto == null ? null : dto.getMainCountryCode(), row.getMainCountryCode()));

        // 主题/范围：优先使用 dto 非空集合；否则解析草稿 CSV；最后保证去重
        List<String> themeIds = (dto != null && !CollectionUtils.isEmpty(dto.getThemeIds()))
                ? dto.getThemeIds() : parseCsv(row.getThemeIdsText());
        if (themeIds != null) {
            themeIds = themeIds.stream().filter(StringUtils::hasText).distinct().collect(Collectors.toList());
        }
        toPublish.setThemeIds(themeIds);

        List<String> scopes = (dto != null && !CollectionUtils.isEmpty(dto.getScopes()))
                ? dto.getScopes() : parseCsv(row.getScopeCountryCodesText());
        if (scopes != null) {
            scopes = scopes.stream().filter(StringUtils::hasText).map(s -> s.trim().toUpperCase())
                    .distinct().collect(Collectors.toList());
        }
        toPublish.setScopes(scopes);

        OsSiteDO targetNow = siteService.getById(row.getTargetSiteId());
        final boolean isUpdate = (targetNow != null && (targetNow.getIsDelete() == null || targetNow.getIsDelete() == 0));

        // 5) 终局唯一性检查（与你 approve 分支保持一致）
        if (isUpdate) {
            OsSiteDO target = siteService.getById(row.getTargetSiteId());
            if (target == null || (target.getIsDelete() != null && target.getIsDelete() == 1)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "目标站点不存在或已删除");
            }
            LambdaQueryWrapper<OsSiteDO> uq = Wrappers.<OsSiteDO>lambdaQuery()
                    .eq(OsSiteDO::getIsDelete, 0)
                    .and(w -> w.eq(OsSiteDO::getUrl, toPublish.getUrl())
                            .or().eq(OsSiteDO::getSiteName, toPublish.getSiteName()))
                    .ne(OsSiteDO::getId, row.getTargetSiteId());
            if (siteService.count(uq) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改后的网址或名称与其他站点冲突");
            }
        } else {
            LambdaQueryWrapper<OsSiteDO> uq = Wrappers.<OsSiteDO>lambdaQuery()
                    .eq(OsSiteDO::getIsDelete, 0)
                    .and(w -> w.eq(OsSiteDO::getUrl, toPublish.getUrl())
                            .or().eq(OsSiteDO::getSiteName, toPublish.getSiteName()));
            if (siteService.count(uq) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "网站地址或名称已存在");
            }
        }

        // 6) 直接落正式表（审批流允许携带给定 ID）
        SiteVO vo = siteService.saveSiteFromApproval(toPublish);

        // 7) 标记该申请为 APPROVED，并记录“管理员直接发布”
        siteApplyMapper.updateReviewIfStatusIn(
                applyId, ApplyStatus.DRAFT.name(), ApplyStatus.REJECTED.name(), ApplyStatus.APPROVED.name(),
                uc.getUserId(), uc.getUserName(),
                "管理员直接发布"
        );
        logApproval(applyId, ApplyStatus.APPROVED.name(), "管理员直接发布（跳过审批）");

        // 8) 返回站点ID
        return vo.getId();
    }

    /**
     * 把 DO 列表一次性装配为 VO 列表
     */
    private List<SiteApplyVO> assembleApplyVOs(List<SiteApplyDO> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        // —— 以下与之前给你的 myApplies 装配逻辑相同，可直接拷贝 ——
        // 1) 批量收集主题ID/国家码
        Set<String> allThemeIds = new HashSet<>();
        Set<String> allCountryCodes = new HashSet<>();
        Set<String> mainCountryCodes = new HashSet<>();
        for (SiteApplyDO a : rows) {
            if (StringUtils.hasText(a.getThemeIdsText())) {
                allThemeIds.addAll(parseCsv(a.getThemeIdsText()));
            }
            if (StringUtils.hasText(a.getScopeCountryCodesText())) {
                allCountryCodes.addAll(parseCsv(a.getScopeCountryCodesText()));
            }
            if (StringUtils.hasText(a.getMainCountryCode())) {
                mainCountryCodes.add(a.getMainCountryCode().trim().toUpperCase());
            }
        }

        Map<String, String> tid2name = allThemeIds.isEmpty()
                ? Collections.emptyMap()
                : dictThemeMapper.selectIdNameMapByIds(allThemeIds);

        Map<String, Map<String, String>> code2names =
                (allCountryCodes.isEmpty() && mainCountryCodes.isEmpty())
                        ? Collections.emptyMap()
                        : countryDictMapper.selectNameMapBatch(
                        Stream.concat(allCountryCodes.stream(), mainCountryCodes.stream())
                                .map(s -> s.trim().toUpperCase()).collect(Collectors.toSet())
                );

        // 2) 逐条装配
        List<SiteApplyVO> result = new ArrayList<>(rows.size());
        for (SiteApplyDO a : rows) {
            List<String> themeIds = parseCsv(a.getThemeIdsText());
            List<String> themeNames = themeIds.stream()
                    .map(tid2name::get).filter(Objects::nonNull).collect(Collectors.toList());

            String mainZh = null, mainEn = null;
            if (StringUtils.hasText(a.getMainCountryCode())) {
                Map<String, String> nm = code2names.get(a.getMainCountryCode().trim().toUpperCase());
                if (nm != null) {
                    mainZh = nm.get("ZH");
                    mainEn = nm.get("EN");
                }
            }

            List<String> scopeCodes = parseCsv(a.getScopeCountryCodesText());
            List<CountryScopeVO> scopes = new ArrayList<>(scopeCodes.size());
            for (String code : scopeCodes) {
                Map<String, String> nm = code2names.get(code.trim().toUpperCase());
                scopes.add(CountryScopeVO.builder()
                        .siteId(null)
                        .countryCode(code)
                        .countryNameZh(nm == null ? null : nm.get("ZH"))
                        .countryNameEn(nm == null ? null : nm.get("EN"))
                        .build());
            }
            String dataQuality = DataQualityUtils.normalizeOrDefault(a.getDataQuality());

            result.add(SiteApplyVO.builder()
                    .id(a.getId())
                    .targetSiteId(a.getTargetSiteId())
                    .actionType(a.getActionType())
                    .siteName(a.getSiteName())
                    .url(a.getUrl())
                    .provider(a.getProvider())
                    .channel(a.getChannel())
                    .summary(a.getSummary())
                    .keywordsText(a.getKeywordsText())
                    .remark(a.getRemark())
                    .dataQuality(dataQuality == null ? DataQualityUtils.QUALITY_NORMAL : dataQuality)
                    .mainCountryCode(a.getMainCountryCode())
                    .mainCountryNameZh(mainZh)
                    .mainCountryNameEn(mainEn)
                    .themeIds(themeIds)
                    .themeNames(themeNames)
                    .scopes(scopes)
                    .status(a.getStatus())
                    .submitUserId(a.getSubmitUserId())
                    .submitUserName(a.getSubmitUserName())
                    .reviewUserId(a.getReviewUserId())
                    .reviewUserName(a.getReviewUserName())
                    .reviewReason(a.getReviewReason())
                    .createdAt(a.getCreatedAt())
                    .reviewedAt(a.getReviewedAt())
                    .updatedAt(a.getUpdatedAt())
                    .build());
        }
        return result;
    }

    /**
     * 小工具：返回第一个非空且非空白的字符串，否则取候选值
     */
    private static String firstNonBlank(String primary, String fallback) {
        if (StringUtils.hasText(primary)) return primary.trim();
        return StringUtils.hasText(fallback) ? fallback.trim() : null;
    }


    // ========== 本类可复用的小工具函数 ==========

    private static String safe(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static String trimToNull(String s) {
        String t = StringUtils.trimWhitespace(Objects.toString(s, ""));
        return t.isEmpty() ? null : t;
    }

    private static String upper2(String s) {
        return StringUtils.hasText(s) ? s.trim().toUpperCase() : null;
    }

    private static String genId() {
        UUID u = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
    }

    private static List<String> parseCsv(String s) {
        if (!StringUtils.hasText(s)) return Collections.emptyList();
        return Arrays.stream(s.split(",")).map(String::trim)
                .filter(t -> !t.isEmpty()).collect(Collectors.toList());
    }

    private static String joinCsv(List<String> l) {
        if (l == null || l.isEmpty()) return null;
        return l.stream().filter(StringUtils::hasText).map(String::trim).distinct()
                .collect(Collectors.joining(","));
    }

    private static String joinCsvUpper(List<String> l) {
        if (l == null || l.isEmpty()) return null;
        return l.stream().filter(StringUtils::hasText).map(x -> x.trim().toUpperCase()).distinct()
                .collect(Collectors.joining(","));
    }
    private String normalizeDataQualityOrThrow(String candidate, String fallback) {
        String source = StringUtils.hasText(candidate) ? candidate : fallback;
        String norm = DataQualityUtils.normalizeOrDefault(source);
        if (norm == null) {
            if (!StringUtils.hasText(candidate)) {
                return DataQualityUtils.QUALITY_NORMAL;
            }
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据质量仅支持：一般/重要/非常重要");
        }
        return norm;
    }

    private void logApproval(String applyId, String action, String detail) {
        UserContext uc = UserContextHolder.get(); // 允许未登录时为 null（极小概率）
        approvalLogMapper.insert(SiteApprovalLogDO.builder()
                .id(genId())
                .applyId(applyId)
                .action(action)
                .opUserId(uc == null ? null : uc.getUserId())
                .opUserName(uc == null ? null : uc.getUserName())
                .detail(detail)
                .build());
    }
}
