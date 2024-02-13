package org.rundeck.util.api.scm

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.groovy.util.BeanUtils

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

    static SetupGitIntegrationRequest defaultRequest(){
        return new SetupGitIntegrationRequest([config: [
                dir : "/home/rundeck/localRepoDirExmpl",
                url :"/url/to/remoteRepoExmpl",
                committerName :"Git Test",
                committerEmail :"A@test.com",
                pathTemplate :'${job.group}${job.name}-${job.id}.${config.format}',
                format :"xml",
                branch :"master",
                strictHostKeyChecking :"yes"
        ]])
    }
}
