// 新增：ValidateUtils.java
package com.yupi.springbootinit.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.entity.OsSiteDO;
import org.springframework.lang.Nullable;

public final class ValidateUtils {
    private ValidateUtils() {}

    /**
     * URL / 名称唯一性断言（未删除内唯一）。传入 excludeId 则排除自身。
     * 统一使用规范化后的 URL 参与判断。
     */
    public static void assertUniqueUrlOrName(UniqueChecker checker,
                                             String normalizedUrl,
                                             String siteName,
                                             @Nullable String excludeId,
                                             String errMsg) {
        LambdaQueryWrapper<OsSiteDO> uq = Wrappers.<OsSiteDO>lambdaQuery()
                .eq(OsSiteDO::getIsDelete, 0)
                .and(w -> w.eq(OsSiteDO::getUrl, normalizedUrl)
                           .or().eq(OsSiteDO::getSiteName, siteName));
        if (excludeId != null) {
            uq.ne(OsSiteDO::getId, excludeId);
        }
        long cnt = checker.count(uq);
        if (cnt > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, errMsg);
        }
    }

    /** 适配器：避免在工具类里依赖具体 Service，实现方传入 lambda */
    @FunctionalInterface
    public interface UniqueChecker {
        long count(LambdaQueryWrapper<OsSiteDO> wrapper);
    }
}
