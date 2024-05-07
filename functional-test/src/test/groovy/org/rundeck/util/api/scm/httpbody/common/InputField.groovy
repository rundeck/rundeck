package org.rundeck.util.api.scm.httpbody.common

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

class InputField {
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

    Map toMap() {
        new ObjectMapper().convertValue(this, new TypeReference<Map<String, Object>>() {})
    }
}
