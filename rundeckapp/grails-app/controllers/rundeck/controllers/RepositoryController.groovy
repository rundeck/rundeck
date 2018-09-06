package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.converters.JSON

class RepositoryController extends ControllerBase {

    def verbClient
    def repositoryPluginService
    def pluginApiService
    def frameworkService

    def index() { }

    def listRepositories() {
        if (!authorized()) {
            specifyUnauthorizedError()
            return
        }
        render verbClient.repositoryManager.listRepositories() as JSON
    }

    def listArtifacts() {
        if (!authorized()) {
            specifyUnauthorizedError()
            return
        }
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
        if (!authorized()) {
            specifyUnauthorizedError()
            return
        }
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
        if (!authorized()) {
            specifyUnauthorizedError()
            return
        }
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
        if (!authorized()) {
            specifyUnauthorizedError()
            return
        }
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
        if (!authorized()) {
            specifyUnauthorizedError()
            return
        }
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
        if (!authorized()) {
            specifyUnauthorizedError()
            return
        }
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
        if (!authorized()) {
            specifyUnauthorizedError()
            return
        }
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

    private def specifyUnauthorizedError() {
        response.setStatus(400)
        def err = [error:"You are not authorized to perform this action"]
        render err as JSON
    }

    private boolean authorized() {
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        frameworkService.authorizeApplicationResourceType(authContext,"system",
                                                                               AuthConstants.ACTION_ADMIN)
    }
}
