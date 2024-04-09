package org.rundeck.util.api.responses.execution

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class RunCommand {
    @JsonProperty("message")
    String message
    @JsonProperty("execution")
    Execution execution
}
