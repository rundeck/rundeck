package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.RundeckResponse
import org.rundeck.util.api.scm.httpbody.ScmPluginsListResponse
import org.rundeck.util.api.scm.httpbody.ScmProjectConfigResponse
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.SetupIntegrationResponse
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

        GitExportSetupRequest requestBody = new GitExportSetupRequest([config: scmSetupProps])

        when:
        SetupIntegrationResponse response = scmClient.callSetupIntegration(requestBody).response

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
        GitExportSetupRequest requestBody = GitExportSetupRequest.defaultRequest(projectName)

        when:
        SetupIntegrationResponse response = scmClient.callSetupIntegration(requestBody).response

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

        GitExportSetupRequest requestBody = GitExportSetupRequest.defaultRequest(projectName)

        expect:
        verifyAll {
            scmClient.callSetupIntegration(requestBody).response.success
        }
        when:
        SetupIntegrationResponse disablePluginResult = scmClient.callSetEnabledStatusForPlugin(false).response

        then:
        verifyAll {
            disablePluginResult.success
            disablePluginResult.message == "Plugin disabled for SCM export: ${scmClient.pluginName}"
            ScmPluginsListResponse scmPlugins = scmClient.callGetPluginsList().response
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

        GitExportSetupRequest requestBody = GitExportSetupRequest.defaultRequest(projectName)

        expect:
        scmClient.callSetupIntegration(requestBody).response.success

        when:
        SetupIntegrationResponse disablePluginResult = scmClient.callSetEnabledStatusForPlugin(false,'wrong-plugin').response

        then:
        verifyAll {
            disablePluginResult.success == false
            disablePluginResult.message == 'Plugin type wrong-plugin for export is not configured'
            ScmPluginsListResponse scmPlugins = scmClient.callGetPluginsList().response
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

        GitExportSetupRequest requestBody = GitExportSetupRequest.defaultRequest(projectName)

        when:
        SetupIntegrationResponse disablePluginResult = scmClient.callSetEnabledStatusForPlugin(false).response

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

        GitExportSetupRequest requestBody = GitExportSetupRequest.defaultRequest(projectName)

        expect:
        verifyAll {
            scmClient.callSetupIntegration(requestBody).response.success
            scmClient.callSetEnabledStatusForPlugin(false).response.success
        }

        when:
        SetupIntegrationResponse enablePluginResult = scmClient.callSetEnabledStatusForPlugin(true).response

        then:
        verifyAll {
            enablePluginResult.success
            enablePluginResult.message == "Plugin enabled for SCM export: ${scmClient.pluginName}"
            ScmPluginsListResponse scmPlugins = scmClient.callGetPluginsList().response
            scmPlugins.integration == integration
            scmPlugins.plugins.find { it.type == scmClient.pluginName && it.enabled }
        }
    }

    def "should return the current  project scm configuration"(){
        given:
        String integration = "export"
        String projectName = "${PROJECT_NAME}-P7"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        GitExportSetupRequest setupScmRequest = GitExportSetupRequest.defaultRequest(projectName)

        expect:
        scmClient.callSetupIntegration(setupScmRequest).response.success

        when:
        ScmProjectConfigResponse configResponse = scmClient.callGetProjectScmConfig().response

        then:
        verifyAll {
            configResponse.integration == integration
            configResponse.type == scmClient.pluginName
            configResponse.enabled == true

            Map setupScmRequestMap = setupScmRequest.config.toMap()
            configResponse.config.size() == setupScmRequestMap.size()
            setupScmRequestMap.each { config ->
                configResponse.config[config.key] == config.value
            }
        }
    }

    def "list plugins on new empty project with no scm plugin setup"(){
        given:
        String projectName = "${PROJECT_NAME}-P8"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        when:
        ScmPluginsListResponse scmPlugins = scmClient.callGetPluginsList().response

        then:
        verifyAll {
            scmPlugins.plugins.size() == 1
            scmPlugins.plugins.first().configured == false
            scmPlugins.plugins.first().enabled == false
            scmPlugins.integration == integration
            scmPlugins.plugins.first().type == scmClient.pluginName
            scmPlugins.plugins.first().title == expectedTitle
            scmPlugins.plugins.first().description == expectedDescription
        }

        where:
        integration | expectedTitle | expectedDescription
        'export'    | 'Git Export'  | 'Export Jobs to a Git Repository'
        'import'    | 'Git Import'  | 'Import Jobs from a Git Repository'
    }

    def "list plugins for non existing integration"() {
        given:
        String integration = 'invalid'
        String projectName = "${PROJECT_NAME}-P9"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        when:
        RundeckResponse.ApiError scmPlugins = scmClient.callGetPluginsList().error

        then:
        verifyAll {
            scmPlugins.errorCode == 'api.error.invalid.request'
            scmPlugins.message == 'Invalid API Request: the value "invalid" for parameter "integration" was invalid. It must be in the list: [export, import]'
        }
    }
}
