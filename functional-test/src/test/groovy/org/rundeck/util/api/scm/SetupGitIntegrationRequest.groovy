package org.rundeck.util.api.scm

import com.fasterxml.jackson.annotation.JsonProperty

class SetupGitIntegrationRequest {
    @JsonProperty
    Config config

    static class Config {
        @JsonProperty
        String dir

        @JsonProperty
        String url

        @JsonProperty
        String committerName

        @JsonProperty
        String committerEmail

        @JsonProperty
        String pathTemplate

        @JsonProperty
        String format

        @JsonProperty
        String branch

        @JsonProperty
        String strictHostKeyChecking
    }
}
