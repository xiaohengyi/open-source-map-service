package com.yupi.springbootinit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.annotation.RequireUser;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.model.dto.ImportCommitDTO;
import com.yupi.springbootinit.model.dto.ImportItemQueryDTO;
import com.yupi.springbootinit.model.dto.ImportItemUpdateDTO;
import com.yupi.springbootinit.model.dto.ImportJobQueryDTO;
import com.yupi.springbootinit.model.vo.ImportItemVO;
import com.yupi.springbootinit.model.vo.ImportJobVO;
import com.yupi.springbootinit.service.SiteImportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "站点批量导入")
@RestController
@RequiredArgsConstructor
@RequestMapping("/site/import")
public class SiteImportController {

    private final SiteImportService importService;

    // —— 新增：异步上传接口 —— //
    @ApiOperation("【异步】上传 Excel，后台解析；立即返回 jobId")
    @PostMapping("/uploadAsync")
    @RequireUser
    public BaseResponse<ImportJobVO> uploadAsync(@RequestParam("file") MultipartFile file) {
        return ResultUtils.success(importService.uploadAndParseAsync(file));
    }

    @ApiOperation("查询导入任务汇总信息")
    @GetMapping("/job")
    @RequireUser
    public BaseResponse<ImportJobVO> getJob(@RequestParam String jobId) {
        return ResultUtils.success(importService.getJob(jobId));
    }

    @ApiOperation("分页查看导入明细（支持 validStatus 与 approvalStatus=NONE/PENDING/APPROVED/REJECTED 过滤）")
    @PostMapping("/items/page")
    @RequireUser
    public BaseResponse<Page<ImportItemVO>> pageItems(@RequestBody ImportItemQueryDTO dto) {
        return ResultUtils.success(importService.pageItems(dto));
    }

    @ApiOperation("提交导入明细为“导入审批申请”（ SITE_IMPORT_APPLY，状态PENDING）")
    @PostMapping("/commit")
    @RequireUser
    public BaseResponse<ImportJobVO> commit(@RequestBody ImportCommitDTO dto) {
        return ResultUtils.success(importService.commitItems(dto));
    }

    @ApiOperation("作废导入任务（仅影响任务状态，不删除明细或申请）")
    @PostMapping("/cancel")
    @RequireUser
    public BaseResponse<String> cancel(@RequestParam String jobId) {
        importService.cancelJob(jobId);
        return ResultUtils.success();
    }

    @ApiOperation("分页查看我的导入任务")
    @PostMapping("/job/pageMy")
    @RequireUser
    public BaseResponse<Page<ImportJobVO>> pageMy(@RequestBody ImportJobQueryDTO dto) {
        return ResultUtils.success(importService.pageMyJobs(dto));
    }

    @ApiOperation("编辑导入明细并重新校验（仅任务拥有者可操作）")
    @PostMapping("/item/update")
    @RequireUser
    public BaseResponse<ImportItemVO> updateItem(@RequestBody ImportItemUpdateDTO dto) {
        return ResultUtils.success(importService.updateItem(dto));
    }

    @ApiOperation("删除导入明细（仅任务拥有者，非 SUBMITTED）")
    @PostMapping("/item/delete")
    @RequireUser
    public BaseResponse<String> deleteItem(@RequestParam String itemId) {
        importService.deleteItem(itemId);
        return ResultUtils.success();
    }


}
