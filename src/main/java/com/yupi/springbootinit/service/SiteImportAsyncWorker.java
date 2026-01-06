package com.yupi.springbootinit.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.mapper.ImportItemMapper;
import com.yupi.springbootinit.mapper.ImportJobMapper;
import com.yupi.springbootinit.model.entity.ImportItemDO;
import com.yupi.springbootinit.model.entity.ImportJobDO;
import com.yupi.springbootinit.utils.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.yupi.springbootinit.common.SiteExcelHeaders.*;
import static com.yupi.springbootinit.enums.ImportItemValidStatus.*;
import static com.yupi.springbootinit.enums.ImportJobStatus.*;

@Slf4j
@Component
@RequiredArgsConstructor
@DS("dm8")
public class SiteImportAsyncWorker {

    private final ImportJobMapper jobMapper;
    private final ImportItemMapper itemMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 后台解析 + 轻校验 + 入库
     * 注意：此方法在独立线程中执行，需自行维护事务边界
     */
    @Async("importExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void parseAndPersist(String jobId, byte[] fileBytes) {
        ImportJobDO job = jobMapper.selectById(jobId);
        if (job == null) return;
        log.info("[IMPORT] parseAndPersist START jobId={}, bytes.length={}", jobId, fileBytes.length);
        try {
            // 标记处理中
            job.setStatus(PROCESSING.name());
            jobMapper.updateById(job);

            // 解析
            try (InputStream is = new ByteArrayInputStream(fileBytes);
                 Workbook wb = WorkbookFactory.create(is)) {
                log.info("[IMPORT] workbook sheets={}, sheet0Name={}",
                        wb.getNumberOfSheets(),
                        wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0).getSheetName() : "NONE");

                Sheet sheet = wb.getSheetAt(0);
                log.info("[IMPORT] sheet0 lastRowNum={}, physicalRows={}",
                        sheet.getLastRowNum(),
                        sheet.getPhysicalNumberOfRows());

                List<ImportItemDO> items = parseExcelToItems(wb, jobId);
                log.info("[IMPORT] parseExcelToItems result size={}", items.size());

                // 轻校验 + URL 文件内去重
                int ready = 0, skipped = 0, dup = 0;
                Set<String> seenUrl = new HashSet<>();

                for (ImportItemDO it : items) {
                    Map<String, Object> row = readRowMap(it.getRawJson());
                    String url = trimToNull((String) row.get("url"));

                    List<String> errs = new ArrayList<>();
                    if (isBlank((String) row.get("siteName"))) errs.add("网站名称为空");
                    if (isBlank(url)) errs.add("网站地址为空");
                    if (isBlank((String) row.get("mainCountryCode"))) errs.add("主覆盖国家为空");

                    if (!errs.isEmpty()) {
                        it.setValidStatus(INVALID.name());
                        it.setValidMsg(String.join("; ", errs));
                        skipped++;
                    } else {
                        it.setValidStatus(VALID.name());
                        it.setValidMsg(null);
                        if (!seenUrl.add(url)) {
                            it.setDupFlag(1);
                            dup++;
                        } else {
                            it.setDupFlag(0);
                        }
                        ready++;
                    }
                    itemMapper.insert(it);
                }

                // 回填统计
                job.setRowsTotal(items.size());
                job.setRowsReady(ready);
                job.setRowsSkipped(skipped);
                job.setRowsDup(dup);
                job.setStatus(READY.name());
                jobMapper.updateById(job);
            }
        } catch (Exception e) {
            log.error("导入任务解析失败 jobId={}, err={}", jobId, e.getMessage(), e);
            job.setStatus(FAILED.name());
            jobMapper.updateById(job);
        }
    }

    /* ----------------- 以下为与 Service 同步版本一致的私有方法（拷贝/最小化修改） ----------------- */

    private List<ImportItemDO> parseExcelToItems(Workbook wb, String jobId) {
        Sheet sheet = wb.getSheetAt(0);
        log.info("[IMPORT] parseExcelToItems sheet lastRowNum={}, physicalRows={}",
                sheet.getLastRowNum(),
                sheet.getPhysicalNumberOfRows());
        Map<String, Integer> col = buildHeaderIndexMap(sheet.getRow(0));

        List<ImportItemDO> list = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null) continue;

            String siteName = getCellString(r, col.get(COL_SITE_NAME));
            String url = getCellString(r, col.get(COL_URL));
            String mainCountryCode = upperOrNull(getCellString(r, col.get(COL_MAIN_COUNTRY_CODE)));
            String themeNamesText = joinComma(splitAndTrim(getCellString(r, col.get(COL_THEME))));
            List<String> coverageCodes = splitAndUpper(getCellString(r, col.get(COL_COVERAGE_COUNTRIES)));
            String provider = getCellString(r, col.get(COL_PROVIDER));
            String channel = getCellString(r, col.get(COL_CHANNEL));
            String summary = getCellString(r, col.get(COL_SUMMARY));
            String keywordsText = getCellString(r, col.get(COL_KEYWORDS_TEXT));
            String remark = getCellString(r, col.get(COL_REMARK));
            String scopesText = joinComma(coverageCodes);

            Map<String, Object> rowMap = new LinkedHashMap<>();
            rowMap.put("siteName", siteName);
            rowMap.put("url", url);
            rowMap.put("mainCountryCode", mainCountryCode);
            rowMap.put("themeIdsText", null);
            rowMap.put("themeNamesText", themeNamesText);
            rowMap.put("provider", provider);
            rowMap.put("channel", channel);
            rowMap.put("summary", summary);
            rowMap.put("keywordsText", keywordsText);
            rowMap.put("remark", remark);
            rowMap.put("scopesText", scopesText);

            ImportItemDO it = new ImportItemDO();
            it.setId(IdUtil.urlSafeUuid());
            it.setJobId(jobId);
            it.setRowNo(i + 1);
            it.setRawJson(writeRowJson(rowMap));
            it.setValidStatus(PENDING.name());
            it.setValidMsg(null);
            it.setDupFlag(0);

            list.add(it);
        }
        return list;
    }

    private Map<String, Integer> buildHeaderIndexMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) {
            return map;
        }
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String raw = formatter.formatCellValue(cell);
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            // 去掉回车/换行，规避“自动换行”带来的多余字符
            String key = raw.replace("\r", "")
                    .replace("\n", "")
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!key.isEmpty()) {
                map.put(key, cell.getColumnIndex());
            }
        }
        return map;
    }


    private static String getCellString(Row r, Integer colIdx) {
        if (r == null || colIdx == null) return null;
        Cell c = r.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        c.setCellType(CellType.STRING);
        String s = c.getStringCellValue();
        return s != null ? s.trim() : null;
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

    private static String trimToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private static String joinComma(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }
    private static String upperOrNull(String s) {
        return isBlank(s) ? null : s.trim().toUpperCase();
    }
    private static List<String> splitAndTrim(String s) {
        if (isBlank(s)) return Collections.emptyList();
        s = s.replace('，', ',');
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
    }
    private static List<String> splitAndUpper(String s) {
        if (isBlank(s)) return Collections.emptyList();
        s = s.replace('，', ',');
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(String::toUpperCase).distinct().collect(Collectors.toList());
    }
}
