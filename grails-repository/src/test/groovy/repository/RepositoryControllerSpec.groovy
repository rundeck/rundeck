package repository

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
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
import org.rundeck.core.auth.AuthConstants
import spock.lang.Specification

class RepositoryControllerSpec extends Specification implements ControllerUnitTest<RepositoryController> {

    def client

    def setup() {
        client = Mock(RepositoryClient)
        controller.repoClient = client
        controller.frameworkService = new FakeFrameworkService()
        controller.pluginApiService = new FakePluginApiService()
    }

    private void setupAuthSystemAdmin(Boolean authorized=true) {
        controller.rundeckAuthContextProcessor = Mock(AuthContextProcessor) {
            1 * authorizeApplicationResourceAny(
                _,
                AuthConstants.RESOURCE_TYPE_SYSTEM,
                [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN]
            ) >> authorized
            getAuthContextForSubject(_) >> Mock(UserAndRolesAuthContext)
        }
    }
    private void setupAuthPluginAction(String action, Boolean authorized=true) {
        controller.rundeckAuthContextProcessor = Mock(AuthContextProcessor) {
            1 * authorizeApplicationResourceAny(
                _,
                AuthConstants.RESOURCE_TYPE_PLUGIN,
                [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN, action]
            ) >> authorized
            getAuthContextForSubject(_) >> Mock(UserAndRolesAuthContext)
        }
    }

    def cleanup() {
    }

    void "list Repositories"() {
        given:
        setupAuthSystemAdmin()
        when:
        1 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE),
                                              new RepositoryDefinition(repositoryName: "official",owner:RepositoryOwner.RUNDECK,enabled:false)]
        controller.listRepositories()

        then:
        response.json.size() == 2
        response.json == [[name:"private",type:"PRIVATE",enabled:true],[name:"official",type:"RUNDECK",enabled:false]]

    }
    void "list Repositories unauthorized"() {
        given:
            setupAuthSystemAdmin(false)
        when:
        controller.listRepositories()

        then:
            0 * client.listRepositories()
            response.status==400
            response.json == [error:'You are not authorized to perform this action']
    }

    void "list artifacts no repo specified"() {
        given:
        String pluginId = PluginUtils.generateShaIdFromName("InstalledPlugin")
        controller.pluginApiService.installedPluginIds = [:]
        controller.pluginApiService.installedPluginIds[pluginId] = "1.0"
        setupAuthPluginAction(AuthConstants.ACTION_READ)

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
            setupAuthPluginAction(AuthConstants.ACTION_READ, false)

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
        setupAuthPluginAction(AuthConstants.ACTION_READ)

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
    void "search artifacts unauthorized"() {
        given:
        controller.pluginApiService.installedPluginIds = [:]

        setupAuthPluginAction(AuthConstants.ACTION_READ, false)

        when:
        params.searchTerm = "artifactType: script-plugin"
        controller.searchArtifacts()

        then:

            0 * client.searchManifests(_)

    }


    void "list installed artifacts"() {
        given:
        def installedPluginId = PluginUtils.generateShaIdFromName("InstalledPlugin")
        controller.pluginApiService.installedPluginIds = [:]
        controller.pluginApiService.installedPluginIds[installedPluginId] = "1.0"
        setupAuthPluginAction(AuthConstants.ACTION_READ)

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
        given:
        setupAuthPluginAction(AuthConstants.ACTION_INSTALL)
        when:
        ResponseBatch successBatch = new ResponseBatch()
        successBatch.addMessage(ResponseMessage.success())
        1 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE)]
        1 * client.uploadArtifact(_,_) >> successBatch
        controller.featureService = Mock(FeatureService){
            featurePresent(_) >> false
        }
        controller.uploadArtifact()

        then:
        response.json == ["msg":"Upload succeeded"]

    }

    void "upload artifact, plugin security enabled"() {
        when:
        controller.featureService = Mock(FeatureService){
            featurePresent(_) >> true
        }
        controller.uploadArtifact()

        then:
        response.json == ["error": "Unable to upload plugins, see find plugins page for all available plugins"]

    }
    void "upload artifact unauthorized"() {
        given:

            controller.featureService = Mock(FeatureService)
            setupAuthPluginAction(AuthConstants.ACTION_INSTALL, false)
        when:
            controller.uploadArtifact()

        then:
            response.status==400
            response.json == ["error": "You are not authorized to perform this action"]
            0 * client.uploadArtifact(*_)
    }

    void "install artifact no repo specified and only 1 repo defined"() {
        given:
        controller.repositoryPluginService = Mock(RepositoryPluginService)
        setupAuthPluginAction(AuthConstants.ACTION_INSTALL)

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

    void "install artifact unauthorized"() {
        given:
        controller.repositoryPluginService = Mock(RepositoryPluginService)
            setupAuthPluginAction(AuthConstants.ACTION_INSTALL, false)

        when:
            controller.installArtifact()

        then:
            response.status==400
            response.json == ["error":"You are not authorized to perform this action"]

            0 * client.listRepositories()
            0 * client.installArtifact(_,_,_)
            0 * client.getArtifact(_,_, null)
            0 * controller.repositoryPluginService.removeOldPlugin(_)
            0 * controller.repositoryPluginService.syncInstalledArtifactsToPluginTarget()

    }

    void "uninstall artifact no repo specified and only 1 repo defined"() {
        given:
        controller.repositoryPluginService = Mock(RepositoryPluginService)
        def installedPluginId = PluginUtils.generateShaIdFromName("InstalledPlugin")
        controller.pluginApiService.installedPluginIds = [:]
        controller.pluginApiService.installedPluginIds[installedPluginId] = "1.0"
        setupAuthPluginAction(AuthConstants.ACTION_UNINSTALL)

        when:
        params.artifactId = installedPluginId
        1 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE)]
        1 * client.getArtifact("private",installedPluginId, "1.0") >> new RundeckRepositoryArtifact()
        1 * controller.repositoryPluginService.uninstallArtifact(_)
        controller.uninstallArtifact()

        then:
        response.json == ["msg":"Plugin Uninstalled"]

    }

    void "uninstall artifact unauthorized"() {
        given:
        controller.repositoryPluginService = Mock(RepositoryPluginService)
        def installedPluginId = PluginUtils.generateShaIdFromName("InstalledPlugin")
        controller.pluginApiService.installedPluginIds = [:]
        controller.pluginApiService.installedPluginIds[installedPluginId] = "1.0"

            setupAuthPluginAction(AuthConstants.ACTION_UNINSTALL, false)

        when:
        params.artifactId = installedPluginId
        controller.uninstallArtifact()

        then:
            response.status==400
            response.json == ["error":"You are not authorized to perform this action"]
            0 * client.listRepositories()
            0 * client.getArtifact("private",installedPluginId, "1.0")
            0 * controller.repositoryPluginService.uninstallArtifact(_)

    }

    void "uninstall artifact that was manually installed"() {
        given:
        ServiceProviderLoader mockPluginManager = Mock(ServiceProviderLoader)
        IFramework mockRundeckFramework = Mock(IFramework) {
            getPluginManager() >> mockPluginManager
        }
        controller.repositoryPluginService = Mock(RepositoryPluginService)
        FakeFrameworkService fwkSvc = new FakeFrameworkService()
        fwkSvc.setRundeckFramework(mockRundeckFramework)
        controller.frameworkService = fwkSvc
        String pluginName = "ManuallyInstalledPlugin"
        def installedPluginId = PluginUtils.generateShaIdFromName(pluginName)
        controller.pluginApiService.installedPluginIds = [:]
        controller.pluginApiService.installedPluginIds[installedPluginId] = "1.0"
        File tmp = File.createTempFile("manual","plugin")
        tmp.deleteOnExit()
        def mockPluginMeta = Mock(PluginMetadata) {
            getFile() >> { tmp  }
        }
        setupAuthPluginAction(AuthConstants.ACTION_UNINSTALL)

        when:
        params.artifactId = installedPluginId
        params.service = "LogFilter"
        params.name = pluginName
        controller.uninstallArtifact()

        then:
        1 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE)]
        1 * client.getArtifact("private",installedPluginId, "1.0") >> null
        0 * controller.repositoryPluginService.uninstallArtifact(_)
        1 * mockPluginManager.getPluginMetadata("LogFilter",pluginName) >> { mockPluginMeta }
        1 * controller.repositoryPluginService.removeOldPlugin(tmp)
        response.json == ["msg":"Plugin Uninstalled"]

    }

    void "regenreate manifest no repo specified and only 1 repo defined"() {
        given:
        controller.repositoryPluginService = Mock(RepositoryPluginService)
        setupAuthSystemAdmin()

        when:
        1 * client.listRepositories() >> [new RepositoryDefinition(repositoryName: "private", owner: RepositoryOwner.PRIVATE)]
        1 * client.refreshRepositoryManifest(_)
        controller.regenerateManifest()

        then:
        response.json == ["msg":"Refreshed Repository private"]

    }

    void "regenreate manifest unauthorized"() {
        given:
            controller.repositoryPluginService = Mock(RepositoryPluginService)
            setupAuthSystemAdmin(false)
        when:
            controller.regenerateManifest()

        then:
            response.status==400
            response.json == ["error":"You are not authorized to perform this action"]

            0 * client.listRepositories()
            0 * client.refreshRepositoryManifest(_)

    }


    void "convert to number for version check"() {
        when:
        String rver = ver?.replaceAll(~/[^\d]/,"")
        long result = controller.convertToNumber(rver,"installed")

        then:
        result == expected

        where:
        ver                 | expected
        "1.2.0-SNAPSHOT"    | 120
        "2.4.3"             | 243
        "SNAPSHOT"          | 0
        "1.2.3rc-2"         | 1232
        null                | 0

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

        IFramework rundeckFramework
        IFramework getRundeckFramework() { return rundeckFramework }
    }
    class FakePluginApiService {
        def installedPluginIds = []
        def listInstalledPluginIds() {
            return installedPluginIds
        }
    }
}
