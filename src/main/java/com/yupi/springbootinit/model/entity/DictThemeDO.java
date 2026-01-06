package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("DICT_THEME")
public class DictThemeDO {
    @TableId(value = "ID", type = IdType.INPUT)
    private String id;

    @TableField("NAME")
    private String name;

    @TableField("CODE")
    private String code;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("STATUS")
    private Integer status; // 1=启用 0=停用
}
