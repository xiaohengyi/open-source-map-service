package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("DICT_CHANNEL")
public class DictChannelDO {

    @ApiModelProperty("主键ID")
    @TableId(value = "ID", type = IdType.INPUT)
    private String id;

    @ApiModelProperty("渠道名称")
    @TableField("NAME")
    private String name;

    @ApiModelProperty("渠道编码（可选）")
    @TableField("CODE")
    private String code;

    @ApiModelProperty("说明")
    @TableField("DESCRIPTION")
    private String description;

    @ApiModelProperty("状态：1启用 0停用")
    @TableField("STATUS")
    private Integer status;
}
