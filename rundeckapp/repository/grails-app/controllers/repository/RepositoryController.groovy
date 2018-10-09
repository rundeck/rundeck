package repository

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.rundeck.repository.client.manifest.search.ManifestSearchBuilder
import com.rundeck.repository.manifest.search.ManifestSearch
import grails.converters.JSON
import groovy.transform.PackageScope

class RepositoryController {

        def repoClient
        def repositoryPluginService
        def pluginApiService
        def frameworkService

        def listRepositories() {
            if (!authorized()) {
                specifyUnauthorizedError()
                return
            }
            def repos = []
            repoClient.listRepositories().each {
                repos.add(name: it.repositoryName,type:it.owner.name(),enabled: it.enabled)
            }
            render repos as JSON
        }

        def listArtifacts() {
            if (!authorized("plugin","read")) {
                specifyUnauthorizedError()
                return
            }
            String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
            if(!repoName) {
                specifyRepoError()
                return
            }
            def installedPluginIds = pluginApiService.listInstalledPluginIds()
            def artifacts = repoClient.listArtifactsByRepository(repoName,params.offset?.toInteger(),params.limit?.toInteger())
            artifacts.each {
                it.results.each {
                    it.installed = installedPluginIds.contains(it.id)
                }
            }

            render artifacts as JSON
        }

        def searchArtifacts() {
            if (!authorized("plugin","read")) {
                specifyUnauthorizedError()
                return
            }
            String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
            if(!repoName) {
                specifyRepoError()
                return
            }
            String searchTerm

            if(request.JSON) {
                searchTerm = request.JSON.searchTerm
            } else {
                searchTerm = params.searchTerm
            }

            def installedPluginIds = pluginApiService.listInstalledPluginIds()
            ManifestSearchBuilder sb = new ManifestSearchBuilder()
            ManifestSearch search = sb.createSearch(searchTerm)
            search.max = params.limit?.toInteger() ?: -1
            search.offset = params.offset?.toInteger() ?: 0
            def artifacts = repoClient.searchManifests(search)

            artifacts.each {
                it.results.each {
                    it.installed = installedPluginIds.contains(it.id)
                }
            }
            def searchResponse = [:]
            searchResponse.warnings = sb.msgs
            searchResponse.artifacts = artifacts

            render searchResponse as JSON
        }

        def listInstalledArtifacts() {
            if (!authorized("plugin","read")) {
                specifyUnauthorizedError()
                return
            }
            String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
            if(!repoName) {
                specifyRepoError()
                return
            }
            def installedPluginIds = pluginApiService.listInstalledPluginIds()
            def artifacts = repoClient.listArtifacts(0,-1)*.results.flatten()
            def installedArtifacts = []
            artifacts.each {
                if(installedPluginIds.contains(it.id)) {
                    installedArtifacts.add([artifactId:it.id, artifactName:it.name, version: it.currentVersion])
                }
            }
            render installedArtifacts as JSON
        }

        def uploadArtifact() {
            if (!authorized("plugin","install")) {
                specifyUnauthorizedError()
                return
            }
            String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
            if(!repoName) {
                specifyRepoError()
                return
            }

            def result = repoClient.uploadArtifact(repoName,request.inputStream)
            if(result.batchSucceeded()) {
                def successMsg = [msg:"Upload succeeded"]
                render successMsg as JSON
            } else {
                def pkg = [:]
                def errors = [:]
                result.messages.each {
                    errors.code = it.code
                    errors.msg = it.message
                }
                pkg.errors = errors
                response.setStatus(400)
                render pkg as JSON
            }
        }



        def installArtifact() {
            if (!authorized("plugin","install")) {
                specifyUnauthorizedError()
                return
            }
            String repoName = params.repoName ?: getOnlyRepoInListOrNullIfMultiple()
            if(!repoName) {
                specifyRepoError()
                return
            }
            def result = repoClient.installArtifact(repoName, params.artifactId, params.artifactVersion)
            if(result.batchSucceeded()) {
                repositoryPluginService.removeOldPlugin(repoClient.getArtifact(repoName, params.artifactId, null))
                repositoryPluginService.syncInstalledArtifactsToPluginTarget()
                def successMsg = [msg:"Plugin Installed"]
                render successMsg as JSON
            } else {
                def pkg = [:]
                def errors = [:]
                result.messages.each {
                    errors.code = it.code
                    errors.msg = it.message
                }
                pkg.errors = errors
                response.setStatus(400)
                render pkg as JSON
            }
        }

        def uninstallArtifact() {
            if (!authorized("plugin","uninstall")) {
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
                def artifact = repoClient.getArtifact(repoName, params.artifactId,null)
                repositoryPluginService.uninstallArtifact(artifact)
                responseMsg.msg = "Plugin Uninstalled"
            } catch(Exception ex) {
                log.error("Unable to uninstall plugin.",ex)
                responseMsg.errors = [[code:"plugin.uninstall.failure", msg: "Failed to uninstall plugin: ${ex.message}"]]
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
            repoClient.refreshRepositoryManifest(repoName)
            def successMsg = [msg:"Refreshed Repository ${repoName}"]
            render successMsg as JSON
        }

        def syncInstalledArtifactsToRundeck() {
            if (!authorized("plugin","install")) {
                specifyUnauthorizedError()
                return
            }
            repositoryPluginService.syncInstalledArtifactsToPluginTarget()
            def successMsg = [msg:"Resync Triggered"]
            render successMsg as JSON
        }

        private def getOnlyRepoInListOrNullIfMultiple() {
            List<String> repoNames = repoClient.listRepositories().findAll { it.enabled }.collect { it.repositoryName }
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

        @PackageScope
        boolean authorized(String resourceType = "admin",String action = "admin") {
            AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
            frameworkService.authorizeApplicationResourceType(authContext,resourceType,
                                                              action)
        }
}
