package org.rundeck.tests.functional.api.ResponseModels

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class ProjectSource {
    @JsonProperty("index")
    int index
    @JsonProperty("type")
    String type
    @JsonProperty("resources")
    Resources resources

    static class Resources {
        String href
        boolean writeable
        String editPermalink
        String syntaxMimeType
        boolean empty
        String description
    }
}
