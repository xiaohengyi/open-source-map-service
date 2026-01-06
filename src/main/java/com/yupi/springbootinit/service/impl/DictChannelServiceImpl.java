package com.yupi.springbootinit.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.mapper.DictChannelMapper;
import com.yupi.springbootinit.model.dto.DictChannelSaveDTO;
import com.yupi.springbootinit.model.entity.DictChannelDO;
import com.yupi.springbootinit.service.DictChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@DS("dm8")
public class DictChannelServiceImpl extends ServiceImpl<DictChannelMapper, DictChannelDO> implements DictChannelService {

    private final DictChannelMapper dictChannelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<String> listEnabledChannelNames() {
        LambdaQueryWrapper<DictChannelDO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DictChannelDO::getStatus,1);
        wrapper.orderByDesc(DictChannelDO::getStatus);
        wrapper.orderByAsc(DictChannelDO::getName);
        return this.list(wrapper).stream().map(DictChannelDO::getName).collect(Collectors.toList());
    }

    @Override
    public DictChannelDO saveChannel(DictChannelSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求体不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "渠道名称不能为空");
        }

        String id = StringUtils.trimWhitespace(dto.getId());
        String name = StringUtils.trimWhitespace(dto.getName());
        String code = StringUtils.hasText(dto.getCode()) ? StringUtils.trimWhitespace(dto.getCode()) : null;
        String desc = StringUtils.hasText(dto.getDescription()) ? StringUtils.trimWhitespace(dto.getDescription()) : null;
        Integer status = dto.getStatus() == null ? 1 : dto.getStatus();

        // 1) NAME 唯一校验（排除自身）
        long nameCnt = this.count(Wrappers.lambdaQuery(DictChannelDO.class)
                .eq(DictChannelDO::getName, name)
                .ne(StringUtils.hasText(id), DictChannelDO::getId, id));
        if (nameCnt > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "渠道名称已存在");
        }

        // 2) CODE 唯一校验（可选字段）
        if (StringUtils.hasText(code)) {
            long codeCnt = this.count(Wrappers.lambdaQuery(DictChannelDO.class)
                    .eq(DictChannelDO::getCode, code)
                    .ne(StringUtils.hasText(id), DictChannelDO::getId, id));
            if (codeCnt > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "渠道编码已存在");
            }
        }

        // 3) Upsert
        if (!StringUtils.hasText(id)) {
            DictChannelDO row = DictChannelDO.builder()
                    .id(genId())
                    .name(name)
                    .code(code)
                    .description(desc)
                    .status(status)
                    .build();
            dictChannelMapper.insert(row);
            return this.getById(row.getId());
        } else {
            DictChannelDO old = this.getById(id);
            if (old == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "渠道不存在");
            }
            DictChannelDO row = DictChannelDO.builder()
                    .id(id)
                    .name(name)
                    .code(code)
                    .description(desc)
                    .status(status)
                    .build();
            dictChannelMapper.updateById(row);
            return this.getById(id);
        }
    }

    @Override
    public void deleteChannel(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id 不能为空");
        }
        DictChannelDO old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "渠道不存在");
        }
        // 真正物理删除，而不是将 status=0
        dictChannelMapper.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DictChannelDO> listAll() {
        return this.list(Wrappers.lambdaQuery(DictChannelDO.class)
                .orderByDesc(DictChannelDO::getStatus)
                .orderByAsc(DictChannelDO::getName));
    }

    @Override
    public void updateStatus(String id, Integer status) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id 不能为空");
        }
        if (status == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "status 不能为空");
        }
        DictChannelDO old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "渠道不存在");
        }
        DictChannelDO row = DictChannelDO.builder()
                .id(id)
                .status(status)
                .build();
        dictChannelMapper.updateById(row);
    }

    private String genId() {
        // 若你项目已有统一 genId()，直接替换为你的即可
        return IdUtil.fastSimpleUUID();
    }
}
