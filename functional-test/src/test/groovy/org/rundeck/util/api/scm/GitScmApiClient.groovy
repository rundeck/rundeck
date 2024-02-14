package org.rundeck.util.api.scm

import okhttp3.Response
import org.rundeck.util.container.ClientProvider
import org.rundeck.util.container.RdClient

class GitScmApiClient {
    private final RdClient client
    private String integration
    private String pluginName
    private String project

    GitScmApiClient(ClientProvider clientProvider){
        this.client = clientProvider.client
    }

    GitScmApiClient forIntegration(String integration){
        this.integration = integration
        this.pluginName = "git-${integration}"
        return this
    }

    GitScmApiClient forProject(String project){
        this.project = project
        return this
    }

    RundeckResponse<SetupIntegrationResponse> callSetupIntegration(SetupGitIntegrationRequest requestBody){
        Response resp = client.doPost("/project/${project}/scm/${integration}/plugin/${pluginName}/setup", requestBody)

        return new RundeckResponse(resp, SetupIntegrationResponse)
    }

    RundeckResponse<IntegrationStatusResponse> callGetIntegrationStatus() {
        Response resp = client.doGet("/project/${project}/scm/${integration}/status")

        return new RundeckResponse(resp, IntegrationStatusResponse)
    }

    RundeckResponse<ScmPluginsListResponse> callGetPluginsList(){
        Response resp = client.doGet("/project/${project}/scm/${integration}/plugins")

        return new RundeckResponse(resp, ScmPluginsListResponse)
    }

    RundeckResponse<SetupIntegrationResponse> callSetEnabledStatusForPlugin(boolean enablePlugin, String pluginName = this.pluginName){
        Response resp = client.doPost("/project/${project}/scm/${integration}/plugin/${pluginName}/${enablePlugin? 'enable' : 'disable'}")

        return new RundeckResponse(resp, SetupIntegrationResponse)
    }

    RundeckResponse<ScmPluginInputFieldsResponse> callGetInputFieldsForPlugin(){
        Response resp = client.doGet("/project/${project}/scm/${integration}/plugin/${pluginName}/input")

        return new RundeckResponse(resp, ScmPluginInputFieldsResponse)
    }

    RundeckResponse<ScmProjectConfigResponse> callGetProjectScmConfig(){
        Response resp = client.doGet("/project/${project}/scm/${integration}/config")

        return new RundeckResponse(resp, ScmProjectConfigResponse)
    }

    String getPluginName() {
        return pluginName
    }
}
