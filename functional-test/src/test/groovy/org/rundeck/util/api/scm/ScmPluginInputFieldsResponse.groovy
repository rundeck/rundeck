package org.rundeck.util.api.scm

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Response

class ScmPluginInputFieldsResponse {

    @JsonProperty
    String integration

    @JsonProperty
    String type

    @JsonProperty
    List<InputField> fields

    static class InputField {
        @JsonProperty
        String defaultValue

        @JsonProperty
        String description

        @JsonProperty
        String name

        @JsonProperty
        Map<String, String> renderingOptions

        @JsonProperty
        Boolean required

        @JsonProperty
        String scope

        @JsonProperty
        String title

        @JsonProperty
        String type

        @JsonProperty
        List<String> values
    }

    static ScmPluginInputFieldsResponse extractFromResponse(Response response){
        new ObjectMapper().readValue(
                response.body().string(),
                ScmPluginInputFieldsResponse.class
        )
    }
}
