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

        Map toMap(){
            return [
                    dir: dir,
                    url: url,
                    committerName: committerName,
                    committerEmail: committerEmail,
                    pathTemplate: pathTemplate,
                    format: format,
                    branch: branch,
                    strictHostKeyChecking: strictHostKeyChecking
            ]
        }
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
