package org.rundeck.util.api.responses.nodes

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class Node {
    @JsonProperty("nodename")
    String nodename
    @JsonProperty("hostname")
    String hostname
    @JsonProperty("osFamily")
    String osFamily
    @JsonProperty("osVersion")
    String osVersion
    @JsonProperty("osArch")
    String osArch
    @JsonProperty("description")
    String description
    @JsonProperty("osName")
    String osName
    @JsonProperty("username")
    String username
    @JsonProperty("tags")
    String tags
    @JsonProperty("type")
    String type

    // Map any unknown properties as attributes
    @JsonAnySetter
    @JsonAnyGetter
    Map<String, String> attributes = [:]
}
