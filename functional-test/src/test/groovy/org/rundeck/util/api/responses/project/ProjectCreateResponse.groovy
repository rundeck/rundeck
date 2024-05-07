package org.rundeck.util.api.responses.project

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.ToString

@ToString
class ProjectCreateResponse {
    String url
    String name
    String description

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX")
    String created

    @JsonProperty("config")
    private Map<String, Object> config = new HashMap<>()

    @JsonAnySetter
    void setOptions(String key, Object value) {
        config.put(key, value);
    }

    @JsonIgnore
    Object getOptions(String key) {
        return config.get(key);
    }
}
