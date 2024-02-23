package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty

class SetupIntegrationResponse {
    @JsonProperty
    String message

    @JsonProperty
    String nextAction

    @JsonProperty
    Boolean success

    @JsonProperty
    Map<String, String> validationErrors
}
