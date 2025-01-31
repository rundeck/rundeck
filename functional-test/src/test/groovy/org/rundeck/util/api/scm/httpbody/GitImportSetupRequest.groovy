package org.rundeck.util.api.scm.httpbody

import com.fasterxml.jackson.annotation.JsonProperty
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo

class GitImportSetupRequest implements GitSetupRequest<GitImportSetupRequest> {
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

        @JsonProperty
        String apiToken

        @JsonProperty
        String project

        @JsonProperty
        String useFilePattern

        @JsonProperty
        String filePattern

        @JsonProperty
        String importUuidBehavior

        @JsonProperty
        String pullAutomatically
    }

    static GitImportSetupRequest defaultRequest(){
        return new GitImportSetupRequest([config: [
                dir : "/home/rundeck/localRepoDirExample/ScmImport",
                pathTemplate :'${job.group}${job.name}-${job.id}.${config.format}',
                branch :"master",
                format :"xml",
                strictHostKeyChecking :"yes",
                url : "/url/to/remote/example",
                useFilePattern       : 'true',
                filePattern          : '.*\\.xml',
                importUuidBehavior   : 'preserve',
                pullAutomatically    : 'false'
        ]])
    }

    @Override
    GitImportSetupRequest forProject(String project){
        this.config.dir = "/home/rundeck/${project}/ScmImport"

        return this
    }

    @Override
    GitImportSetupRequest withRepo(GiteaApiRemoteRepo remoteRepo){
        this.config.url = remoteRepo.getRepoUrlForRundeck()
        this.config.gitPasswordPath = remoteRepo.repoPassStoragePathForRundeck
        return this
    }
}
