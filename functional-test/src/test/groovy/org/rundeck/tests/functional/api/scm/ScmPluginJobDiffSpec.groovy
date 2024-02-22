package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.JobUtils
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.ScmActionPerformRequest
import org.rundeck.util.api.scm.httpbody.ScmJobStatusResponse
import org.rundeck.util.api.scm.httpbody.SetupIntegrationResponse
import org.rundeck.util.container.BaseContainer

@APITest
class ScmPluginJobDiffSpec extends BaseContainer {

    static final String PROJECT_NAME = "ScmPluginJobActionsInput-project"
    final String EXPORT_INTEGRATION = "export"
    final String DUMMY_JOB_ID = "383d0599-3ea3-4fa6-ac3a-75a53d6b0000"
    final String JOB_XML_NAME = "job-template-common.xml"
    static final GiteaApiRemoteRepo remoteRepo = new GiteaApiRemoteRepo('repoExample4')

    def setupSpec() {
        remoteRepo.setupRepo()
    }

    def "test_job_action_perform"() {
        given:
        setupProject(PROJECT_NAME)
        def args = [
                "job-name": "job-test",
                "job-description-name": "description-test",
                "args": "echo hello there",
                "2-args": "echo hello there 2",
                "uuid": DUMMY_JOB_ID
        ]
        JobUtils.jobImportFile(PROJECT_NAME,JobUtils.updateJobFileToImport(JOB_XML_NAME,PROJECT_NAME,args) as String,client)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(EXPORT_INTEGRATION).forProject(PROJECT_NAME)
        scmClient.callSetupIntegration(GitExportSetupRequest.defaultRequest().forProject(PROJECT_NAME).withRepo(remoteRepo))
        ScmJobStatusResponse firstStatus = scmClient.callGetJobStatus(DUMMY_JOB_ID).response
        ScmActionPerformRequest actionRequest = new ScmActionPerformRequest([
                input: [message: "Commit msg example"],
                jobs : [DUMMY_JOB_ID]
        ])
        def actionId = 'job-commit'

        when:
        SetupIntegrationResponse performAction = scmClient.callPerformJobAction(actionId, actionRequest, DUMMY_JOB_ID).response

        then:
        firstStatus.actions.size() == 1
        firstStatus.commit == null
        firstStatus.id == DUMMY_JOB_ID
        firstStatus.integration == EXPORT_INTEGRATION
        firstStatus.message == "Created"
        firstStatus.project == PROJECT_NAME
        firstStatus.synchState == "CREATE_NEEDED"
        performAction.message == "SCM export Action was Successful: ${actionId}"
        performAction.success == true
        ScmJobStatusResponse updatedStatus = scmClient.callGetJobStatus(DUMMY_JOB_ID).response
        updatedStatus.actions.size() == 0
        updatedStatus.commit.size()== 5
        updatedStatus.id == DUMMY_JOB_ID
        updatedStatus.integration == EXPORT_INTEGRATION
        updatedStatus.message == "No Change"
        updatedStatus.project == PROJECT_NAME
        updatedStatus.synchState == "CLEAN"
        def newArgs = [
                "job-name": "job-test-updated",
                "job-description-name": "description-test-updated",
                "args": "echo hello there updated",
                "2-args": "echo hello there 2 updated",
                "uuid": DUMMY_JOB_ID
        ]
        JobUtils.jobImportFile(PROJECT_NAME,JobUtils.updateJobFileToImport(JOB_XML_NAME,PROJECT_NAME,newArgs) as String,client, JobUtils.DUPE_OPTION_UPDATE)

        def updatedStatus1 = scmClient.callGetJobStatus(DUMMY_JOB_ID).response
        updatedStatus1.actions.size() == 1
        updatedStatus1.commit.size()== 5
        updatedStatus1.id == DUMMY_JOB_ID
        updatedStatus1.integration == EXPORT_INTEGRATION
        updatedStatus1.message == "Modified"
        updatedStatus1.project == PROJECT_NAME
        updatedStatus1.synchState == "EXPORT_NEEDED"

        def performAction1= scmClient.callPerformJobAction(actionId, actionRequest, DUMMY_JOB_ID).response
        performAction1.message == "SCM export Action was Successful: ${actionId}"
        performAction1.success == true

        def updatedStatus2 = scmClient.callGetJobStatus(DUMMY_JOB_ID).response
        updatedStatus2.actions.size() == 0
        updatedStatus2.commit.size()== 5
        updatedStatus2.id == DUMMY_JOB_ID
        updatedStatus2.integration == EXPORT_INTEGRATION
        updatedStatus2.message == "No Change"
        updatedStatus2.project == PROJECT_NAME
        updatedStatus2.synchState == "CLEAN"


    }

}
