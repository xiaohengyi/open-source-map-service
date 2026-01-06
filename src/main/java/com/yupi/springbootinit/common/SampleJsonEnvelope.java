package com.yupi.springbootinit.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SampleJsonEnvelope {

    // 兼容：有些源可能把这些字段放根节点
    private String id;
    private String siteName;
    private String siteUrl;
    private String language;

    private String title;
    private String sourceUrl;
    private String publishedAt;
    private String summary;

    private SampleContent content;
    private SampleEntities entities;

    // domain 字段差异很大：用 JsonNode 容纳任意结构
    private JsonNode domain;

    private SampleCapture capture;


}
