package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.yupi.springbootinit.injector.DmLocalDateTimeTypeHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导入任务 DO
 * 表：OS_IMPORT_JOB
 * 主键 String-UUID（应用生成）
 */
@Data
@TableName("OS_IMPORT_JOB")
@ApiModel("导入任务")
public class ImportJobDO {

    @ApiModelProperty("任务ID（String-UUID）")
    @TableId(type = IdType.INPUT)
    private String id;

    @ApiModelProperty("原始文件名")
    private String fileName;

    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty("总行数")
    private Integer rowsTotal;

    @ApiModelProperty("可提交条数（轻校验通过）")
    private Integer rowsReady;

    @ApiModelProperty("跳过条数（轻校验失败）")
    private Integer rowsSkipped;

    @ApiModelProperty("重复条数（标记 DUP）")
    private Integer rowsDup;

    @ApiModelProperty("任务状态：NEW/READY/COMMITTING/PARTIAL/DONE/FAILED/CANCELED")
    private String status;

    @ApiModelProperty("任务所有者用户ID")
    private String ownerUserId;

    @ApiModelProperty("任务所有者用户名")
    private String ownerUserName;

    @ApiModelProperty("创建时间")
    @TableField(value = "CREATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    @TableField(value = "UPDATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
