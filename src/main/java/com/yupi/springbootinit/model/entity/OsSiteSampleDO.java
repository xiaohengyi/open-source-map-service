package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.yupi.springbootinit.injector.DmLocalDateTimeTypeHandler;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.apache.ibatis.type.ClobTypeHandler;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("OS_SITE_SAMPLE")
public class OsSiteSampleDO {

    @ApiModelProperty("样例主键ID（字符串，Base64或UUID等生成）")
    @TableId(value = "ID", type = IdType.INPUT)
    private String id;

    @ApiModelProperty("数据源ID（逻辑外键，OS_SITE.ID）")
    @TableField("SITE_ID")
    private String siteId;

    @ApiModelProperty("样例标题（冗余，便于列表展示）")
    @TableField("TITLE")
    private String title;

    @ApiModelProperty("样例来源URL（冗余）")
    @TableField("SOURCE_URL")
    private String sourceUrl;

    @ApiModelProperty("样例语言（zh/vi/en 等）")
    @TableField("LANGUAGE")
    private String language;

    @ApiModelProperty("发布时间（冗余）")
    @TableField(value = "PUBLISHED_AT", typeHandler = DmLocalDateTimeTypeHandler.class)
    private LocalDateTime publishedAt;

    @ApiModelProperty("摘要（冗余，便于列表展示）")
    @TableField("SUMMARY")
    private String summary;

    @ApiModelProperty("完整样例JSON（通用壳），CLOB 存储")
    @TableField(value = "SAMPLE_JSON", typeHandler = ClobTypeHandler.class)
    private String sampleJson;

    @ApiModelProperty("创建时间")
    @TableField(value = "CREATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    @TableField(value = "UPDATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @ApiModelProperty("逻辑删除：0=未删 1=已删")
    @TableLogic(value = "0", delval = "1")
    @TableField("IS_DELETE")
    private Integer isDelete;
}
