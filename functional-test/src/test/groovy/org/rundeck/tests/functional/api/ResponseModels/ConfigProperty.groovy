package org.rundeck.tests.functional.api.ResponseModels

import com.fasterxml.jackson.annotation.JsonProperty

class ConfigProperty {
    @JsonProperty("key")
    String key
    @JsonProperty("value")
    String value
}
