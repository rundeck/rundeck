package repository

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.plugins.ServiceTypes
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.client.exceptions.ArtifactNotFoundException
import com.rundeck.repository.client.manifest.search.ManifestSearchBuilder
import com.rundeck.repository.definition.RepositoryDefinition
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
            if (!authorized(PLUGIN_RESOURCE,"read")) {
                specifyUnauthorizedError()
                return
            }
            String repoName = params.repoName

            def installedPluginIds = pluginApiService.listInstalledPluginIds()
            def artifacts = repoName ? repoClient.listArtifactsByRepository(repoName,params.offset?.toInteger(),params.limit?.toInteger())
                                     : repoClient.listArtifacts(params.offset?.toInteger(),params.limit?.toInteger())
            artifacts.each {
                it.results.each {
                    it.installed = it.installId ? installedPluginIds.keySet().contains(it.installId) : false
                    if(it.installed) {
                        it.updatable = checkUpdatable(installedPluginIds[it.installId],it.currentVersion)
                        it.installedVersion = installedPluginIds[it.installId]
                    }
                }
            }

            render artifacts as JSON
        }

        def searchArtifacts() {
            if (!authorized(PLUGIN_RESOURCE,"read")) {
                specifyUnauthorizedError()
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
                    it.installed = it.installId ? installedPluginIds.keySet().contains(it.installId) : false
                }
            }
            def searchResponse = [:]
            searchResponse.warnings = sb.msgs
            searchResponse.artifacts = artifacts

            render searchResponse as JSON
        }

        def listInstalledArtifacts() {
            if (!authorized(PLUGIN_RESOURCE,"read")) {
                specifyUnauthorizedError()
                return
            }

            def installedPluginIds = pluginApiService.listInstalledPluginIds()
            def artifacts = repoClient.listArtifacts(0,-1)*.results.flatten()
            def installedArtifacts = []
            artifacts.each {
                if(installedPluginIds.keySet().contains(it.installId)) {
                    installedArtifacts.add([artifactId:it.installId, artifactName:it.name, version: installedPluginIds[it.installId]])
                }
            }
            render installedArtifacts as JSON
        }

        def listPluginTypes() {
            SortedSet<String> types = [] as SortedSet
            ServiceTypes.getPluginTypesMap().keySet().each { name ->
                types.add(name.replaceAll(/([A-Z]+)/, ' $1').replaceAll(/^ /, ''))
            }
            render types as JSON
        }

        def uploadArtifact() {
            if (!authorized(PLUGIN_RESOURCE,"install")) {
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
                def errors = []
                result.messages.each {
                    errors.add([code:it.code,msg:it.message])
                }
                pkg.errors = errors
                response.setStatus(400)
                render pkg as JSON
            }
        }



        def installArtifact() {
            if (!authorized(PLUGIN_RESOURCE,"install")) {
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
                def errors = []
                result.messages.each {
                    errors.add([code:it.code,msg:it.message])
                }
                pkg.errors = errors
                response.setStatus(400)
                render pkg as JSON
            }
        }

        def uninstallArtifact() {
            if (!authorized(PLUGIN_RESOURCE,"uninstall")) {
                specifyUnauthorizedError()
                return
            }
            if(!params.artifactId) {
                response.setStatus(400)
                def err = [error:"You must specify an artifact id"]
                render err as JSON
            }
            String installedVersion = pluginApiService.listInstalledPluginIds()[params.artifactId]
            def responseMsg = [:]
            try {
                RepositoryArtifact artifact = null
                for(RepositoryDefinition repoDef : repoClient.listRepositories()) {
                    try {
                        artifact = repoClient.getArtifact(repoDef.repositoryName, params.artifactId, installedVersion)
                    } catch(ArtifactNotFoundException anfe) {} //the repository does not have the artifact. That could be normal.
                    if(artifact) break;
                }
                if(!artifact) throw new Exception("Could not find artifact information for: ${params.artifactId}. Please check that the supplied artifact id is correct.")
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
            if (!authorized(PLUGIN_RESOURCE,"install")) {
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

        private boolean checkUpdatable(installedVersion,latestVersion) {
            String cleanInstalledVer = installedVersion.replaceAll(~/[^\d]/,"")
            String cleanLatestVer = latestVersion.replaceAll(~/[^\d]/,"")
            long installed = convertToNumber(cleanInstalledVer, "Installed")
            long latest = convertToNumber(cleanLatestVer,"Current")
            return latest > installed
        }

        private long convertToNumber(String val, String prefix) {
            try {
                Long.parseLong(val)
            } catch(NumberFormatException nfe) {
                log.error("${prefix} plugin version value can't be converted to a number. Can't check updatability. Value: ${val}",nfe)
            }
        }

        @PackageScope
        boolean authorized(Map resourceType = ADMIN_RESOURCE,String action = "admin") {
            List authorizedActions = ["admin"]
            if(action != "admin") authorizedActions.add(action)
            AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
            frameworkService.authorizeApplicationResourceAny(authContext,resourceType,authorizedActions)

        }

        private static Map PLUGIN_RESOURCE = Collections.unmodifiableMap(AuthorizationUtil.resourceType("plugin"))
        private static Map ADMIN_RESOURCE = Collections.unmodifiableMap(AuthorizationUtil.resourceType("admin"))
}
