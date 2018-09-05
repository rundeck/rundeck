package rundeck.controllers

import grails.converters.JSON

class RepositoryController {

    def verbClient
    def repositoryPluginService
    def pluginApiService

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
        def installedPluginIds = pluginApiService.listPlugins()*.providers.collect { it.artifactId }.flatten()
        def artifacts = verbClient.listArtifacts(repoName,params.offset?.toInteger(),params.limit?.toInteger())
        artifacts.each {
            it.results.each {
                it.installed = installedPluginIds.contains(it.id)
            }
        }

        render artifacts as JSON
    }

    def listInstalledArtifacts() {
        String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
        if(!repoName) {
            specifyRepoError()
            return
        }
        def installedPluginIds = pluginApiService.listPlugins()*.providers.collect { it.artifactId }.flatten()
        def artifacts = verbClient.listArtifacts()*.results.flatten()
        def installedArtifacts = []
        artifacts.each {
            if(installedPluginIds.contains(it.id)) {
             installedArtifacts.add([artifactId:it.id, artifactName:it.name, version: it.currentVersion])
            }
        }
        render installedArtifacts as JSON
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
        if(result.batchSucceeded()) {
            repositoryPluginService.removeOldPlugin(verbClient.getArtifact(repoName, params.artifactId, null))
            repositoryPluginService.syncInstalledArtifactsToPluginTarget()
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

    def uninstallArtifact() {
        String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
        if(!repoName) {
            specifyRepoError()
            return
        }
        def responseMsg = [:]
        try {
            def artifact = verbClient.getArtifact(repoName, params.artifactId,null)
            repositoryPluginService.uninstallArtifact(artifact)
            responseMsg.msg = "Artifact Uninstalled"
        } catch(Exception ex) {
            log.error("Unable to uninstall artifact.",ex)
            responseMsg.err = "Failed to uninstall artifact: ${ex.message}"
            response.setStatus(400)
        }
        render responseMsg as JSON
    }

    def regenerateManifest() {
        String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
        if(!repoName) {
            specifyRepoError()
            return
        }
        verbClient.refreshRepositoryManifest(repoName)
        def successMsg = [msg:"Refreshed Repository ${repoName}"]
        render successMsg as JSON
    }

    def syncInstalledArtifactsToRundeck() {
        repositoryPluginService.syncInstalledArtifactsToPluginTarget()
        def successMsg = [msg:"Resync Triggered"]
        render successMsg as JSON
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
