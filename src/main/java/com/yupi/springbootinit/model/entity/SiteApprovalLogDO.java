package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.yupi.springbootinit.injector.DmLocalDateTimeTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("OS_SITE_APPROVAL_LOG")
public class SiteApprovalLogDO {

    @TableId(value = "ID", type = IdType.INPUT)
    private String id;

    @TableField("APPLY_ID")
    private String applyId;

    /** SUBMIT / APPROVE / REJECT */
    @TableField("ACTION")
    private String action;

    @TableField("OP_USER_ID")
    private String opUserId;

    @TableField("OP_USER_NAME")
    private String opUserName;

    @TableField("DETAIL")
    private String detail;

    @TableField(value = "CREATED_AT", typeHandler = DmLocalDateTimeTypeHandler.class,fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
