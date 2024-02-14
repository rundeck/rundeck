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

    SetupIntegrationResponse callSetupIntegration(SetupGitIntegrationRequest requestBody){
        Response resp = client.doPost("/project/${project}/scm/${integration}/plugin/${pluginName}/setup", requestBody)

        return SetupIntegrationResponse.extractFromResponse(resp)
    }

    IntegrationStatusResponse callGetIntegrationStatus() {
        Response resp = client.doGet("/project/${project}/scm/${integration}/status")

        return IntegrationStatusResponse.extractFromResponse(resp)
    }

    ScmPluginsListResponse callGetPluginsList(){
        Response resp = client.doGet("/project/${project}/scm/${integration}/plugins")

        return ScmPluginsListResponse.extractFromResponse(resp)
    }

    SetupIntegrationResponse callSetEnabledStatusForPlugin(boolean enablePlugin, String pluginName = this.pluginName){
        Response resp = client.doPost("/project/${project}/scm/${integration}/plugin/${pluginName}/${enablePlugin? 'enable' : 'disable'}")

        return SetupIntegrationResponse.extractFromResponse(resp)
    }

    ScmPluginInputFieldsResponse callGetInputFieldsForPlugin(){
        Response resp = client.doGet("/project/${project}/scm/${integration}/plugin/${pluginName}/input")

        return ScmPluginInputFieldsResponse.extractFromResponse(resp)
    }

    String getPluginName() {
        return pluginName
    }
}
