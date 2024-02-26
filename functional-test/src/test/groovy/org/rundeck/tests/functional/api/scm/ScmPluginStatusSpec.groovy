package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo
import org.rundeck.util.api.scm.httpbody.IntegrationStatusResponse
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.container.BaseContainer

@APITest
@ExcludePro
class ScmPluginStatusSpec extends BaseContainer {
    static final GiteaApiRemoteRepo remoteRepo = new GiteaApiRemoteRepo('repoExample')
    static final String PROJECT_NAME = "ScmPluginStatusSpec-project"

    def setupSpec() {
        setupProject(PROJECT_NAME)
        remoteRepo.setupRepo()
    }

    def "project scm export status must be clean after setup on empty project"(){
        given:
        String integration = "export"
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(PROJECT_NAME)

        GitExportSetupRequest requestBody = GitExportSetupRequest.defaultRequest().forProject(PROJECT_NAME).withRepo(remoteRepo)

        expect:
        scmClient.callSetupIntegration(requestBody).response.success

        when:
        IntegrationStatusResponse resp = scmClient.callGetIntegrationStatus().response

        then:
        verifyAll {
            resp != null
            resp.project == PROJECT_NAME
            resp.integration == integration
            resp.synchState == "CLEAN"
        }
    }
}
