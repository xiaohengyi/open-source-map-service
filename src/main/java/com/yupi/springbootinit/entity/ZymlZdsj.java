package com.yupi.springbootinit.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TableName("ZYML_ZDSJ")
public class ZymlZdsj{

    @TableId("ID")
    @Schema(description = "字典id")
    private String id;

    @TableField("PARENT_ID")
    @Schema(description = "父级id")
    private String parentId;

    @TableField("DICT_NAME")
    @Schema(description = "字典名称")
    private String dictName;

    @TableField("DICT_CONTENT")
    @Schema(description = "字典内容")
    private String dictContent;

    @TableField("DICT_TYPE")
    @Schema(description = "字典类型")
    private String dictType;

}
