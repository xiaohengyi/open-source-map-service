package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.UserQueryLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserQueryLogMapper extends BaseMapper<UserQueryLogDO> {
}
