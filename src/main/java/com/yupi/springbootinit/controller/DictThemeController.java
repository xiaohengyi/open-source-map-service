package com.yupi.springbootinit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.annotation.RequireUser;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.model.dto.DictThemeQueryDTO;
import com.yupi.springbootinit.model.dto.DictThemeSaveDTO;
import com.yupi.springbootinit.model.dto.DictThemeUpdateStatusDTO;
import com.yupi.springbootinit.model.entity.DictThemeDO;
import com.yupi.springbootinit.service.DictThemeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Api(tags = "主题字典")
@RestController
@RequestMapping("/theme")
@RequiredArgsConstructor
public class DictThemeController {

    private final DictThemeService dictThemeService;

    @PostMapping("/save")
    @RequireUser(admin = true)
    @ApiOperation("新增/编辑主题")
    public BaseResponse<String> save(@Valid @RequestBody DictThemeSaveDTO dto) {
        String id = dictThemeService.saveTheme(dto);
        return ResultUtils.success(id);
    }

    @PostMapping("/delete")
    @RequireUser(admin = true)
    @ApiOperation("删除主题；force=true 时会先删除关联关系再删除主题")
    public BaseResponse<String> delete(@RequestParam @NotBlank String id,
                                       @RequestParam(defaultValue = "false") boolean force) {
        dictThemeService.deleteTheme(id, force);
        return ResultUtils.success();
    }

    @PostMapping("/status")
    @RequireUser(admin = true)
    @ApiOperation("更新主题启用状态（1=启用 0=停用）")
    public BaseResponse<String> updateStatus(@Valid @RequestBody DictThemeUpdateStatusDTO dto) {
        dictThemeService.updateStatus(dto.getId(), dto.getStatus());
        return ResultUtils.success();
    }

    @PostMapping("/page")
    @ApiOperation("分页查询主题（关键字/状态）")
    public BaseResponse<Page<DictThemeDO>> page(@Valid @RequestBody DictThemeQueryDTO dto) {
        return ResultUtils.success(dictThemeService.pageThemes(dto));
    }

    @GetMapping("/enabled")
    @ApiOperation("查询全部启用的主题（按名称升序）")
    public BaseResponse<List<DictThemeDO>> listEnabled() {
        return ResultUtils.success(dictThemeService.listEnabled());
    }

    @GetMapping("/count")
    @ApiOperation("查询当前系统中的主题数量")
    public BaseResponse<Long> themeCount() {
        return ResultUtils.success(dictThemeService.getThemeCount());
    }




}
