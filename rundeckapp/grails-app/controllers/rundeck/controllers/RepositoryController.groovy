package rundeck.controllers

import grails.converters.JSON

class RepositoryController {

    def verbClient
    def repositoryPluginService

    def index() { }

    def listRepositories() {
        render verbClient.repositoryManager.listRepositories() as JSON
    }

    def listArtifacts() {
        String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
        if(!repoName) {
            specifyRepoError()
            return
        }
        println "listing for repoName: ${repoName}"
        render verbClient.listArtifacts(repoName,params.offset?.toInteger(),params.limit?.toInteger()) as JSON
    }

    def uploadArtifact() {
        String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
        if(!repoName) {
            specifyRepoError()
            return
        }

        def result = verbClient.uploadArtifact(repoName,request.inputStream)
        if(result.batchSucceeded()) {
            def successMsg = [msg:"Upload succeeded"]
            render successMsg as JSON
        } else {
            def errors = [:]
            result.messages.each {
                errors[it.code] = it.message
            }
            response.setStatus(400)
            render errors as JSON
        }
    }



    def installArtifact() {
        String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
        if(!repoName) {
            specifyRepoError()
            return
        }
        def result = verbClient.installArtifact(repoName, params.artifactId, params.artifactVersion)
        repositoryPluginService.syncInstalledArtifactsToPluginTarget()
        if(result.batchSucceeded()) {
            def successMsg = [msg:"Artifact Installed"]
            render successMsg as JSON
        } else {
            def errors = [:]
            result.messages.each {
                errors[it.code] = it.message
            }
            response.setStatus(400)
            render errors as JSON
        }
    }

    private def getOnlyRepoInListOrNullIfMultiple() {
        List<String> repoNames = verbClient.repositoryManager.listRepositories()
        if(repoNames.isEmpty() || repoNames.size() > 1) return null
        return repoNames[0]
    }

    private def specifyRepoError() {
        response.setStatus(400)
        def err = [error:"You must specify a repository"]
        render err as JSON
    }
}
