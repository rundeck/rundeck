package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.api.JobUtils
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.ScmActionPerformRequest
import org.rundeck.util.api.scm.httpbody.ScmJobStatusResponse
import org.rundeck.util.api.scm.httpbody.SetupIntegrationResponse
import org.rundeck.util.container.BaseContainer

@APITest
@ExcludePro
class ScmPluginJobDiffSpec extends BaseContainer {

    static final String PROJECT_NAME = "ScmPluginJobActionsInput-project"
    final String EXPORT_INTEGRATION = "export"
    final String DUMMY_JOB_ID = "383d0599-3ea3-4fa6-ac3a-75a53d6b0000"
    final String JOB_XML_NAME = "job-template-common.xml"
    static final GiteaApiRemoteRepo remoteRepo = new GiteaApiRemoteRepo('repoExample4')

    def setupSpec() {
        remoteRepo.setupRepo()
    }

    def "test_check_diff_after_actions"() {
        given:
        // Initial setup of the project and action parameters
        setupProject(PROJECT_NAME)
        def initialArgs = [
                "job-name": "job-test",
                "job-description-name": "description-test",
                "args": "echo hello there",
                "2-args": "echo hello there 2",
                "uuid": DUMMY_JOB_ID
        ]
        JobUtils.jobImportFile(PROJECT_NAME, JobUtils.updateJobFileToImport(JOB_XML_NAME, PROJECT_NAME, initialArgs) as String, client)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(EXPORT_INTEGRATION).forProject(PROJECT_NAME)
        scmClient.callSetupIntegration(GitExportSetupRequest.defaultRequest().forProject(PROJECT_NAME).withRepo(remoteRepo))
        ScmJobStatusResponse initialStatus = scmClient.callGetJobStatus(DUMMY_JOB_ID).response
        ScmActionPerformRequest actionRequest = new ScmActionPerformRequest([
                input: [message: "Commit msg example"],
                jobs: [DUMMY_JOB_ID]
        ])
        def actionId = 'job-commit'

        when:
        // Perform the action
        SetupIntegrationResponse performAction = scmClient.callPerformJobAction(actionId, actionRequest, DUMMY_JOB_ID).response

        then:
        // Verify state before the action
        initialStatus.actions.size() == 1
        initialStatus.commit == null
        initialStatus.id == DUMMY_JOB_ID
        initialStatus.integration == EXPORT_INTEGRATION
        initialStatus.message == "Created"
        initialStatus.project == PROJECT_NAME
        initialStatus.synchState == "CREATE_NEEDED"
        // Verify action response
        performAction.message == "SCM export Action was Successful: ${actionId}"
        performAction.success == true
        // Verify state after the action
        ScmJobStatusResponse updatedStatus = scmClient.callGetJobStatus(DUMMY_JOB_ID).response
        updatedStatus.actions.size() == 0
        updatedStatus.commit.size() == 5
        updatedStatus.id == DUMMY_JOB_ID
        updatedStatus.integration == EXPORT_INTEGRATION
        updatedStatus.message == "No Change"
        updatedStatus.project == PROJECT_NAME
        updatedStatus.synchState == "CLEAN"

        // Update job arguments and verify updated state
        and:
        def updatedArgs = [
                "job-name": "job-test-updated",
                "job-description-name": "description-test-updated",
                "args": "echo hello there updated",
                "2-args": "echo hello there 2 updated",
                "uuid": DUMMY_JOB_ID
        ]
        JobUtils.jobImportFile(PROJECT_NAME, JobUtils.updateJobFileToImport(JOB_XML_NAME, PROJECT_NAME, updatedArgs) as String, client, JobUtils.DUPE_OPTION_UPDATE)
        def exportNeededStatus = scmClient.callGetJobStatus(DUMMY_JOB_ID).response
        exportNeededStatus.actions.size() == 1
        exportNeededStatus.commit.size() == 5
        exportNeededStatus.id == DUMMY_JOB_ID
        exportNeededStatus.integration == EXPORT_INTEGRATION
        exportNeededStatus.message == "Modified"
        exportNeededStatus.project == PROJECT_NAME
        exportNeededStatus.synchState == "EXPORT_NEEDED"

        // Perform the action again and verify final state
        and:
        def finalAction = scmClient.callPerformJobAction(actionId, actionRequest, DUMMY_JOB_ID).response
        finalAction.message == "SCM export Action was Successful: ${actionId}"
        finalAction.success == true
        def finalStatus = scmClient.callGetJobStatus(DUMMY_JOB_ID).response
        finalStatus.actions.size() == 0
        finalStatus.commit.size() == 5
        finalStatus.id == DUMMY_JOB_ID
        finalStatus.integration == EXPORT_INTEGRATION
        finalStatus.message == "No Change"
        finalStatus.project == PROJECT_NAME
        finalStatus.synchState == "CLEAN"
    }


}
