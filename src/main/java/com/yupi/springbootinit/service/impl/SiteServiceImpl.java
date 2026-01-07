package com.yupi.springbootinit.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.SampleParsedResult;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.mapper.*;
import com.yupi.springbootinit.model.dto.SiteCountryQueryDTO;
import com.yupi.springbootinit.model.dto.SiteQueryDTO;
import com.yupi.springbootinit.model.dto.SiteSampleSaveDTO;
import com.yupi.springbootinit.model.dto.SiteSaveDTO;
import com.yupi.springbootinit.model.entity.*;
import com.yupi.springbootinit.model.vo.*;
import com.yupi.springbootinit.security.UserContext;
import com.yupi.springbootinit.security.UserContextHolder;
import com.yupi.springbootinit.service.SiteService;
import com.yupi.springbootinit.utils.DataQualityUtils;
import com.yupi.springbootinit.utils.SampleJsonParser;
import com.yupi.springbootinit.utils.SqlLikeUtils;
import com.yupi.springbootinit.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@DS("dm8")
public class SiteServiceImpl extends ServiceImpl<OsSiteMapper, OsSiteDO> implements SiteService {

    private final OsSiteMapper osSiteMapper;
    private final DictThemeMapper dictThemeMapper;
    private final RelSiteThemeMapper relSiteThemeMapper;
    private final CountryDictMapper countryDictMapper;
    private final RelSiteScopeMapper relSiteScopeMapper;
    private final UserQueryLogMapper userQueryLogMapper;

    private final OsSiteSampleMapper osSiteSampleMapper;

    private final SampleJsonParser sampleJsonParser;

    private static final String KEY_SPLIT_REGEX = "[,，;；\\s]+"; // 英文/中文逗号、分号、任意空白

    @Override
    public SiteVO saveSite(SiteSaveDTO dto) {
        return saveSiteInternal(dto, false, null, null);
    }

    @Override
    public SiteVO saveSiteFromApproval(SiteSaveDTO dto) {
        // 审批通过：允许“携带 targetSiteId 新增”
        return saveSiteInternal(dto, true, null, null);
    }

    @Override
    public SiteVO saveSiteFromApproval(SiteSaveDTO dto, String createdByOverride, String updatedByOverride) {
        return saveSiteInternal(dto, true, createdByOverride, updatedByOverride);
    }


    private SiteVO saveSiteInternal(SiteSaveDTO dto, boolean allowCreateWithGivenId, String createdByOverride, String updatedByOverride) {
        UserContext uc = UserContextHolder.get();

        // 0) 基本参数校验（保持你的原逻辑不变）
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
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "主覆盖国家不能为空");
        }
        final String mainCode = dto.getMainCountryCode().trim().toUpperCase();
        if (!"ALL".equals(mainCode) && mainCode.length() != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "主覆盖国家代码必须是'ALL'或2位（ISO-3166-1 alpha-2）");
        }

        // ★ 编辑场景判断（根据 allowCreateWithGivenId 调整）
        final boolean isUpdate;
        OsSiteDO target = null;
        if (StringUtils.hasText(dto.getId())) {
            target = this.getById(dto.getId());
            if (target == null) {
                if (allowCreateWithGivenId) {
                    // 审批流允许：携带给定 ID 进行“受信新增”
                    isUpdate = false;
                } else {
                    // 普通入口禁止自定义ID新增
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止指定不存在的ID进行新增，请移除ID后再提交");
                }
            } else {
                if (target.getIsDelete() != null && target.getIsDelete() == 1) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "编辑目标站点不存在或已删除");
                }
                isUpdate = true;
            }
        } else {
            isUpdate = false;
        }

        // 1) URL/名称 唯一性（未删除内唯一；编辑排除自身）——保持你的原逻辑
        LambdaQueryWrapper<OsSiteDO> uq = Wrappers.<OsSiteDO>lambdaQuery()
                .eq(OsSiteDO::getIsDelete, 0)
                .and(w -> w.eq(OsSiteDO::getUrl, normUrl).or().eq(OsSiteDO::getSiteName, siteName));
        if (isUpdate) {
            uq.ne(OsSiteDO::getId, dto.getId());
        }
        if (this.count(uq) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "网站地址或网站名称已存在（未删除），请勿重复添加");
        }

        // 2) 主题合法性 —— 保持你的原逻辑
        if (!CollectionUtils.isEmpty(dto.getThemeIds())) {
            List<String> themeIds = dto.getThemeIds().stream()
                    .filter(StringUtils::hasText).distinct().collect(Collectors.toList());
            int cnt = dictThemeMapper.countByIds(themeIds);
            if (cnt != themeIds.size()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在非法主题ID");
            }
        }

        // 3) 国别校验与标准化
        List<String> scopes = Optional.ofNullable(dto.getScopes()).orElse(Collections.emptyList())
                .stream().filter(StringUtils::hasText)
                .map(s -> s.trim().toUpperCase()).distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        if (countryDictMapper.countByCodes(Collections.singletonList(mainCode)) == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "主覆盖国家代码非法");
        }
        if (!scopes.isEmpty()) {
            int c = countryDictMapper.countByCodes(scopes);
            if (c != scopes.size()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在非法覆盖国家代码");
            }
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
        // 4) 冗余主题文本 —— 保持你的原逻辑
        String themeText = "";
        if (!CollectionUtils.isEmpty(dto.getThemeIds())) {
            List<String> normThemeIds = dto.getThemeIds().stream()
                    .filter(StringUtils::hasText).distinct().collect(Collectors.toList());
            List<String> names = dictThemeMapper.selectNamesByIds(normThemeIds);
            themeText = String.join(",", names);
        }
        String keywordsText = StringUtils.trimWhitespace(Objects.toString(dto.getKeywordsText(), ""));
        if (!StringUtils.hasText(keywordsText)) {
            keywordsText = null;
        }

        // 5) 生成/确认主键 —— 关键调整：允许“审批流”携带的不存在ID用于新增
        final String siteId = (isUpdate ? dto.getId() : (StringUtils.hasText(dto.getId()) ? dto.getId() : genId()));

        // 数据质量：为空取默认；非法报错；编辑未传则沿用历史值
        String dataQuality;
        if (StringUtils.hasText(dto.getDataQuality())) {
            dataQuality = DataQualityUtils.normalizeOrDefault(dto.getDataQuality());
            if (dataQuality == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据质量仅支持：一般/重要/非常重要");
            }
        } else {
            String fallback = (isUpdate && target != null) ? target.getDataQuality() : null;
            dataQuality = DataQualityUtils.normalizeOrDefault(fallback);
            if (dataQuality == null) {
                dataQuality = DataQualityUtils.QUALITY_NORMAL;
            }
        }

        // === 关键：createdBy / updatedBy 的归属 ===
        final String createdBy =
                !isUpdate
                        ? (StringUtils.hasText(createdByOverride) ? createdByOverride : (uc == null ? null : uc.getUserId()))
                        : null; // 新增才写 createdBy；编辑不覆盖 createdBy（保持原表值）

        final String updatedBy =
                isUpdate
                        ? (StringUtils.hasText(updatedByOverride) ? updatedByOverride : (uc == null ? null : uc.getUserId()))
                        : null; // 新增不写 updatedBy（首次创建没有“更新人”）

        // 6) Upsert 主表 —— 保持你的原逻辑（新增时写 createdBy；编辑不改 createdBy）
        OsSiteDO po = OsSiteDO.builder()
                .id(siteId)
                .siteName(siteName)
                .url(normUrl)
                .provider(StringUtils.hasText(dto.getProvider()) ? dto.getProvider().trim() : null)
                .channel(StringUtils.hasText(dto.getChannel()) ? dto.getChannel().trim() : null)
                .summary(StringUtils.hasText(dto.getSummary()) ? dto.getSummary().trim() : null)
                .keywordsText(keywordsText)
                .remark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null)
                .theme(themeText)
                .dataQuality(dataQuality)
                .mainCountryCode(mainCode)
                .createdBy(createdBy)
                .updatedBy(updatedBy)
                .isDelete(0)
                .build();
        try {
            this.saveOrUpdate(po);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "网站地址或网站名称已存在（并发校验）");
        }

        // 7) 维护主题关系：先删后插 —— 保持你的原逻辑
        relSiteThemeMapper.deleteBySiteId(siteId);
        if (!CollectionUtils.isEmpty(dto.getThemeIds())) {
            List<RelSiteThemeDO> themeRows = dto.getThemeIds().stream()
                    .filter(StringUtils::hasText).distinct()
                    .map(tid -> RelSiteThemeDO.builder().siteId(siteId).themeId(tid).build())
                    .collect(Collectors.toList());
            if (!themeRows.isEmpty()) {
                relSiteThemeMapper.batchInsert(themeRows);
            }
        }

        // 8) 维护覆盖范围：先删后插；
        relSiteScopeMapper.deleteBySiteId(siteId);
        List<RelSiteScopeDO> scopeRows = scopes.stream()
                .map(code -> RelSiteScopeDO.builder().siteId(siteId).countryCode(code).build())
                .collect(Collectors.toList());
        if (!scopeRows.isEmpty()) {
            relSiteScopeMapper.batchInsert(scopeRows);
        }

        // 9) 返回详情 —— 保持你的原逻辑
        return getSiteDetail(siteId);
    }


    @Override
    public void deleteSite(String siteId) {
        // 逻辑删除：仅改 OS_SITE.IS_DELETE=1；关系不动，查询时自然被主表过滤
        // 下面的方法等价于 UPDATE os_site SET is_delete = 1 WHERE id = 'siteId';
        this.removeById(siteId);
    }

    @Override
    public long getSiteCount() {
        return this.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThemeStatVO> statByTheme() {
        return osSiteMapper.statByTheme();
    }

    @Override
    @Transactional(readOnly = true)
    public SiteVO getSiteDetail(String siteId) {
        OsSiteDO po = this.getById(siteId);
        if (po == null || (po.getIsDelete() != null && po.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "站点不存在");
        }
        // 主题ID + 名称
        List<String> themeIds = relSiteThemeMapper.listThemeIds(siteId);
        List<String> themeNames = themeIds.isEmpty() ? Collections.emptyList()
                : dictThemeMapper.selectNamesByIds(themeIds);
        // 覆盖国家（含名称、是否主）
        List<CountryScopeVO> scopes = relSiteScopeMapper.listScopesWithNames(siteId);
        // 主覆盖名称
        String mainZh = null, mainEn = null;
        if (StringUtils.hasText(po.getMainCountryCode())) {
            Map<String, String> names = countryDictMapper.selectNameMap(po.getMainCountryCode());
            mainZh = names.get("ZH");
            mainEn = names.get("EN");
        }
        String dataQuality = DataQualityUtils.normalizeOrDefault(po.getDataQuality());
        if (dataQuality == null) {
            dataQuality = DataQualityUtils.QUALITY_NORMAL;
        }
        return SiteVO.builder()
                .id(po.getId()).siteName(po.getSiteName()).url(po.getUrl())
                .theme(po.getTheme()).provider(po.getProvider()).channel(po.getChannel())
                .summary(po.getSummary()).keywordsText(po.getKeywordsText()).remark(po.getRemark())
                .dataQuality(dataQuality)
                .mainCountryCode(po.getMainCountryCode()).mainCountryNameZh(mainZh).mainCountryNameEn(mainEn)
                .themeIds(themeIds).themeNames(themeNames).scopes(scopes)
                .createdAt(po.getCreatedAt()).updatedAt(po.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public Page<SiteVO> searchSites(SiteQueryDTO dto) {
        // —— 新增：记录检索日志（忽略未登录）
        tryLogQuery(dto);

        // 1) 关系筛选（主题=全部匹配；覆盖国家=全部匹配）
        Set<String> byThemeAll = null, byScopeAll = null;

        if (!CollectionUtils.isEmpty(dto.getThemeIds())) {
            List<String> themeIds = dto.getThemeIds().stream()
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());
            if (!themeIds.isEmpty()) {
                List<String> siteIds = relSiteThemeMapper.selectSiteIdsByThemeIdsAll(themeIds, themeIds.size());
                byThemeAll = new HashSet<>(siteIds);
                if (byThemeAll.isEmpty()) return emptyPage(dto);
            }
        }

        // 覆盖国家：全部匹配
        List<String> mustCodes = Optional.ofNullable(dto.getCountryCodes())
                .orElse(Collections.emptyList())
                .stream()
                .filter(StringUtils::hasText)
                .map(s -> s.trim().toUpperCase())
                .distinct()
                .collect(Collectors.toList());

        if (!mustCodes.isEmpty()) {
            byScopeAll = findSitesByCountriesAll(mustCodes);
            if (byScopeAll.isEmpty()) return emptyPage(dto);
        }

        // 交集（AND）：同时满足主题与覆盖条件
        Set<String> filterSet = null;
        if (byThemeAll != null && byScopeAll != null) {
            byThemeAll.retainAll(byScopeAll);
            filterSet = byThemeAll;
            if (filterSet.isEmpty()) return emptyPage(dto);
        } else if (byThemeAll != null) {
            filterSet = byThemeAll;
        } else if (byScopeAll != null) {
            filterSet = byScopeAll;
        }

        // 2) 主表过滤：名称 / 提供方 / 渠道 / 主覆盖国家（单选）
        LambdaQueryWrapper<OsSiteDO> qw = Wrappers.lambdaQuery();
        qw.eq(OsSiteDO::getIsDelete, 0);  // ★ 只查未删除
        if (!CollectionUtils.isEmpty(filterSet)) {
            qw.in(OsSiteDO::getId, filterSet);
        }
        if (StringUtils.hasText(dto.getNameLike())) {
            String p = SqlLikeUtils.likeContainsLiteral(dto.getNameLike());
            qw.apply("SITE_NAME LIKE {0} ESCAPE '" + SqlLikeUtils.ESC + "'", p);
        }
        if (StringUtils.hasText(dto.getProvider())) {
            String p = SqlLikeUtils.likeContainsLiteral(dto.getProvider());
            qw.apply("PROVIDER LIKE {0} ESCAPE '" + SqlLikeUtils.ESC + "'", p);
        }
        if (StringUtils.hasText(dto.getChannel())) {
            String p = SqlLikeUtils.likeContainsLiteral(dto.getChannel());
            qw.apply("CHANNEL LIKE {0} ESCAPE '" + SqlLikeUtils.ESC + "'", p);
        }
        if (StringUtils.hasText(dto.getDataQuality())) {
            String dataQuality = DataQualityUtils.requireValidOrDefault(dto.getDataQuality());
            qw.eq(OsSiteDO::getDataQuality, dataQuality);
        }
        if (StringUtils.hasText(dto.getMainCountryCode())) {
            qw.eq(OsSiteDO::getMainCountryCode, dto.getMainCountryCode().trim().toUpperCase());
        }
        if (dto.getBeginTime() != null) {
            qw.ge(OsSiteDO::getUpdatedAt, dto.getBeginTime());
        }
        if (dto.getEndTime() != null) {
            qw.le(OsSiteDO::getUpdatedAt, dto.getEndTime());
        }

        // 2.5) 排序逻辑（ 新增）
        // 默认：按 updatedAt desc 排序（保持原来的行为）
        String sortField = StringUtils.hasText(dto.getSortField())
                ? dto.getSortField().trim()
                : "updatedAt";
        String sortOrder = StringUtils.hasText(dto.getSortOrder())
                ? dto.getSortOrder().trim()
                : "desc";
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);

        switch (sortField) {
            case "createdAt":
                qw.orderBy(true, isAsc, OsSiteDO::getCreatedAt);
                break;
            case "siteName":
                qw.orderBy(true, isAsc, OsSiteDO::getSiteName);
                break;
            case "provider":
                qw.orderBy(true, isAsc, OsSiteDO::getProvider);
                break;
            case "mainCountryCode":
                qw.orderBy(true, isAsc, OsSiteDO::getMainCountryCode);
                break;
            case "updatedAt":
            default:
                // 默认还是用更新时间
                qw.orderBy(true, isAsc, OsSiteDO::getUpdatedAt);
                break;
        }

        // 3) 分页
        long current = Math.max(1, Optional.ofNullable(dto.getCurrent()).orElse(1L));
        long size = Math.max(1, Optional.ofNullable(dto.getSize()).orElse(20L));
        Page<OsSiteDO> page = new Page<>(current, size);
        Page<OsSiteDO> result = this.page(page, qw);

        // 使用共用的 VO 组装方法
        return buildSiteVoPage(result);
    }

    @Override
    @Transactional
    public Page<SiteVO> searchSitesByCountry(SiteCountryQueryDTO dto) {
        // 记录检索日志（如果你希望下钻也计入日志）
        tryLogQuery(dto);

        // 1) 处理国家 code
        String code = Optional.ofNullable(dto.getCountryCode())
                .map(String::trim)
                .map(String::toUpperCase)
                .orElse("");
        if (!StringUtils.hasText(code)) {
            // 复用原来的 emptyPage(dto)，因为 SiteCountryQueryDTO 继承了 SiteQueryDTO
            return emptyPage(dto);
        }

        // 2) 覆盖国家：用原来 searchSites 的关系逻辑，保证「属于该国家」的判定一致
        List<String> mustCodes = Collections.singletonList(code);
        Set<String> byScopeAll = findSitesByCountriesAll(mustCodes, false);
        if (CollectionUtils.isEmpty(byScopeAll)) {
            return emptyPage(dto);
        }

        // 3) 主表过滤：只看未删除 + 属于 byScopeAll 的站点
        LambdaQueryWrapper<OsSiteDO> qw = Wrappers.lambdaQuery();
        qw.eq(OsSiteDO::getIsDelete, 0)
                .in(OsSiteDO::getId, byScopeAll);

        // includeGlobal = false 时，排除 mainCountryCode = ALL
        if (Boolean.FALSE.equals(dto.getIncludeGlobal())) {
            qw.ne(OsSiteDO::getMainCountryCode, "ALL");
        }
        if (StringUtils.hasText(dto.getDataQuality())) {
            String dataQuality = DataQualityUtils.requireValidOrDefault(dto.getDataQuality());
            qw.eq(OsSiteDO::getDataQuality, dataQuality);
        }

        // 4) 分页参数
        long current = Math.max(1, Optional.ofNullable(dto.getCurrent()).orElse(1L));
        long size = Math.max(1, Optional.ofNullable(dto.getSize()).orElse(20L));
        Page<OsSiteDO> page = new Page<>(current, size);

        // 5) 排序：具体国家在前，ALL 在后，同组按更新时间倒序
        // 注意这里用 .last() 自定义 ORDER BY，避免再受字典序影响
        qw.last("ORDER BY CASE WHEN MAIN_COUNTRY_CODE = 'ALL' THEN 1 ELSE 0 END, UPDATED_AT DESC");

        Page<OsSiteDO> result = this.page(page, qw);

        // 6) 复用 VO 组装
        return buildSiteVoPage(result);
    }


    /**
     * 把 OsSiteDO 的分页结果转换成 SiteVO 分页结果
     */
    private Page<SiteVO> buildSiteVoPage(Page<OsSiteDO> result) {
        Page<SiteVO> voPage = new Page<>(result.getCurrent(), result.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setPages(result.getPages());

        List<OsSiteDO> rows = result.getRecords();
        if (rows.isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 4) 批量补充主题、范围
        List<String> ids = rows.stream().map(OsSiteDO::getId).collect(Collectors.toList());
        Map<String, List<String>> themeMap = relSiteThemeMapper.listThemeIdsBatch(ids);
        Map<String, List<CountryScopeVO>> scopeMap = relSiteScopeMapper.listScopesWithNamesBatch(ids);

        // 批量拉所有主题 ID → 名字
        Set<String> allTids = themeMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Map<String, String> tid2name = allTids.isEmpty()
                ? Collections.emptyMap()
                : dictThemeMapper.selectIdNameMapByIds(allTids);

        // 批量拉所有主覆盖国家名
        Set<String> allMainCodes = rows.stream()
                .map(OsSiteDO::getMainCountryCode)
                .filter(StringUtils::hasText)
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toSet());
        Map<String, Map<String, String>> code2names = allMainCodes.isEmpty()
                ? Collections.emptyMap()
                : countryDictMapper.selectNameMapBatch(allMainCodes);

        // 5) 组装 VO
        List<SiteVO> voList = rows.stream().map(po -> {
            List<String> tids = themeMap.getOrDefault(po.getId(), Collections.emptyList());
            List<String> tnames = tids.stream()
                    .map(tid2name::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            String mainZh = null, mainEn = null;
            if (StringUtils.hasText(po.getMainCountryCode())) {
                Map<String, String> nm = code2names.get(po.getMainCountryCode().trim().toUpperCase());
                if (nm != null) {
                    mainZh = nm.get("ZH");
                    mainEn = nm.get("EN");
                }
            }
            String dataQuality = DataQualityUtils.normalizeOrDefault(po.getDataQuality());
            if (dataQuality == null) {
                dataQuality = DataQualityUtils.QUALITY_NORMAL;
            }

            return SiteVO.builder()
                    .id(po.getId())
                    .siteName(po.getSiteName())
                    .url(po.getUrl())
                    .theme(po.getTheme())
                    .provider(po.getProvider())
                    .channel(po.getChannel())
                    .summary(po.getSummary())
                    .keywordsText(po.getKeywordsText())
                    .remark(po.getRemark())
                    .dataQuality(dataQuality)
                    .mainCountryCode(po.getMainCountryCode())
                    .mainCountryNameZh(mainZh)
                    .mainCountryNameEn(mainEn)
                    .themeIds(tids)
                    .themeNames(tnames)
                    .scopes(scopeMap.getOrDefault(po.getId(), Collections.emptyList()))
                    .createdAt(po.getCreatedAt())
                    .updatedAt(po.getUpdatedAt())
                    .build();
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }


    /**
     * 简单规则推荐：最近30天用户偏好 + 时间衰减
     */
    @Transactional(readOnly = true)
    public List<SiteVO> recommendForCurrentUser() {
        UserContext uc = UserContextHolder.get();
        if (uc == null) return Collections.emptyList();

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        // 1) 拉取用户近30天检索日志
        List<UserQueryLogDO> logs = userQueryLogMapper.selectList(
                Wrappers.<UserQueryLogDO>lambdaQuery()
                        .eq(UserQueryLogDO::getUserId, uc.getUserId())
                        .ge(UserQueryLogDO::getCreatedAt, since)
        );
        if (logs.isEmpty()) {
            Page<OsSiteDO> page = new Page<>(1, 20);
            this.page(page, Wrappers.<OsSiteDO>lambdaQuery()
                    .eq(OsSiteDO::getIsDelete, 0)
                    .orderByDesc(OsSiteDO::getUpdatedAt));
            return toVOs(page.getRecords());
        }

        // 2) 统计偏好
        Map<String, Integer> themeWeight = new HashMap<>();
        Map<String, Integer> countryWeight = new HashMap<>();
        for (UserQueryLogDO l : logs) {
            if (StringUtils.hasText(l.getThemeIdsText())) {
                for (String tid : l.getThemeIdsText().split(",")) {
                    if (!tid.isEmpty()) themeWeight.merge(tid, 1, Integer::sum);
                }
            }
            if (StringUtils.hasText(l.getCountryCodesText())) {
                for (String cc : l.getCountryCodesText().split(",")) {
                    if (!cc.isEmpty()) countryWeight.merge(cc, 1, Integer::sum);
                }
            }
            if (StringUtils.hasText(l.getMainCountryCode())) {
                countryWeight.merge(l.getMainCountryCode(), 2, Integer::sum); // 主覆盖权重稍高
            }
        }

        // 3) 拿一批候选：最近30天更新过的站点
        List<OsSiteDO> candidates = this.list(
                Wrappers.<OsSiteDO>lambdaQuery()
                        .eq(OsSiteDO::getIsDelete, 0) //  只取未删除
                        .ge(OsSiteDO::getUpdatedAt, since)
                        .orderByDesc(OsSiteDO::getUpdatedAt)
                        .last("LIMIT 50")
        );
        if (candidates.isEmpty()) return Collections.emptyList();

        // 4) 批量取候选的主题/范围
        List<String> ids = candidates.stream().map(OsSiteDO::getId).collect(Collectors.toList());
        Map<String, List<String>> themeMap = relSiteThemeMapper.listThemeIdsBatch(ids);
        Map<String, List<CountryScopeVO>> scopeMap = relSiteScopeMapper.listScopesWithNamesBatch(ids);

        // 5) 评分
        List<OsSiteDO> sorted = candidates.stream()
                .sorted((a, b) -> {
                    double sa = score(a, themeMap, scopeMap, themeWeight, countryWeight);
                    double sb = score(b, themeMap, scopeMap, themeWeight, countryWeight);
                    return Double.compare(sb, sa); // 降序
                })
                .limit(20)
                .collect(Collectors.toList());

        return toVOs(sorted);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CountryStatVO> statByCountry() {
        return osSiteMapper.statByCountry();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCountryCount() {
        return osSiteMapper.countDistinctMainCountry();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimelinePointVO> statTimeline(String window, LocalDate beginDate, LocalDate endDate) {

        // ==== 1）统一确定时间范围 & 维度 ====
        LocalDate today = LocalDate.now();

        // 区间起止（日期维度，用于补零）
        LocalDate rangeStartDate;
        LocalDate rangeEndDate;

        // 区间起止（时间维度，用于 SQL 查询）
        LocalDateTime begin;
        LocalDateTime endExclusive;

        // true = 按天；false = 按月
        boolean byDay;

        // 1.1 自定义时间范围：beginDate & endDate 同时存在时生效
        if (beginDate != null && endDate != null) {
            // 容错：自动纠正顺序
            if (endDate.isBefore(beginDate)) {
                LocalDate tmp = beginDate;
                beginDate = endDate;
                endDate = tmp;
            }

            rangeStartDate = beginDate;
            rangeEndDate = endDate;

            begin = rangeStartDate.atStartOfDay();
            endExclusive = rangeEndDate.plusDays(1).atStartOfDay();

            long days = ChronoUnit.DAYS.between(rangeStartDate, rangeEndDate) + 1;
            // 约定：区间 <= 31 天按“天”聚合，否则按“月”聚合
            byDay = days <= 31;

        } else {
            // 1.2 预设窗口逻辑（保持原有行为不变）
            String w = (window == null ? "6m" : window.trim().toLowerCase());

            switch (w) {
                case "7d": {
                    byDay = true;
                    rangeEndDate = today;
                    rangeStartDate = today.minusDays(6); // 含今天共 7 天
                    begin = rangeStartDate.atStartOfDay();
                    endExclusive = rangeEndDate.plusDays(1).atStartOfDay();
                    break;
                }
                case "12m": {
                    byDay = false;
                    YearMonth startYm = YearMonth.from(today).minusMonths(11);
                    rangeStartDate = startYm.atDay(1);
                    rangeEndDate = today; // 截止到今天
                    begin = rangeStartDate.atStartOfDay();
                    endExclusive = rangeEndDate.plusDays(1).atStartOfDay();
                    break;
                }
                case "6m":
                default: {
                    byDay = false;
                    YearMonth startYm = YearMonth.from(today).minusMonths(5);
                    rangeStartDate = startYm.atDay(1);
                    rangeEndDate = today;
                    begin = rangeStartDate.atStartOfDay();
                    endExclusive = rangeEndDate.plusDays(1).atStartOfDay();
                    break;
                }
            }
        }

        // ==== 2）拉原始 createdAt（未删除） ====
        List<LocalDateTime> list = osSiteMapper.selectCreatedAtBetween(begin, endExclusive);

        // ==== 3）分桶并补零 ====
        if (byDay) {
            // 按“天”聚合
            Map<LocalDate, Long> buckets = new LinkedHashMap<>();
            for (LocalDate d = rangeStartDate; !d.isAfter(rangeEndDate); d = d.plusDays(1)) {
                buckets.put(d, 0L);
            }
            for (LocalDateTime ts : list) {
                LocalDate d = ts.toLocalDate();
                buckets.computeIfPresent(d, (k, v) -> v + 1);
            }

            List<TimelinePointVO> res = new ArrayList<>(buckets.size());
            for (Map.Entry<LocalDate, Long> e : buckets.entrySet()) {
                res.add(TimelinePointVO.builder()
                        .date(e.getKey())
                        .count(e.getValue())
                        .build());
            }
            return res;
        } else {
            // 按“月”聚合
            YearMonth startYm = YearMonth.from(rangeStartDate);
            YearMonth endYm = YearMonth.from(rangeEndDate);

            Map<YearMonth, Long> buckets = new LinkedHashMap<>();
            for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
                buckets.put(ym, 0L);
            }
            for (LocalDateTime ts : list) {
                YearMonth ym = YearMonth.from(ts.toLocalDate());
                buckets.computeIfPresent(ym, (k, v) -> v + 1);
            }

            List<TimelinePointVO> res = new ArrayList<>(buckets.size());
            for (Map.Entry<YearMonth, Long> e : buckets.entrySet()) {
                res.add(TimelinePointVO.builder()
                        .month(e.getKey())
                        .count(e.getValue())
                        .build());
            }
            return res;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WordCloudVO> buildKeywordWordCloud(Integer topN) {
        int limit = (topN == null || topN <= 0) ? 100 : Math.min(topN, 1000);

        List<String> rows = osSiteMapper.listAllKeywordsText();
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        // 计数（大小写不敏感，但展示用原样规范化成去首尾空格）
        Map<String, Integer> counter = new HashMap<>(256);
        for (String line : rows) {
            if (!StringUtils.hasText(line)) continue;
            String[] parts = line.split(KEY_SPLIT_REGEX);
            for (String raw : parts) {
                if (!StringUtils.hasText(raw)) continue;
                String norm = raw.trim();
                if (norm.isEmpty()) continue;

                // 这里用“小写”作为计数 key，保证不区分大小写；展示 keyword 用去掉首尾空格的原词
                String key = norm.toLowerCase(Locale.ROOT);
                counter.merge(key, 1, Integer::sum);
            }
        }

        if (counter.isEmpty()) {
            return Collections.emptyList();
        }

        // 排序、截断、还原展示值（展示值首字母大小写保持：这里简单把 key 还原为计数时最后一次遇到的原样。
        // 若想更精致，可在上面同时维护一个 key->display 映射，这里为了简洁直接用 key 本身。）
        return counter.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> WordCloudVO.builder()
                        .keyword(e.getKey()) // 展示小写；若需要“首字母大写”或“原样”，可额外维护 displayMap
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderTopVO> providerTop(Integer top, String order) {
        int limit = (top == null || top <= 0) ? 20 : Math.min(top, 100);

        // 归一化排序方向：只允许 ASC / DESC，其他全部按 DESC 处理
        String sortOrder = (order == null ? "DESC" : order.trim().toUpperCase());
        if (!"ASC".equals(sortOrder)) {
            sortOrder = "DESC";
        }

        List<ProviderTopVO> rows = osSiteMapper.selectProviderTop(limit, sortOrder);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelTopVO> channelTop(Integer top, String order) {
        int limit = (top == null || top <= 0) ? 20 : Math.min(top, 100);

        String sortOrder = (order == null ? "DESC" : order.trim().toUpperCase());
        if (!"ASC".equals(sortOrder)) {
            sortOrder = "DESC";
        }

        List<ChannelTopVO> rows = osSiteMapper.selectChannelTop(limit, sortOrder);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows;
    }


    @Override
    @Transactional(readOnly = true)
    public List<SiteExportRowVO> buildSiteExportRows() {
        // 1) 拉全部未删除站点（按创建时间排序，方便人工比对）
        List<OsSiteDO> sites = this.list(
                Wrappers.<OsSiteDO>lambdaQuery()
                        .eq(OsSiteDO::getIsDelete, 0)
                        .orderByAsc(OsSiteDO::getCreatedAt)
        );
        if (sites == null || sites.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 批量拉覆盖国家（与你 listScopesWithNamesBatch 的逻辑保持一致）
        List<String> ids = sites.stream()
                .map(OsSiteDO::getId)
                .collect(Collectors.toList());
        Map<String, List<CountryScopeVO>> scopeMap = relSiteScopeMapper.listScopesWithNamesBatch(ids);

        // 3) 时间格式化器：与模板中的字符串格式一致
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 4) 组装导出行（列顺序和模板完全一致）
        List<SiteExportRowVO> rows = new ArrayList<>(sites.size());
        for (OsSiteDO po : sites) {
            List<CountryScopeVO> scopes = scopeMap.getOrDefault(po.getId(), Collections.emptyList());

            // 覆盖国家代码：取所有 code，去重 + 大写 + 排序，逗号拼接
            LinkedHashSet<String> codeSet = new LinkedHashSet<>();
            for (CountryScopeVO s : scopes) {
                if (s.getCountryCode() == null) continue;
                String c = s.getCountryCode().trim().toUpperCase();
                if (!c.isEmpty()) {
                    codeSet.add(c);
                }
            }
            String coverageCountries = codeSet.isEmpty()
                    ? null
                    : codeSet.stream().sorted().collect(Collectors.joining(","));

            // THEME：优先使用 OS_SITE.THEME 冗余文本（已经是“主题名逗号拼接”）
            // 如果某些历史数据没有填冗余文本，可以在这里按需补充从 DICT_THEME 反查的逻辑
            String themeText = null;
            if (StringUtils.hasText(po.getTheme())) {
                themeText = po.getTheme().trim();
            }
            String dataQuality = DataQualityUtils.normalizeOrDefault(po.getDataQuality());
            if (dataQuality == null) {
                dataQuality = DataQualityUtils.QUALITY_NORMAL;
            }
            String summary = StringUtils.hasText(po.getSummary()) ? po.getSummary().trim() : null;
            String keywords = StringUtils.hasText(po.getKeywordsText()) ? po.getKeywordsText().trim() : null;
            String remark = StringUtils.hasText(po.getRemark()) ? po.getRemark().trim() : null;

            // 时间字段转成字符串（导入模板中的 CREATED_AT / UPDATED_AT 也是字符串）
            String createdAtStr = (po.getCreatedAt() != null) ? po.getCreatedAt().format(dtf) : null;
            String updatedAtStr = (po.getUpdatedAt() != null) ? po.getUpdatedAt().format(dtf) : null;

            SiteExportRowVO row = SiteExportRowVO.builder()
                    // 1) SITE_NAME
                    .siteName(po.getSiteName())
                    // 2) THEME（主题名称，而不是ID）
                    .theme(themeText)
                    // 3) PROVIDER
                    .provider(po.getProvider())
                    // 4) CHANNEL
                    .channel(po.getChannel())
                    // 4.5) DATA_QUALITY
                    .dataQuality(dataQuality)
                    // 5) MAIN_COUNTRY_CODE
                    .mainCountryCode(po.getMainCountryCode())
                    // 6) COVERAGE_COUNTRIES
                    .coverageCountries(coverageCountries)
                    // 7) URL
                    .url(po.getUrl())
                    // 8) SUMMARY
                    .summary(summary)
                    // 9) KEYWORDS_TEXT
                    .keywordsText(keywords)
                    // 10) REMARK
                    .remark(remark)
                    // 8) IS_DELETE（当前只导出未删除，基本都是 0）
                    .isDelete(po.getIsDelete())
                    // 9) CREATED_AT
                    .createdAt(createdAtStr)
                    // 10) UPDATED_AT
                    .updatedAt(updatedAtStr)
                    .build();

            rows.add(row);
        }
        return rows;
    }


    @Override
    public List<CountryDictDO> getCountryDictList() {
        return countryDictMapper.selectList(Wrappers.lambdaQuery());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OsSiteSampleDO> listSamplesBySiteId(String siteId) {
        if (!StringUtils.hasText(siteId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "siteId 不能为空");
        }

        // 建议保留：站点存在性校验（避免误查/脏请求）
        OsSiteDO site = this.getById(siteId);
        if (site == null || (site.getIsDelete() != null && site.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "站点不存在");
        }

        return osSiteSampleMapper.selectList(
                Wrappers.<OsSiteSampleDO>lambdaQuery()
                        .eq(OsSiteSampleDO::getSiteId, siteId)
                        .eq(OsSiteSampleDO::getIsDelete, 0)
                        .orderByDesc(OsSiteSampleDO::getUpdatedAt)
                        .orderByDesc(OsSiteSampleDO::getCreatedAt)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OsSiteSampleDO getSampleDetail(String sampleId) {
        if (!StringUtils.hasText(sampleId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleId 不能为空");
        }
        OsSiteSampleDO po = osSiteSampleMapper.selectById(sampleId);
        if (po == null || (po.getIsDelete() != null && po.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "数据示例不存在");
        }
        return po;
    }

    @Override
    @Transactional
    public OsSiteSampleDO saveSampleFromJson(SiteSampleSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求体不能为空");
        }
        if (!StringUtils.hasText(dto.getSiteId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "siteId 不能为空");
        }
        if (!StringUtils.hasText(dto.getSampleJson())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleJson 不能为空");
        }

        // 1) 站点存在性校验（与你 listSamplesBySiteId 保持一致）
        OsSiteDO site = this.getById(dto.getSiteId());
        if (site == null || (site.getIsDelete() != null && site.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "站点不存在");
        }

        // 2) 解析 JSON + 抽取冗余字段
        SampleParsedResult parsed = sampleJsonParser.parse(dto.getSampleJson());

        // 2.1) 校验：sampleJson.siteUrl 必须与该站点 url 同域/同站点
        String siteUrlNorm = UrlUtils.normalizeUrl(site.getUrl());
        String jsonSiteUrlNorm = UrlUtils.normalizeUrl(parsed.getSiteUrl());

        if (!isSameHost(siteUrlNorm, jsonSiteUrlNorm)) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "sampleJson.siteUrl 与所选站点不匹配：json=" + parsed.getSiteUrl() + "，site=" + site.getUrl()
            );
        }

        // 3) 新增 or 编辑
        if (!StringUtils.hasText(dto.getId())) {
            // 新增：DB 主键自己生成（不和 JSON.id 强绑定）
            OsSiteSampleDO row = OsSiteSampleDO.builder()
                    .id(genId())
                    .siteId(dto.getSiteId())
                    .title(parsed.getTitle())
                    .sourceUrl(parsed.getSourceUrl())
                    .language(parsed.getLanguage())
                    .publishedAt(parsed.getPublishedAt())
                    .summary(parsed.getSummary())
                    .sampleJson(parsed.getNormalizedJson())
                    .isDelete(0)
                    .build();
            osSiteSampleMapper.insert(row);
            return getSampleDetail(row.getId());
        } else {
            // 编辑：只允许编辑未删除记录；且不允许跨站点
            OsSiteSampleDO old = osSiteSampleMapper.selectActiveById(dto.getId());
            if (old == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "数据示例不存在");
            }
            if (!dto.getSiteId().equals(old.getSiteId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不允许修改样例所属站点");
            }

            OsSiteSampleDO row = OsSiteSampleDO.builder()
                    .id(old.getId())
                    .siteId(old.getSiteId())
                    .title(parsed.getTitle())
                    .sourceUrl(parsed.getSourceUrl())
                    .language(parsed.getLanguage())
                    .publishedAt(parsed.getPublishedAt())
                    .summary(parsed.getSummary())
                    .sampleJson(parsed.getNormalizedJson())
                    .build();
            osSiteSampleMapper.updateById(row);
            return getSampleDetail(row.getId());
        }
    }

    @Override
    @Transactional
    public void deleteSample(String sampleId) {
        if (!StringUtils.hasText(sampleId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sampleId 不能为空");
        }

        OsSiteSampleDO old = osSiteSampleMapper.selectActiveById(sampleId);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "数据示例不存在");
        }

        // MP + @TableLogic：deleteById => IS_DELETE=1
        int rows = osSiteSampleMapper.deleteById(sampleId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        }
    }

    private static String genId() {
        UUID u = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
    }

    private List<SiteVO> toVOs(List<OsSiteDO> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();

        List<String> ids = list.stream().map(OsSiteDO::getId).collect(Collectors.toList());
        Map<String, List<String>> themeMap = relSiteThemeMapper.listThemeIdsBatch(ids);
        Map<String, List<CountryScopeVO>> scopeMap = relSiteScopeMapper.listScopesWithNamesBatch(ids);

        //  批量主题名
        Set<String> allTids = themeMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Map<String, String> tid2name = allTids.isEmpty()
                ? Collections.emptyMap()
                : dictThemeMapper.selectIdNameMapByIds(allTids);

        // 批量主覆盖国家名
        Set<String> allMainCodes = list.stream()
                .map(OsSiteDO::getMainCountryCode)
                .filter(StringUtils::hasText)
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toSet());
        Map<String, Map<String, String>> code2names = allMainCodes.isEmpty()
                ? Collections.emptyMap()
                : countryDictMapper.selectNameMapBatch(allMainCodes);

        List<SiteVO> res = new ArrayList<>(list.size());
        for (OsSiteDO po : list) {
            List<String> tids = themeMap.getOrDefault(po.getId(), Collections.emptyList());
            List<String> tnames = tids.stream()
                    .map(tid2name::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            String mainZh = null, mainEn = null;
            if (StringUtils.hasText(po.getMainCountryCode())) {
                Map<String, String> nm = code2names.get(po.getMainCountryCode().trim().toUpperCase());
                if (nm != null) {
                    mainZh = nm.get("ZH");
                    mainEn = nm.get("EN");
                }
            }
            String dataQuality = DataQualityUtils.normalizeOrDefault(po.getDataQuality());
            if (dataQuality == null) {
                dataQuality = DataQualityUtils.QUALITY_NORMAL;
            }

            SiteVO vo = SiteVO.builder()
                    .id(po.getId())
                    .siteName(po.getSiteName())
                    .url(po.getUrl())
                    .theme(po.getTheme())
                    .provider(po.getProvider())
                    .channel(po.getChannel())
                    .summary(po.getSummary())
                    .keywordsText(po.getKeywordsText())
                    .remark(po.getRemark())
                    .dataQuality(dataQuality)
                    .mainCountryCode(po.getMainCountryCode())
                    .mainCountryNameZh(mainZh)
                    .mainCountryNameEn(mainEn)
                    .themeIds(tids)
                    .themeNames(tnames)
                    .scopes(scopeMap.getOrDefault(po.getId(), Collections.emptyList()))
                    .createdAt(po.getCreatedAt())
                    .updatedAt(po.getUpdatedAt())
                    .build();
            res.add(vo);
        }
        return res;
    }

    private Page<SiteVO> emptyPage(SiteQueryDTO dto) {
        long current = Math.max(1, Optional.ofNullable(dto.getCurrent()).orElse(1L));
        long size = Math.max(1, Optional.ofNullable(dto.getSize()).orElse(20L));
        return new Page<>(current, size);
    }

    private void tryLogQuery(SiteQueryDTO dto) {
        try {
            UserContext uc = UserContextHolder.get();
            if (uc == null) return;
            UserQueryLogDO row = UserQueryLogDO.builder()
                    .id(genId())
                    .userId(uc.getUserId())
                    .keyword(dto.getNameLike())
                    .themeIdsText(joinCsvRaw(dto.getThemeIds())) //
                    .countryCodesText(joinCsvUpper(dto.getCountryCodes())) //
                    .mainCountryCode(StringUtils.hasText(dto.getMainCountryCode())
                            ? dto.getMainCountryCode().trim().toUpperCase() : null)
                    .createdAt(LocalDateTime.now())
                    .build();
            userQueryLogMapper.insert(row);
        } catch (Exception ignored) {
        }
    }

    // 兼容原来的调用：默认 ALL 作为通配
    private Set<String> findSitesByCountriesAll(List<String> mustCodes) {
        return findSitesByCountriesAll(mustCodes, true);
    }

    // 新增：覆盖国家“全部命中（AND）”的 Java 实现，自动兼容 ALL
    private Set<String> findSitesByCountriesAll(List<String> mustCodes, boolean wildcardAll) {
        if (mustCodes == null || mustCodes.isEmpty()) return Collections.emptySet();
        Set<String> ok = new HashSet<>();

        // 1) 是否把 scope=ALL 当作“天然命中”
        if (wildcardAll) {
            ok.addAll(relSiteScopeMapper.selectSiteIdsHavingAll());
        }


        // 2) 命中任一给定 code 的 (siteId, countryCode) 对
        List<CountryScopeVO> pairs = relSiteScopeMapper.listSiteCodesIn(mustCodes);

        // 3) 聚合成：siteId -> 命中的国家码集合
        Map<String, Set<String>> site2codes = new HashMap<>();
        for (CountryScopeVO p : pairs) {
            if (p.getSiteId() == null || p.getCountryCode() == null) continue;
            site2codes.computeIfAbsent(p.getSiteId(), k -> new HashSet<>()).add(p.getCountryCode());
        }

        // 4) 过滤出“包含全部 mustCodes 的站点” + “含 ALL 的站点”
        for (Map.Entry<String, Set<String>> e : site2codes.entrySet()) {
            if (e.getValue().containsAll(mustCodes)) {
                ok.add(e.getKey());
            }
        }
        return ok;
    }


    private static String joinCsvUpper(List<String> l) {
        if (l == null || l.isEmpty()) return null;
        return l.stream().filter(StringUtils::hasText).map(s -> s.trim().toUpperCase())
                .distinct().collect(Collectors.joining(","));
    }

    private static String joinCsvRaw(List<String> l) {
        if (l == null || l.isEmpty()) return null;
        return l.stream().filter(StringUtils::hasText).map(String::trim)
                .distinct().collect(Collectors.joining(","));
    }


    private double score(OsSiteDO s,
                         Map<String, List<String>> themeMap,
                         Map<String, List<CountryScopeVO>> scopeMap,
                         Map<String, Integer> themeW,
                         Map<String, Integer> countryW) {

        List<String> themeList = themeMap.get(s.getId());
        if (themeList == null) {
            themeList = Collections.emptyList();
        }
        int themeMatch = 0;
        for (String t : themeList) {
            Integer weight = themeW.get(t);
            if (weight != null) {
                themeMatch += weight;
            }
        }

        int mainMatch = 0;
        String mainCode = s.getMainCountryCode();
        if (StringUtils.hasText(mainCode) && !"ALL".equalsIgnoreCase(mainCode)) {
            Integer mainWeight = countryW.get(mainCode);
            mainMatch = (mainWeight != null ? mainWeight : 0);
        }


        List<CountryScopeVO> scopeList = scopeMap.get(s.getId());
        if (scopeList == null) {
            scopeList = Collections.emptyList();
        }
        int scopeMatch = 0;
        boolean hasAll = scopeList.stream().anyMatch(v -> "ALL".equalsIgnoreCase(v.getCountryCode()));
        if (hasAll) {
            scopeMatch += 1; // 温和基准分，避免“横扫”
        } else {
            for (CountryScopeVO v : scopeList) {
                Integer weight = countryW.get(v.getCountryCode());
                if (weight != null) {
                    scopeMatch += weight;
                }
            }
        }

        double raw = themeMatch * 2.0 + mainMatch * 2.0 + scopeMatch * 1.0;

        LocalDateTime refDate = s.getUpdatedAt();
        if (refDate == null) refDate = s.getCreatedAt();
        if (refDate == null) refDate = LocalDateTime.now(); // 兜底，避免 NPE
        long days = Math.max(0, java.time.Duration.between(refDate, LocalDateTime.now()).toDays());

        double decay = Math.exp(-days / 15.0); // 15天半衰
        return raw * decay;
    }

    private boolean isSameHost(String urlA, String urlB) {
        try {
            URI a = URI.create(urlA);
            URI b = URI.create(urlB);
            String ha = a.getHost();
            String hb = b.getHost();
            if (ha != null && hb != null) {
                ha = ha.toLowerCase();
                hb = hb.toLowerCase();
                if (ha.startsWith("www.")) ha = ha.substring(4);
                if (hb.startsWith("www.")) hb = hb.substring(4);
                return ha.equals(hb);
            }
        } catch (Exception ignored) {
        }
        // 兜底：去掉末尾斜杠直接比
        return trimSlash(urlA).equalsIgnoreCase(trimSlash(urlB));
    }

    private String trimSlash(String s) {
        if (s == null) return "";
        String t = s.trim();
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }


}
