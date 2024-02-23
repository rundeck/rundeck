package org.rundeck.util.api.scm.httpbody
import com.fasterxml.jackson.annotation.JsonProperty

class ScmActionPerformRequest {

    @JsonProperty
    Map<String, String> input

    @JsonProperty
    List<String> jobs

    @JsonProperty
    List<String> items

    @JsonProperty
    List<String> deleted

    @JsonProperty
    List<String> deletedJobs
}
