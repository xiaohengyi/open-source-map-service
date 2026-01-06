package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.yupi.springbootinit.injector.DmLocalDateTimeTypeHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导入明细 DO
 * 表：OS_IMPORT_ITEM
 * 主键 String-UUID（应用生成）
 * 注意：Excel 字段不再逐列入库，统一放 RAW_JSON
 */
@Data
@TableName("OS_IMPORT_ITEM")
@ApiModel("导入明细")
public class ImportItemDO {

    @ApiModelProperty("明细ID（String-UUID）")
    @TableId(type = IdType.INPUT)
    private String id;

    @ApiModelProperty("所属任务ID（String-UUID）")
    private String jobId;

    @ApiModelProperty("Excel 中的行号（从 1 开始，含表头则从 2 开始计算）")
    private Integer rowNo;

    @ApiModelProperty("原始行对象 JSON（siteName/url/mainCountryCode/themeIdsText/…）")
    private String rawJson;

    @ApiModelProperty("轻校验状态：PENDING/VALID/INVALID/SUBMITTED")
    private String validStatus;

    @ApiModelProperty("轻校验提示/错误信息")
    private String validMsg;

    @ApiModelProperty("是否标记为重复（0/1）")
    private Integer dupFlag;

    @ApiModelProperty("创建时间")
    @TableField(value = "CREATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    @TableField(value = "UPDATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
