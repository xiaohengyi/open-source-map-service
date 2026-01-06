package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.model.dto.ImportCommitDTO;
import com.yupi.springbootinit.model.dto.ImportItemQueryDTO;
import com.yupi.springbootinit.model.dto.ImportItemUpdateDTO;
import com.yupi.springbootinit.model.dto.ImportJobQueryDTO;
import com.yupi.springbootinit.model.vo.ImportItemVO;
import com.yupi.springbootinit.model.vo.ImportJobVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 站点批量导入 - 服务接口（ID 统一为 String-UUID）
 */
public interface SiteImportService {

    /**
     * 异步：上传→立即建任务→后台解析
     */
    ImportJobVO uploadAndParseAsync(MultipartFile file);

    /**
     * 查询导入任务
     */
    ImportJobVO getJob(String jobId);

    /**
     * 分页查询导入明细
     */
    Page<ImportItemVO> pageItems(ImportItemQueryDTO dto);

    /**
     * 将可提交明细批量生成 SiteApply（走既有审批流程）
     */
    ImportJobVO commitItems(ImportCommitDTO dto);

    /**
     * 作废导入任务（仅改变任务与未提交明细的状态）
     */
    void cancelJob(String jobId);

    /**
     * 分页查询“我的导入任务”（ownerUserId=当前用户）
     */
    Page<ImportJobVO> pageMyJobs(ImportJobQueryDTO dto);

    /**
     * 用户编辑导入明细（只允许任务拥有者），并重新轻校验
     */
    ImportItemVO updateItem(ImportItemUpdateDTO dto);

    /**
     * 用户删除任务中的item项
     */
    void deleteItem(String itemId);
}
