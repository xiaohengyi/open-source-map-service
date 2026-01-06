package com.yupi.springbootinit.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DictChannelSaveDTO {

    /** 传则更新；不传则新增 */
    private String id;

    @NotBlank(message = "渠道名称不能为空")
    private String name;

    /** 可选：建议填 INTERNET/SELLING/INTERNAL 这类 */
    private String code;

    private String description;

    /** 可选：默认 1 */
    private Integer status;
}
