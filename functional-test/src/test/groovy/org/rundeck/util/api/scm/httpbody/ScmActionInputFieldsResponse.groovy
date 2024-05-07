package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.api.scm.httpbody.common.InputField
import org.rundeck.util.common.scm.ScmActionId
import org.rundeck.util.common.scm.ScmIntegration

class ScmActionInputFieldsResponse {
    @JsonProperty
    String actionId

    @JsonProperty
    String description

    @JsonProperty
    List<InputField> fields

    @JsonProperty
    String integration

    @JsonProperty
    String title

    @JsonProperty
    List<ImportItems> importItems

    @JsonProperty
    List<ExportItems> exportItems

    ScmIntegration getIntegration(){
        ScmIntegration.getEnum(integration)
    }

    ScmActionId getActionId(){
        ScmActionId.getEnum(actionId)
    }


    static class ImportItems extends CommonItems {
        @JsonProperty
        Boolean tracked
    }

    static class ExportItems extends CommonItems {
        @JsonProperty
        String originalId

        @JsonProperty
        Boolean renamed
    }

    static class CommonItems {
        @JsonProperty
        Boolean deleted

        @JsonProperty
        String itemId

        @JsonProperty
        JobData job

        @JsonProperty
        String status

        static class JobData {
            @JsonProperty
            String groupPath

            @JsonProperty
            String jobId

            @JsonProperty
            String jobName
        }

        Map toMap() {
            new ObjectMapper().convertValue(this, new TypeReference<Map<String, Object>>() {})
        }
    }
}



