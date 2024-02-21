package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo

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

        @JsonProperty
        String gitPasswordPath

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

    static GitExportSetupRequest defaultRequest(){
        return new GitExportSetupRequest([config: [
                dir : "/home/rundeck/localRepoDirExample/ScmExport",
                url : "/url/to/remote/example",
                committerName :"Git Test",
                committerEmail :"A@test.com",
                pathTemplate :'${job.group}${job.name}-${job.id}.${config.format}',
                format :"xml",
                branch :"master",
                strictHostKeyChecking :"yes"
        ]])
    }

    GitExportSetupRequest forProject(String project){
        this.config.dir = "/home/rundeck/${project}/ScmExport"

        return this
    }

    GitExportSetupRequest withRepo(GiteaApiRemoteRepo remoteRepo){
        this.config.url = remoteRepo.getRepoUrlForRundeck()
        this.config.gitPasswordPath = remoteRepo.repoPassStoragePathForRundeck
        return this
    }
}
