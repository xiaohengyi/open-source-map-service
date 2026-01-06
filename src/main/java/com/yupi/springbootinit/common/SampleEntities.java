package com.yupi.springbootinit.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SampleEntities {
    private List<String> persons;
    private List<String> orgs;
    private List<String> locations;
    private List<String> equipment;
    private List<String> keywords;
}
