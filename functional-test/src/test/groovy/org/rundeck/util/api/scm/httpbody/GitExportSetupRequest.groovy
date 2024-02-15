package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty
import org.rundeck.util.api.scm.GitLocalServerRepoCreator

class GitExportSetupRequest {
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

    static GitExportSetupRequest defaultRequest(String forProject = 'localRepoDirExmpl', Map<String, String> configs = [:]){
        def defaultConfig = [
                dir : "/home/rundeck/${forProject}/ScmExport",
                url : "${GitLocalServerRepoCreator.REPO_TEMPLATE_PATH}",
                committerName :"Git Test",
                committerEmail :"A@test.com",
                pathTemplate :'${job.group}${job.name}-${job.id}.${config.format}',
                format :"xml",
                branch :"master",
                strictHostKeyChecking :"yes"
        ]

        configs.each {defaultConfig[it.key] = it.value }

        return new GitExportSetupRequest([config: defaultConfig])
    }
}
