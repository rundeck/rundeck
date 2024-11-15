package com.dtolabs.rundeck.net.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectInfo {
    public Boolean successful;

    public String name;
    public String description;
    public String url;
    public Map<String, String> config;
}
