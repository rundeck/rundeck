package org.rundeck.tests.functional.api.ResponseModels

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
}
