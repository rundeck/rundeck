package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Response

class ScmPluginsListResponse {

    @JsonProperty
    String integration

    @JsonProperty
    List<ScmPlugin> plugins

    static class ScmPlugin {
        @JsonProperty
        Boolean configured

        @JsonProperty
        String description

        @JsonProperty
        Boolean enabled

        @JsonProperty
        String title

        @JsonProperty
        String type
    }
}
