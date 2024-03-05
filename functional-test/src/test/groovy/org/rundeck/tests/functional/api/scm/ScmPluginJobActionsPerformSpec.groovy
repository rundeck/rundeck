package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.ScmActionPerformRequest
import org.rundeck.util.api.scm.httpbody.SetupIntegrationResponse
import org.rundeck.util.container.BaseContainer

@APITest
@ExcludePro
class ScmPluginJobActionsPerformSpec extends BaseContainer{

    static final String PROJECT_NAME = "ScmPluginJobActionsPerform-project"
    final String EXPORT_INTEGRATION= "export"
    final String DUMMY_JOB_ID = "383d0599-3ea3-4fa6-ac3a-75a53d6bfdf3"
    final String JOB_XML_NAME = "job-template-common.xml"
    static final GiteaApiRemoteRepo remoteRepo = new GiteaApiRemoteRepo('repoExample4')

    def setupSpec() {
        remoteRepo.setupRepo()
    }

    def "test_job_action_perform"(){
        given:
        setupProject(PROJECT_NAME)
        def args =["uuid": DUMMY_JOB_ID]
        JobUtils.jobImportFile(PROJECT_NAME,JobUtils.updateJobFileToImport(JOB_XML_NAME,PROJECT_NAME,args) as String,client)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(EXPORT_INTEGRATION).forProject(PROJECT_NAME)
        scmClient.callSetupIntegration(GitExportSetupRequest.defaultRequest().forProject(PROJECT_NAME).withRepo(remoteRepo))
        ScmActionPerformRequest actionRequest = new ScmActionPerformRequest([
                input: [ message : "Commit msg example" ],
                jobs: [ DUMMY_JOB_ID ]
        ])
        def actionId = 'job-commit'

        when:
        SetupIntegrationResponse performAction = scmClient.callPerformJobAction(actionId,actionRequest,DUMMY_JOB_ID).response

        then:
        verifyAll {
            performAction.message == "SCM export Action was Successful: ${actionId}"
            performAction.success == true
        }
    }
}
