package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.annotation.RequireUser;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.model.dto.DictChannelSaveDTO;
import com.yupi.springbootinit.model.dto.DictChannelUpdateStatusDTO;
import com.yupi.springbootinit.model.entity.DictChannelDO;
import com.yupi.springbootinit.service.DictChannelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "渠道字典（获取手段）")
@RestController
@RequiredArgsConstructor
@RequestMapping("/dict/channel")
public class DictChannelController {

    private final DictChannelService dictChannelService;

    @GetMapping("/enabled")
    @RequireUser
    @ApiOperation("获取启用的渠道字典（含 id/name/code/description/status）")
    public BaseResponse<List<String>> listEnabled() {
        return ResultUtils.success(dictChannelService.listEnabledChannelNames());
    }

    @GetMapping("/list")
    @RequireUser(admin = true)
    @ApiOperation("管理端：获取全部渠道字典（含停用）")
    public BaseResponse<List<DictChannelDO>> listAll() {
        return ResultUtils.success(dictChannelService.listAll());
    }

    @PostMapping("/save")
    @RequireUser(admin = true)
    @ApiOperation("管理端：新增 / 编辑渠道字典")
    public BaseResponse<DictChannelDO> save(@Valid @RequestBody DictChannelSaveDTO dto) {
        return ResultUtils.success(dictChannelService.saveChannel(dto));
    }

    // 新增：只更新启用状态
    @PostMapping("/status")
    @RequireUser(admin = true)
    @ApiOperation("更新渠道启用状态（1=启用 0=停用）")
    public BaseResponse<String> updateStatus(@Valid @RequestBody DictChannelUpdateStatusDTO dto) {
        dictChannelService.updateStatus(dto.getId(), dto.getStatus());
        return ResultUtils.success();
    }

    // 删除：真正物理删除
    @PostMapping("/delete")
    @RequireUser(admin = true)
    @ApiOperation("删除渠道字典")
    public BaseResponse<String> delete(@RequestParam String id) {
        dictChannelService.deleteChannel(id);
        return ResultUtils.success();
    }
}
