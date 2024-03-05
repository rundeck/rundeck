package org.rundeck.util.api.responses.project

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
