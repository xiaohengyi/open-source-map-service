package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.DictChannelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DictChannelMapper extends BaseMapper<DictChannelDO> {

    /**
     * 仅返回启用的渠道名称列表
     * 为了兼容原枚举顺序：INTERNET -> SELLING -> INTERNAL
     */
    @Select({
            "SELECT NAME",
            "FROM DICT_CHANNEL",
            "WHERE STATUS = 1",
            "ORDER BY CASE CODE",
            "  WHEN 'INTERNET' THEN 1",
            "  WHEN 'SELLING' THEN 2",
            "  WHEN 'INTERNAL' THEN 3",
            "  ELSE 99 END,",
            "NAME ASC"
    })
    List<String> listEnabledNames();
}
