package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.scm.GitLocalServerRepoCreator
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.httpbody.IntegrationStatusResponse
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.container.BaseContainer

@APITest
class ScmPluginStatusSpec extends BaseContainer {

    static final String PROJECT_NAME = "ScmPluginStatusSpec-project"
    def setupSpec() {
        startEnvironment()
        setupProject(PROJECT_NAME)
    }

    def "project scm export status must be clean after setup on empty project"(){
        given:
        String integration = "export"
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(PROJECT_NAME)

        GitExportSetupRequest requestBody = GitExportSetupRequest.defaultRequest()
        requestBody.config.dir = "/home/rundeck/projects/${PROJECT_NAME}/ScmExport"
        requestBody.config.url = "${GitLocalServerRepoCreator.REPO_TEMPLATE_PATH}"

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
