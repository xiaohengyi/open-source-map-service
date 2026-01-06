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
@TableName("USER_QUERY_LOG")
public class UserQueryLogDO {

    @TableId(value = "ID", type = IdType.INPUT)
    private String id;

    @TableField("USER_ID")
    private String userId;

    @TableField("KEYWORD")
    private String keyword;

    @TableField("THEME_IDS_TEXT")
    private String themeIdsText; // 逗号拼接

    @TableField("COUNTRY_CODES_TEXT")
    private String countryCodesText; // 逗号拼接

    @TableField("MAIN_COUNTRY_CODE")
    private String mainCountryCode;

    @TableField(value = "CREATED_AT",typeHandler = DmLocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
