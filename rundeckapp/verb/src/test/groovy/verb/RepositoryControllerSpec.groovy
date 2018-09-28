package verb

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.artifact.SupportLevel
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.client.VerbClient
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.client.repository.RundeckRepositoryManager
import com.rundeck.verb.manifest.ManifestEntry
import com.rundeck.verb.manifest.search.ManifestSearchResult
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class RepositoryControllerSpec extends Specification implements ControllerUnitTest<RepositoryController> {

    def verbClient

    def setup() {
        verbClient = Mock(VerbClient)
        controller.verbClient = verbClient
        controller.frameworkService = new FakeFrameworkService()
        controller.pluginApiService = new FakePluginApiService()
    }

    def cleanup() {
    }

    void "list Repositories"() {
        when:
        1 * verbClient.listRepositories() >> ["private","official"]
        controller.listRepositories()

        then:
        response.json.size() == 2
        response.json == ["private","official"]

    }

    void "list artifacts no repo specified and only 1 repo defined"() {
        given:
        controller.pluginApiService.installedPluginIds = [PluginUtils.generateShaIdFromName("InstalledPlugin")]

        when:
        1 * verbClient.listRepositories() >> ["private"]
        1 * verbClient.listArtifacts(_,_,_) >> testArtifactList("private")
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

    void "search artifacts no repo specified and only 1 repo defined"() {

        when:
        1 * verbClient.listRepositories() >> ["private"]
        1 * verbClient.searchManifests(_) >> testSearch("private")
        params.searchTerm = "artifactType: script-plugin"
        controller.searchArtifacts()

        then:
        response.json.artifacts.size() == 1
        response.json.artifacts[0].repositoryName == "private"
        response.json.artifacts[0].results.size() == 1
        response.json.artifacts[0].results[0].name == "Script Plugin"
        response.json.artifacts[0].results[0].installed == false

    }

    void "list installed artifacts no repo specified and only 1 repo defined"() {
        given:
        def installedPluginId = PluginUtils.generateShaIdFromName("InstalledPlugin")
        controller.pluginApiService.installedPluginIds = [installedPluginId]

        when:
        1 * verbClient.listRepositories() >> ["private"]
        1 * verbClient.listArtifacts(_,_) >> testArtifactList("private")
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
        1 * verbClient.listRepositories() >> ["private"]
        1 * verbClient.uploadArtifact(_,_) >> successBatch
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
        1 * verbClient.listRepositories() >> ["private"]
        1 * verbClient.installArtifact(_,_,_) >> successBatch
        1 * verbClient.getArtifact(_,_, null) >> new RundeckVerbArtifact()
        1 * controller.repositoryPluginService.removeOldPlugin(_)
        1 * controller.repositoryPluginService.syncInstalledArtifactsToPluginTarget()
        controller.installArtifact()

        then:
        response.json == ["msg":"Plugin Installed"]

    }

    void "uninstall artifact no repo specified and only 1 repo defined"() {
        given:
        controller.repositoryPluginService = Mock(RepositoryPluginService)

        when:
        1 * verbClient.listRepositories() >> ["private"]
        1 * verbClient.getArtifact(_,_, null) >> new RundeckVerbArtifact()
        1 * controller.repositoryPluginService.uninstallArtifact(_)
        controller.uninstallArtifact()

        then:
        response.json == ["msg":"Plugin Uninstalled"]

    }

    void "regenreate manifest no repo specified and only 1 repo defined"() {
        given:
        controller.repositoryPluginService = Mock(RepositoryPluginService)

        when:
        1 * verbClient.listRepositories() >> ["private"]
        1 * verbClient.refreshRepositoryManifest(_)
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
        AuthContext getAuthContextForSubject(def subject) {
            return [] as AuthContext
        }
        boolean authorizeApplicationResourceType(def authCtx, def type, def action) { return true }
    }
    class FakePluginApiService {
        def installedPluginIds = []
        def listInstalledPluginIds() {
            return installedPluginIds
        }
    }
}
