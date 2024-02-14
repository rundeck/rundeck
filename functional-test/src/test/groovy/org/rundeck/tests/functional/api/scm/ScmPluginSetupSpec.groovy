package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.scm.GitLocalServerRepoCreator
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.ScmPluginsListResponse
import org.rundeck.util.api.scm.SetupGitIntegrationRequest
import org.rundeck.util.api.scm.SetupIntegrationResponse
import org.rundeck.util.container.BaseContainer

@APITest
class ScmPluginSetupSpec extends BaseContainer{

    static final String PROJECT_NAME = "ScmPluginSetupSpec"
    def setupSpec() {
        startEnvironment()
    }

    def "should mark corresponding validation errors with 'required' on missing properties"(){
        given:
        String integration = "export"
        String projectName = "${PROJECT_NAME}-P1"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        SetupGitIntegrationRequest requestBody = new SetupGitIntegrationRequest([config: scmSetupProps])

        when:
        SetupIntegrationResponse response = scmClient.callSetupIntegration(requestBody)

        then:
        !response.success
        response.message == "Some input values were not valid."
        scmSetupProps.each { propName, propValue ->
            if(!propValue || propValue == "")
                response.validationErrors[propName] == "required"
        }

        where:
        scmSetupProps << [
                [dir: "/dir/example", url:""],
                [dir: "", url:"/remote/repo/example"],
                [dir: "", url:""]
        ]
    }

    def "should return no errors on valid setup"(){
        given:
        String integration = "export"
        String projectName = "${PROJECT_NAME}-P2"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        SetupGitIntegrationRequest requestBody = SetupGitIntegrationRequest.defaultRequest()
        requestBody.config.dir = "/home/rundeck/projects/${projectName}/ScmExport"
        requestBody.config.url = "${GitLocalServerRepoCreator.REPO_TEMPLATE_PATH}"

        when:
        SetupIntegrationResponse response = scmClient.callSetupIntegration(requestBody)

        then:
        response.success
        response.message == "SCM Plugin Setup Complete"
        !response.validationErrors
    }

    def "plugin must be present in scm plugins list with enabled false after disabling plugin"(){
        given:
        String integration = "export"
        String projectName = "${PROJECT_NAME}-P3"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        SetupGitIntegrationRequest requestBody = SetupGitIntegrationRequest.defaultRequest()
        requestBody.config.dir = "/home/rundeck/projects/${projectName}/ScmExport"
        requestBody.config.url = "${GitLocalServerRepoCreator.REPO_TEMPLATE_PATH}"

        expect:
        verifyAll {
            scmClient.callSetupIntegration(requestBody).success
            ScmPluginsListResponse scmPlugins = scmClient.callGetPluginsList()
            scmPlugins.integration == integration
            scmPlugins.plugins.find { it.type == scmClient.pluginName && it.enabled }
        }
        when:
        SetupIntegrationResponse disablePluginResult = scmClient.callSetEnabledStatusForPlugin(false)

        then:
        verifyAll {
            disablePluginResult.success
            disablePluginResult.message == "Plugin disabled for SCM export: ${scmClient.pluginName}"
            ScmPluginsListResponse scmPlugins = scmClient.callGetPluginsList()
            scmPlugins.integration == integration
            scmPlugins.plugins.find { it.type == scmClient.pluginName && !it.enabled }
        }
    }

    def "respond with success false if disabling a non existing plugin"(){
        given:
        String integration = "export"
        String projectName = "${PROJECT_NAME}-P4"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        SetupGitIntegrationRequest requestBody = SetupGitIntegrationRequest.defaultRequest()
        requestBody.config.dir = "/home/rundeck/projects/${projectName}/ScmExport"
        requestBody.config.url = "${GitLocalServerRepoCreator.REPO_TEMPLATE_PATH}"

        expect:
        scmClient.callSetupIntegration(requestBody).success

        when:
        SetupIntegrationResponse disablePluginResult = scmClient.callSetEnabledStatusForPlugin(false,'wrong-plugin')

        then:
        verifyAll {
            disablePluginResult.success == false
            disablePluginResult.message == 'Plugin type wrong-plugin for export is not configured'
            ScmPluginsListResponse scmPlugins = scmClient.callGetPluginsList()
            scmPlugins.integration == integration
            scmPlugins.plugins.find {scmPlugin -> scmPlugin.type == scmClient.pluginName && scmPlugin.enabled }
        }
    }

    def "disable a plugin that wasn't configured results in response with success false"(){
        given:
        String integration = "export"
        String projectName = "${PROJECT_NAME}-P5"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        SetupGitIntegrationRequest requestBody = SetupGitIntegrationRequest.defaultRequest()
        requestBody.config.dir = "/home/rundeck/projects/${projectName}/ScmExport"
        requestBody.config.url = "${GitLocalServerRepoCreator.REPO_TEMPLATE_PATH}"

        when:
        SetupIntegrationResponse disablePluginResult = scmClient.callSetEnabledStatusForPlugin(false)

        then:
        !disablePluginResult.success
        disablePluginResult.message == "No export plugin configured"
    }

    def "plugin must be present in scm plugins list with enabled true after enabling plugin"(){
        given:
        String integration = "export"
        String projectName = "${PROJECT_NAME}-P6"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        SetupGitIntegrationRequest requestBody = SetupGitIntegrationRequest.defaultRequest()
        requestBody.config.dir = "/home/rundeck/projects/${projectName}/ScmExport"
        requestBody.config.url = "${GitLocalServerRepoCreator.REPO_TEMPLATE_PATH}"

        expect:
        verifyAll {
            scmClient.callSetupIntegration(requestBody).success
            scmClient.callSetEnabledStatusForPlugin(false).success
        }

        when:
        SetupIntegrationResponse enablePluginResult = scmClient.callSetEnabledStatusForPlugin(true)

        then:
        verifyAll {
            enablePluginResult.success
            enablePluginResult.message == "Plugin enabled for SCM export: ${scmClient.pluginName}"
            ScmPluginsListResponse scmPlugins = scmClient.callGetPluginsList()
            scmPlugins.integration == integration
            scmPlugins.plugins.find { it.type == scmClient.pluginName && it.enabled }
        }
    }
}
