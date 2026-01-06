package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.ImportJobDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 导入任务 Mapper
 */
@Mapper
public interface ImportJobMapper extends BaseMapper<ImportJobDO> {
}
