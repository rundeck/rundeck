package org.rundeck.util.api.scm

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Response

class SetupIntegrationResponse {
    @JsonProperty
    String message

    @JsonProperty
    String nextAction

    @JsonProperty
    Boolean success

    @JsonProperty
    Map<String, String> validationErrors

    static SetupIntegrationResponse extractFromResponse(Response response){
        new ObjectMapper().readValue(
                response.body().string(),
                SetupIntegrationResponse.class
        )
    }
}
