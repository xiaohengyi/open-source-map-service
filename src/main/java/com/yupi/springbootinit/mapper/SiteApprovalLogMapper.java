package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.SiteApprovalLogDO;
import com.yupi.springbootinit.security.UserContext;
import com.yupi.springbootinit.security.UserContextHolder;
import com.yupi.springbootinit.utils.IdUtil;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SiteApprovalLogMapper extends BaseMapper<SiteApprovalLogDO> {

    /**
     * 统一的导入/审批日志入口：
     * - action 建议：IMPORT_SUBMIT / IMPORT_RESUBMIT / IMPORT_APPROVED / IMPORT_REJECTED / IMPORT_AUTO_REJECT 等
     * - applyId：导入申请或普通申请的主键ID
     */
    default void logImport(String applyId, String action, String detail) {
        UserContext uc = UserContextHolder.get(); // 允许为 null
        SiteApprovalLogDO row = SiteApprovalLogDO.builder()
                .id(IdUtil.urlSafeUuid())
                .applyId(applyId)
                .action(action)
                .opUserId(uc == null ? null : uc.getUserId())
                .opUserName(uc == null ? null : uc.getUserName())
                .detail(detail)
                .build();
        this.insert(row);
    }
}
