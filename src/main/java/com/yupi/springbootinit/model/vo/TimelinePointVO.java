package com.yupi.springbootinit.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 时间轴统计点
 * 7天：按天返回 -> date 非空；半年/一年：按月返回 -> month 非空
 */
@Data
@Builder
@ApiModel("接入时间轴统计点")
public class TimelinePointVO {

    @ApiModelProperty("按天维度时使用，形如 2025-09-01")
    private LocalDate date;

    @ApiModelProperty("按月维度时使用，形如 2025-09")
    private YearMonth month;

    @ApiModelProperty("数量")
    private Long count;
}
