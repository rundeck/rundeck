package org.rundeck.util.api.responses.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

class ErrorResponse {
    String errorCode
    int apiversion
    boolean error
    String message

    @JsonCreator
    ErrorResponse(
            @JsonProperty("errorCode") String errorCode,
            @JsonProperty("apiversion") int apiversion,
            @JsonProperty("error") boolean error,
            @JsonProperty("message") String message
    ) {
        this.errorCode = errorCode
        this.apiversion = apiversion
        this.error = error
        this.message = message
    }

    static ErrorResponse fromJson(String json) {
        new ObjectMapper().readValue(json, ErrorResponse)
    }

    String toJson() {
        new ObjectMapper().writeValueAsString(this)
    }
}
