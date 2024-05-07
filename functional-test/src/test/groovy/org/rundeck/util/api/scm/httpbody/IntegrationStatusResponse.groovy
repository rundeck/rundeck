package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty
import org.rundeck.util.common.scm.ScmIntegration

class IntegrationStatusResponse {
    @JsonProperty
    List<String> actions

    @JsonProperty
    String integration

    @JsonProperty
    String message

    @JsonProperty
    String project

    @JsonProperty
    String synchState

    ScmIntegration getIntegration(){
        return ScmIntegration.getEnum(integration)
    }
}
