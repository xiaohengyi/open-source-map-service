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
@TableName("DICT_KEYWORD")
public class DictKeywordDO {
    @TableId(value = "ID", type = IdType.INPUT)
    private String id;

    @TableField("KEYWORD")
    private String keyword;

    @TableField("DESCRIPTION")
    private String description;
}
