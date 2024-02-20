package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty

class ScmJobStatusResponse {

    @JsonProperty
    String id

    @JsonProperty
    ArrayList actions

    @JsonProperty
    def commit

    @JsonProperty
    String integration

    @JsonProperty
    String message

    @JsonProperty
    String project

    @JsonProperty
    String synchState
}