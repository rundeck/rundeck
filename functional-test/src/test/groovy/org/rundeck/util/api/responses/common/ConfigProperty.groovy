package org.rundeck.util.api.responses.common

import com.fasterxml.jackson.annotation.JsonProperty

class ConfigProperty {
    @JsonProperty("key")
    String key
    @JsonProperty("value")
    String value
}
