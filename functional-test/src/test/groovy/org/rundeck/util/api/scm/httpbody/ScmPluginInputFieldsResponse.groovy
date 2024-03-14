package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty
import org.rundeck.util.api.scm.httpbody.common.InputField
import org.rundeck.util.common.scm.ScmIntegration

class ScmPluginInputFieldsResponse {

    @JsonProperty
    String integration

    @JsonProperty
    String type

    @JsonProperty
    List<InputField> fields

    ScmIntegration getIntegration(){
        return ScmIntegration.getEnum(integration)
    }
}
