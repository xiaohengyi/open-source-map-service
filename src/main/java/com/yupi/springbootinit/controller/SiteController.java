package com.yupi.springbootinit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.annotation.RequireUser;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.common.SiteExcelHeaders;
import com.yupi.springbootinit.enums.Channel;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.dto.*;
import com.yupi.springbootinit.model.entity.CountryDictDO;
import com.yupi.springbootinit.model.entity.DictChannelDO;
import com.yupi.springbootinit.model.entity.OsSiteSampleDO;
import com.yupi.springbootinit.model.vo.*;
import com.yupi.springbootinit.service.DictChannelService;
import com.yupi.springbootinit.service.SiteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Api(tags = "处理开源地图资源相关内容")
@RestController
@RequiredArgsConstructor
@RequestMapping("/site")
public class SiteController {

    private final SiteService siteService;

    private final DictChannelService dictChannelService;

    @PostMapping("/sample/save")
    @RequireUser(admin = true)
    @ApiOperation("新增 / 编辑数据示例（粘贴 JSON，后端自动抽取冗余列字段）")
    public BaseResponse<OsSiteSampleDO> saveSample(@Valid @RequestBody SiteSampleSaveDTO dto) {
        return ResultUtils.success(siteService.saveSampleFromJson(dto));
    }

    @PostMapping("/sample/delete")
    @RequireUser(admin = true)
    @ApiOperation("删除数据示例（仅管理员）")
    public BaseResponse<Boolean> deleteSample(@Valid @RequestBody SiteSampleDeleteDTO dto) {
        siteService.deleteSample(dto.getId());
        return ResultUtils.success(true);
    }

    @GetMapping("/sample/listBySite")
    @RequireUser
    @ApiOperation("查看某数据源下的全部数据示例（实验期：返回全量字段，包含 sampleJson）")
    public BaseResponse<List<OsSiteSampleDO>> listSamplesBySite(@RequestParam String siteId) {
        return ResultUtils.success(siteService.listSamplesBySiteId(siteId));
    }

    @GetMapping("/sample/get")
    @RequireUser
    @ApiOperation("按数据示例ID查看详情")
    public BaseResponse<OsSiteSampleDO> getSample(@RequestParam String id) {
        return ResultUtils.success(siteService.getSampleDetail(id));
    }


    @PostMapping("/save")
    @RequireUser(admin = true)
    @ApiOperation("新增 / 编辑站点（保存即更新）")
    public BaseResponse<SiteVO> save(@Valid @RequestBody SiteSaveDTO dto) {
        return ResultUtils.success(siteService.saveSite(dto));
    }

    @PostMapping("/delete")
    @RequireUser(admin = true)
    @ApiOperation("删除站点")
    public BaseResponse<String> delete(@RequestParam String id) {
        siteService.deleteSite(id);
        return ResultUtils.success();
    }

    @GetMapping("/get")
    @RequireUser
    @ApiOperation("站点详情")
    public BaseResponse<SiteVO> get(@RequestParam String id) {
        return ResultUtils.success(siteService.getSiteDetail(id));
    }

    @PostMapping("/search")
    @RequireUser
    @ApiOperation("高级检索（名称模糊 / 主题 / 提供方 / 渠道 / 国家）")
    public BaseResponse<Page<SiteVO>> search(@Valid @RequestBody SiteQueryDTO dto) {
        return ResultUtils.success(siteService.searchSites(dto));
    }

    /**
     * 下钻：按覆盖国家查看数据源（国家永远在前，ALL 永远在后）
     */
    @PostMapping("/search/country")
    @RequireUser
    @ApiOperation("下钻：按覆盖国家查看数据源（国家在前，ALL 在后）")
    public BaseResponse<Page<SiteVO>> searchByCountry(@Valid @RequestBody SiteCountryQueryDTO dto) {
        return ResultUtils.success(siteService.searchSitesByCountry(dto));
    }

    @GetMapping("/stat/country")
    @RequireUser
    @ApiOperation("按国家分组统计站点数量（地图气泡）")
    public BaseResponse<List<CountryStatVO>> statByCountry() {
        return ResultUtils.success(siteService.statByCountry());
    }


    @GetMapping("/count")
    @RequireUser
    @ApiOperation("获取目前的站点数量")
    public BaseResponse<Long> siteCount() {
        return ResultUtils.success(siteService.getSiteCount());
    }


    @GetMapping("/country/count")
    @RequireUser
    @ApiOperation("获取目前系统中覆盖到的国家数量")
    public BaseResponse<Long> getCountryCount() {
        return ResultUtils.success(siteService.getCountryCount());
    }

    @GetMapping("/recommend")
    @RequireUser
    @ApiOperation("智能推荐")
    public BaseResponse<List<SiteVO>> recommend() {
        return ResultUtils.success(siteService.recommendForCurrentUser());
    }


    @GetMapping("/stat/theme")
    @RequireUser
    @ApiOperation("按主题领域分组统计站点数量（柱状图）")
    public BaseResponse<List<ThemeStatVO>> statByTheme() {
        return ResultUtils.success(siteService.statByTheme());
    }

    @GetMapping("/stat/timeline")
    @RequireUser
    @ApiOperation("接入情况时间轴（window: 7d / 6m / 12m 或自定义时间范围）")
    public BaseResponse<List<TimelinePointVO>> statTimeline(
            @RequestParam(required = false, defaultValue = "6m")
            String window,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate beginDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        // beginDate & endDate 同时存在时，视为自定义时间范围，忽略 window
        return ResultUtils.success(siteService.statTimeline(window, beginDate, endDate));
    }

    @GetMapping("/stat/wordcloud")
    @RequireUser
    @ApiOperation("关键词词云（按 KEYWORDS_TEXT 聚合，默认取 Top-100）")
    public BaseResponse<List<WordCloudVO>> keywordWordCloud(
            @RequestParam(value = "top", required = false) Integer top) {
        return ResultUtils.success(siteService.buildKeywordWordCloud(top));
    }

    @GetMapping("/stat/providerTop")
    @RequireUser
    @ApiOperation("按 Provider 聚合统计 Top-N（默认20, 上限100，可指定排序方向）")
    public BaseResponse<List<ProviderTopVO>> providerTop(
            @RequestParam(value = "top", required = false) Integer top,
            @RequestParam(value = "order", required = false) String order) {
        return ResultUtils.success(siteService.providerTop(top, order));
    }

    @GetMapping("/stat/channelTop")
    @RequireUser
    @ApiOperation("按 Channel 聚合统计 Top-N（默认20, 上限100，可指定排序方向）")
    public BaseResponse<List<ChannelTopVO>> channelTop(
            @RequestParam(value = "top", required = false) Integer top,
            @RequestParam(value = "order", required = false) String order) {
        return ResultUtils.success(siteService.channelTop(top, order));
    }

    @GetMapping("/country/dict")
    @RequireUser
    @ApiOperation("获取当前系统中存储的所有的国家代码")
    public BaseResponse<List<CountryDictDO>> countryDict() {
        return ResultUtils.success(siteService.getCountryDictList());
    }


    @GetMapping("/export")
    @RequireUser
    @ApiOperation("导出全部数据源为 Excel（列头与导入模板一致）")
    public void exportAllSites(HttpServletResponse response) {
        try {
            String fileName = URLEncoder.encode("数据源全量导出.xlsx", StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment;filename*=UTF-8''" + fileName);

            // 1）拉数据（保留原有逻辑）
            List<SiteExportRowVO> rows = siteService.buildSiteExportRows();

            // 2）用 POI 手工写表头 + 数据
            try (Workbook wb = new XSSFWorkbook();
                 ServletOutputStream out = response.getOutputStream()) {

                Sheet sheet = wb.createSheet("数据源");

                // 2.1 表头行：与导入模板保持完全一致的英文字段
                Row header = sheet.createRow(0);
                int colIdx = 0;
                for (String head : SiteExcelHeaders.EXPORT_HEADER_ORDER) {
                    Cell cell = header.createCell(colIdx++);
                    cell.setCellValue(head);
                }

                // 2.2 数据行
                int rowIdx = 1;
                for (SiteExportRowVO row : rows) {
                    Row r = sheet.createRow(rowIdx++);
                    int c = 0;

                    // 注意顺序要和 EXPORT_HEADER_ORDER 对应
                    r.createCell(c++).setCellValue(nvl(row.getSiteName()));           // SITE_NAME
                    r.createCell(c++).setCellValue(nvl(row.getTheme()));              // THEME（主题名称）
                    r.createCell(c++).setCellValue(nvl(row.getProvider()));           // PROVIDER
                    r.createCell(c++).setCellValue(nvl(row.getChannel()));            // CHANNEL
                    r.createCell(c++).setCellValue(nvl(row.getMainCountryCode()));    // MAIN_COUNTRY_CODE
                    r.createCell(c++).setCellValue(nvl(row.getCoverageCountries()));  // COVERAGE_COUNTRIES
                    r.createCell(c++).setCellValue(nvl(row.getUrl()));                // URL

                    // 下面这几个，如果有的话就填，没有就留空
                    r.createCell(c++).setCellValue("");                               // SUMMARY
                    r.createCell(c++).setCellValue("");                               // KEYWORDS_TEXT
                    r.createCell(c++).setCellValue("");                               // REMARK

                    // 保留你原来导出的三列（可选）
                    r.createCell(c++).setCellValue(row.getIsDelete() == null ? "" : String.valueOf(row.getIsDelete()));  // IS_DELETE
                    r.createCell(c++).setCellValue(nvl(row.getCreatedAt()));          // CREATED_AT（你在 VO 里本来就是字符串）
                    r.createCell(c++).setCellValue(nvl(row.getUpdatedAt()));          // UPDATED_AT
                }

                // 2.3 简单自适应列宽（可选）
                for (int i = 0; i < SiteExcelHeaders.EXPORT_HEADER_ORDER.size(); i++) {
                    sheet.autoSizeColumn(i);
                }

                wb.write(out);
                out.flush();
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出数据源失败：" + e.getMessage());
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }


}
