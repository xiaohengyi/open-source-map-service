package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("导入任务VO")
public class ImportJobVO {

    @ApiModelProperty("任务ID")
    private String id;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件大小")
    private Long fileSize;

    @ApiModelProperty("总行数")
    private Integer rowsTotal;

    @ApiModelProperty("可提交条数")
    private Integer rowsReady;

    @ApiModelProperty("跳过条数")
    private Integer rowsSkipped;

    @ApiModelProperty("重复条数")
    private Integer rowsDup;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("任务所有者ID")
    private String ownerUserId;

    @ApiModelProperty("任务所有者")
    private String ownerUserName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
