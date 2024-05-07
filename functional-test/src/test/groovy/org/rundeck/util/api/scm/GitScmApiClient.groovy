package org.rundeck.util.api.scm

import okhttp3.Response
import org.rundeck.util.api.scm.httpbody.IntegrationStatusResponse
import org.rundeck.util.api.responses.common.RundeckResponse
import org.rundeck.util.api.scm.httpbody.ScmActionInputFieldsResponse
import org.rundeck.util.api.scm.httpbody.ScmActionPerformRequest
import org.rundeck.util.api.scm.httpbody.ScmJobStatusResponse
import org.rundeck.util.api.scm.httpbody.ScmPluginInputFieldsResponse
import org.rundeck.util.api.scm.httpbody.ScmPluginsListResponse
import org.rundeck.util.api.scm.httpbody.ScmProjectConfigResponse
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.SetupIntegrationResponse
import org.rundeck.util.common.scm.ScmActionId
import org.rundeck.util.common.scm.ScmIntegration
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

    GitScmApiClient forIntegration(ScmIntegration integration){
        this.integration = integration.name
        this.pluginName = "git-${integration.name}"
        return this
    }

    GitScmApiClient forProject(String project){
        this.project = project
        return this
    }

    RundeckResponse<SetupIntegrationResponse> callSetupIntegration(GitExportSetupRequest requestBody){
        Response resp = client.doPost("/project/${project}/scm/${integration}/plugin/${pluginName}/setup", requestBody)

        return new RundeckResponse(resp, SetupIntegrationResponse)
    }

    RundeckResponse<IntegrationStatusResponse> callGetIntegrationStatus() {
        Response resp = client.doGet("/project/${project}/scm/${integration}/status")

        return new RundeckResponse(resp, IntegrationStatusResponse)
    }

    RundeckResponse<ScmActionInputFieldsResponse> callGetFieldsForAction(ScmActionId actionId) {
        Response resp = client.doGet("/project/${project}/scm/${integration}/action/${actionId.name}/input")

        return new RundeckResponse(resp, ScmActionInputFieldsResponse)
    }

    RundeckResponse<SetupIntegrationResponse> callPerformAction(String actionId, ScmActionPerformRequest requestBody ) {
        Response resp = client.doPost("/project/${project}/scm/${integration}/action/${actionId}", requestBody)

        return new RundeckResponse(resp, SetupIntegrationResponse)
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

    RundeckResponse<SetupIntegrationResponse> callPerformJobAction(String actionId, ScmActionPerformRequest requestBody, String jobId ) {
        Response resp = client.doPost("/job/${jobId}/scm/${integration}/action/${actionId}", requestBody)

        return new RundeckResponse(resp, SetupIntegrationResponse)
    }

    RundeckResponse<ScmJobStatusResponse> callGetJobStatus(String jobId) {
        Response resp = client.doGet("/job/${jobId}/scm/${integration}/status")

        return new RundeckResponse(resp, ScmJobStatusResponse)
    }


    String getPluginName() {
        return pluginName
    }
}
