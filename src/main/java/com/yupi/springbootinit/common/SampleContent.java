package com.yupi.springbootinit.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SampleContent {
    private String title;
    private String sourceUrl;
    private String publishedAt;
    private String author;
    private String section;
    private String summary;
    private String bodyText;

    // 兼容：少数源可能会把 language 放 content
    private String language;
}
