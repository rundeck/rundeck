package org.rundeck.util.api.scm

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Response

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
