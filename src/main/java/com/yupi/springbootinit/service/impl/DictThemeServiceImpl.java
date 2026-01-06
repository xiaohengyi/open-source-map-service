package com.yupi.springbootinit.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.mapper.DictThemeMapper;
import com.yupi.springbootinit.mapper.RelSiteThemeMapper;
import com.yupi.springbootinit.model.dto.DictThemeQueryDTO;
import com.yupi.springbootinit.model.dto.DictThemeSaveDTO;
import com.yupi.springbootinit.model.entity.DictThemeDO;
import com.yupi.springbootinit.service.DictThemeService;
import com.yupi.springbootinit.utils.SqlLikeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@DS("dm8")
public class DictThemeServiceImpl extends ServiceImpl<DictThemeMapper, DictThemeDO>
        implements DictThemeService {

    private final DictThemeMapper dictThemeMapper;
    private final RelSiteThemeMapper relSiteThemeMapper;

    @Override
    public String saveTheme(DictThemeSaveDTO dto) {
        // 唯一性校验（排除自身）
        if (StringUtils.hasText(dto.getName())) {
            int cnt = dictThemeMapper.countByName(dto.getName(), dto.getId());
            if (cnt > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "主题名称已存在");
            }
        }
        if (StringUtils.hasText(dto.getCode())) {
            int cnt = dictThemeMapper.countByCode(dto.getCode(), dto.getId());
            if (cnt > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "主题编码已存在");
            }
        }

        DictThemeDO po = DictThemeDO.builder()
                .id(StringUtils.hasText(dto.getId()) ? dto.getId() : genUrlSafeBase64Uuid())
                .name(dto.getName())
                .code(dto.getCode())
                .description(dto.getDescription())
                .status(dto.getStatus() == null ? 1 : dto.getStatus())
                .build();

        this.saveOrUpdate(po);
        return po.getId();
    }

    @Override
    public long getThemeCount() {
        return this.count();
    }

    @Override
    public void deleteTheme(String id, boolean force) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID 不能为空");
        }
        int used = relSiteThemeMapper.countByThemeId(id);
        if (used > 0 && !force) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该主题已被站点引用，请先解除引用或使用强制删除（同时删除该主题所有关联的数据源）");
        }
        if (force) {
            relSiteThemeMapper.deleteByThemeId(id); // 先删关联
        }
        this.removeById(id); // 再删主题
    }

    @Override
    public void updateStatus(String id, Integer status) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID 不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法状态，只能为 0 或 1");
        }
        DictThemeDO po = new DictThemeDO();
        po.setId(id);
        po.setStatus(status);
        this.updateById(po);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DictThemeDO> pageThemes(DictThemeQueryDTO dto) {
        LambdaQueryWrapper<DictThemeDO> qw = Wrappers.<DictThemeDO>lambdaQuery();
        if (StringUtils.hasText(dto.getKeyword())) {
            String p = SqlLikeUtils.likeContainsLiteral(dto.getKeyword());
            qw.and(w -> w
                    .apply("NAME LIKE {0} ESCAPE '" + SqlLikeUtils.ESC + "'", p)
                    .or()
                    .apply("CODE LIKE {0} ESCAPE '" + SqlLikeUtils.ESC + "'", p)
                    .or()
                    .apply("DESCRIPTION LIKE {0} ESCAPE '" + SqlLikeUtils.ESC + "'", p)
            );
        }
        if (dto.getStatus() != null) {
            qw.eq(DictThemeDO::getStatus, dto.getStatus());
        }

        long current = dto.getCurrent() == null ? 1L : dto.getCurrent();
        long size = dto.getSize() == null ? 20L : dto.getSize();

        Page<DictThemeDO> page = new Page<>(current, size);
        return this.page(page, qw.orderByAsc(DictThemeDO::getName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DictThemeDO> listEnabled() {
        return this.list(Wrappers.<DictThemeDO>lambdaQuery()
                .eq(DictThemeDO::getStatus, 1)
                .orderByAsc(DictThemeDO::getName));
    }

    /** 生成 URL-safe Base64 UUID（无=，/ -> _，+ -> -） */
    private static String genUrlSafeBase64Uuid() {
        UUID u = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        String b64 = Base64.getEncoder().encodeToString(bb.array());
        return b64.replace("=", "").replace('/', '_').replace('+', '-');
    }
}
