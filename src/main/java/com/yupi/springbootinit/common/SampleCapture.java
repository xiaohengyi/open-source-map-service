package com.yupi.springbootinit.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SampleCapture {
    private String capturedAt;
    private String method;
    private String notes;
}
