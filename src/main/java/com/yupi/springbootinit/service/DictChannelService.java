package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.dto.DictChannelSaveDTO;
import com.yupi.springbootinit.model.entity.DictChannelDO;

import java.util.List;

public interface DictChannelService extends IService<DictChannelDO> {

    List<String> listEnabledChannelNames();

    DictChannelDO saveChannel(DictChannelSaveDTO dto);

    /** “删除”这里做成停用（STATUS=0），更符合字典表设计 */
    void deleteChannel(String id);

    /** 管理端查看全部（含停用） */
    List<DictChannelDO> listAll();

    /** 新增：单独更新状态 */
    void updateStatus(String id, Integer status);
}
