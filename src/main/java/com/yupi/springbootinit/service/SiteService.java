package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.dto.SiteCountryQueryDTO;
import com.yupi.springbootinit.model.dto.SiteQueryDTO;
import com.yupi.springbootinit.model.dto.SiteSampleSaveDTO;
import com.yupi.springbootinit.model.dto.SiteSaveDTO;
import com.yupi.springbootinit.model.entity.CountryDictDO;
import com.yupi.springbootinit.model.entity.OsSiteDO;
import com.yupi.springbootinit.model.entity.OsSiteSampleDO;
import com.yupi.springbootinit.model.vo.*;

import java.time.LocalDate;
import java.util.List;

public interface SiteService extends IService<OsSiteDO> {

    SiteVO saveSite(SiteSaveDTO dto);

    void deleteSite(String siteId);

    SiteVO getSiteDetail(String siteId);

    Page<SiteVO> searchSites(SiteQueryDTO dto);

    Page<SiteVO> searchSitesByCountry(SiteCountryQueryDTO dto);

    List<CountryStatVO> statByCountry();

    Long getCountryCount();

    SiteVO saveSiteFromApproval(SiteSaveDTO dto);

    List<SiteVO> recommendForCurrentUser();

    long getSiteCount();

    List<ThemeStatVO> statByTheme();

    List<TimelinePointVO> statTimeline(String window, LocalDate beginDate, LocalDate endDate);

    List<WordCloudVO> buildKeywordWordCloud(Integer topN);

    /**
     * Provider 维度 Top / Bottom-N 统计
     * @param top   前 N 个（正序/倒序由 order 决定）
     * @param order ASC=倒数 N，DESC=前 N（默认）
     */
    List<ProviderTopVO> providerTop(Integer top, String order);

    /**
     * Channel 维度 Top / Bottom-N 统计
     */
    List<ChannelTopVO> channelTop(Integer top, String order);

    /**
     * 构建“导出全部数据源”的 Excel 行数据（列头顺序与导入模板保持一致）
     */
    List<SiteExportRowVO> buildSiteExportRows();

    List<CountryDictDO> getCountryDictList();

    List<OsSiteSampleDO> listSamplesBySiteId(String siteId);

    OsSiteSampleDO getSampleDetail(String sampleId);

    OsSiteSampleDO saveSampleFromJson(SiteSampleSaveDTO dto);

    void deleteSample(String sampleId);


    default SiteVO saveSiteFromApproval(SiteSaveDTO dto, String createdByOverride, String updatedByOverride) {
        // 由实现类去覆盖；也可在实现类上直接声明 public 方法
        throw new UnsupportedOperationException();
    }





}
