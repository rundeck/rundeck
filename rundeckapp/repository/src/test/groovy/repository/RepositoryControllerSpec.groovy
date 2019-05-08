package repository

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.artifact.ArtifactType
import com.rundeck.repository.artifact.SupportLevel
import com.rundeck.repository.client.artifact.RundeckRepositoryArtifact
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.search.ManifestSearchResult
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.api.RepositoryOwner
import com.rundeck.repository.client.RepositoryClient
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class RepositoryControllerSpec extends Specification implements ControllerUnitTest<RepositoryController> {

    def client

    def setup() {
        client = Mock(RepositoryClient)
        controller.repoClient = client
        controller.frameworkService = new FakeFrameworkService()
        controller.pluginApiService = new FakePluginApiService()
    }

    def cleanup() {
    }

    void "list Repositories"() {
        when:
        1 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE),
                                              new RepositoryDefinition(repositoryName: "official",owner:RepositoryOwner.RUNDECK,enabled:false)]
        controller.listRepositories()

        then:
        response.json.size() == 2
        response.json == [[name:"private",type:"PRIVATE",enabled:true],[name:"official",type:"RUNDECK",enabled:false]]

    }

    void "list artifacts no repo specified"() {
        given:
        String pluginId = PluginUtils.generateShaIdFromName("InstalledPlugin")
        controller.pluginApiService.installedPluginIds = [:]
        controller.pluginApiService.installedPluginIds[pluginId] = "1.0"

        when:
        1 * client.listArtifacts(_,_) >> testArtifactList("private")
        controller.listArtifacts()

        then:
        response.json.size() == 1
        response.json[0].repositoryName == "private"
        response.json[0].results.size() == 3
        def installed = response.json[0].results.find { it.name == "InstalledPlugin" }
        installed.installed == true
        def available = response.json[0].results.find { it.name == "Available Plugin" }
        available.installed == false

    }

    void "list artifacts without correct permission"() {
        given:
        controller.frameworkService = new FakeFrameworkService(false)

        when:
        0 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE)]
        0 * client.listArtifacts(_,_,_) >> testArtifactList("private")
        controller.listArtifacts()

        then:
        response.json == [error:"You are not authorized to perform this action"]

    }

    void "search artifacts"() {
        given:
        controller.pluginApiService.installedPluginIds = [:]

        when:
        1 * client.searchManifests(_) >> testSearch("private")
        params.searchTerm = "artifactType: script-plugin"
        controller.searchArtifacts()

        then:
        response.json.artifacts.size() == 1
        response.json.artifacts[0].repositoryName == "private"
        response.json.artifacts[0].results.size() == 1
        response.json.artifacts[0].results[0].name == "Script Plugin"
        response.json.artifacts[0].results[0].installed == false

    }

    void "list installed artifacts"() {
        given:
        def installedPluginId = PluginUtils.generateShaIdFromName("InstalledPlugin")
        controller.pluginApiService.installedPluginIds = [:]
        controller.pluginApiService.installedPluginIds[installedPluginId] = "1.0"

        when:
        1 * client.listArtifacts(_,_) >> testArtifactList("private")
        controller.listInstalledArtifacts()

        then:
        response.json.size() == 1
        def installed = response.json[0]
        installed.artifactId == installedPluginId
        installed.artifactName == "InstalledPlugin"
        installed.version == "1.0"

    }

    void "upload artifact no repo specified and only 1 repo defined"() {
        when:
        ResponseBatch successBatch = new ResponseBatch()
        successBatch.addMessage(ResponseMessage.success())
        1 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE)]
        1 * client.uploadArtifact(_,_) >> successBatch
        controller.uploadArtifact()

        then:
        response.json == ["msg":"Upload succeeded"]

    }

    void "install artifact no repo specified and only 1 repo defined"() {
        given:
        controller.repositoryPluginService = Mock(RepositoryPluginService)

        when:
        ResponseBatch successBatch = new ResponseBatch()
        successBatch.addMessage(ResponseMessage.success())
        1 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE)]
        1 * client.installArtifact(_,_,_) >> successBatch
        1 * client.getArtifact(_,_, null) >> new RundeckRepositoryArtifact()
        1 * controller.repositoryPluginService.removeOldPlugin(_)
        1 * controller.repositoryPluginService.syncInstalledArtifactsToPluginTarget()
        controller.installArtifact()

        then:
        response.json == ["msg":"Plugin Installed"]

    }

    void "uninstall artifact no repo specified and only 1 repo defined"() {
        given:
        controller.repositoryPluginService = Mock(RepositoryPluginService)
        def installedPluginId = PluginUtils.generateShaIdFromName("InstalledPlugin")
        controller.pluginApiService.installedPluginIds = [:]
        controller.pluginApiService.installedPluginIds[installedPluginId] = "1.0"

        when:
        params.artifactId = installedPluginId
        1 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE)]
        1 * client.getArtifact("private",installedPluginId, "1.0") >> new RundeckRepositoryArtifact()
        1 * controller.repositoryPluginService.uninstallArtifact(_)
        controller.uninstallArtifact()

        then:
        response.json == ["msg":"Plugin Uninstalled"]

    }

    void "regenreate manifest no repo specified and only 1 repo defined"() {
        given:
        controller.repositoryPluginService = Mock(RepositoryPluginService)

        when:
        1 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE)]
        1 * client.refreshRepositoryManifest(_)
        controller.regenerateManifest()

        then:
        response.json == ["msg":"Refreshed Repository private"]

    }

    List<ManifestSearchResult> testArtifactList(String repoName) {
        ManifestSearchResult one = new ManifestSearchResult()
        one.repositoryName = repoName
        one.results = [
                createTestManifestEntry("InstalledPlugin",[:]),
                createTestManifestEntry("Available Plugin",[:]),
                createTestManifestEntry("Script Plugin",[artifactType:ArtifactType.SCRIPT_PLUGIN,tags:["script","node step"]])
        ]
        return [one]
    }

    List<ManifestSearchResult> testSearch(String repoName) {
        ManifestSearchResult one = new ManifestSearchResult()
        one.repositoryName = repoName
        one.results = [
                createTestManifestEntry("Script Plugin",[artifactType:ArtifactType.SCRIPT_PLUGIN,tags:["script","node step"]])
        ]
        return [one]
    }

    ManifestEntry createTestManifestEntry(String pluginName, Map artifactProps) {
        Map props = [:]
        props.id = PluginUtils.generateShaIdFromName(pluginName)
        props.name = pluginName
        props.description = "Rundeck plugin"
        props.artifactType = ArtifactType.JAVA_PLUGIN
        props.author = "rundeck"
        props.currentVersion = "1.0"
        props.support = SupportLevel.RUNDECK
        props.tags = ["rundeck","orignal"]
        props.putAll(artifactProps)
        return new ManifestEntry(props)
    }

    class FakeFrameworkService {

        boolean authorized = true

        FakeFrameworkService() {}
        FakeFrameworkService(authorized) {
            this.authorized = authorized
        }

        AuthContext getAuthContextForSubject(def subject) {
            return [] as AuthContext
        }
        boolean authorizeApplicationResourceAny(def authCtx, def type, def action) { return authorized }
    }
    class FakePluginApiService {
        def installedPluginIds = []
        def listInstalledPluginIds() {
            return installedPluginIds
        }
    }
}
