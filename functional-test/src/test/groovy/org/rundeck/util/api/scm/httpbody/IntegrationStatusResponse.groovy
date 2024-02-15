package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty

class IntegrationStatusResponse {
    @JsonProperty
    List<String> actions

    @JsonProperty
    String integration

    @JsonProperty
    String message

    @JsonProperty
    String project

    @JsonProperty
    String synchState
}
