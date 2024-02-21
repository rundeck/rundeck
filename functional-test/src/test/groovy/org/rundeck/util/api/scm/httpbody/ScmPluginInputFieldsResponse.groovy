package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty
import org.rundeck.util.api.scm.httpbody.common.InputField

class ScmPluginInputFieldsResponse {

    @JsonProperty
    String integration

    @JsonProperty
    String type

    @JsonProperty
    List<InputField> fields
}
