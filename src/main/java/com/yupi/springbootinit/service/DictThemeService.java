package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.dto.DictThemeQueryDTO;
import com.yupi.springbootinit.model.dto.DictThemeSaveDTO;
import com.yupi.springbootinit.model.entity.DictThemeDO;

import java.util.List;

public interface DictThemeService extends IService<DictThemeDO> {

    /**
     * 新增/编辑：返回主题ID
     */
    String saveTheme(DictThemeSaveDTO dto);

    /**
     * 删除：force=true 时删除引用关系后再删主题
     */
    void deleteTheme(String id, boolean force);

    /**
     * 更新启用状态：1=启用 0=停用
     */
    void updateStatus(String id, Integer status);

    /**
     * 分页查询
     */
    Page<DictThemeDO> pageThemes(DictThemeQueryDTO dto);

    /**
     * 获取当前系统中的领域主题数量
     */
    long getThemeCount();

    /**
     * 全部启用的主题
     */
    List<DictThemeDO> listEnabled();


}
