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
    SetupIntegrationResponse setupIntegration(SetupGitIntegrationRequest requestBody){
        Response resp = client.doPost("/project/${project}/scm/${integration}/plugin/${pluginName}/setup", requestBody)

        return SetupIntegrationResponse.extractFromResponse(resp)
    }

    IntegrationStatusResponse getIntegrationStatus() {
        Response resp = client.doGet("/project/${project}/scm/${integration}/status")

        return IntegrationStatusResponse.extractFromResponse(resp)
    }
}
