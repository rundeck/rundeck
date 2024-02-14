package org.rundeck.util.api.scm

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Response

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

    static ScmProjectConfigResponse extractFromResponse(Response response){
        new ObjectMapper().readValue(
                response.body().string(),
                ScmProjectConfigResponse.class
        )
    }
}
