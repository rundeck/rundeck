package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo
import org.rundeck.util.api.scm.httpbody.IntegrationStatusResponse
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.SetupIntegrationResponse
import org.rundeck.util.common.scm.ScmIntegration
import org.rundeck.util.container.BaseContainer

@APITest
@ExcludePro
class ScmPluginStatusSpec extends BaseContainer {
    static final String PROJECT_NAME = UUID.randomUUID().toString()
    static final GiteaApiRemoteRepo remoteRepo = new GiteaApiRemoteRepo(PROJECT_NAME)

    def setupSpec() {
        setupProject(PROJECT_NAME)
        remoteRepo.setupRepo()
    }

    def "project scm export status must be clean after setup on empty project"(){
        given:
        ScmIntegration integration = ScmIntegration.EXPORT
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(PROJECT_NAME)

        GitExportSetupRequest requestBody = GitExportSetupRequest.defaultRequest().forProject(PROJECT_NAME).withRepo(remoteRepo)

        expect:
        /**
         * A race condition is suspected between the remoteRepo.setupRepo() call in the Spec setup and
         * the integration setup call below. It materializes as a 400 response with the error:
         * message=Failed cloning the repository from http://rundeckgitea@gitea:3000/rundeckgitea/<...>: Invalid remote: origin
         * Additional error checking was added to the setupRepo() to enable diagnostic.
         */
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
