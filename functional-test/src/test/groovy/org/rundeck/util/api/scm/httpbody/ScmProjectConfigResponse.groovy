package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty

class ScmProjectConfigResponse {
    @JsonProperty
    Map<String, String> config

    @JsonProperty
    Boolean enabled

    @JsonProperty
    String integration

    @JsonProperty
    String project

    @JsonProperty
    String type
}
