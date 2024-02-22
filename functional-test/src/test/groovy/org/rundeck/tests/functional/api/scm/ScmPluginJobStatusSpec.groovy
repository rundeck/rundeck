package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.JobUtils
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.ScmJobStatusResponse
import org.rundeck.util.container.BaseContainer

@APITest
class ScmPluginJobStatusSpec extends BaseContainer{

    static final String PROJECT_NAME = "ScmPluginJobStatus-project"
    final String EXPORT_INTEGRATION= "export"
    final String DUMMY_JOB_ID = "383d0599-3ea3-4fa6-ac3a-75a53d611111"
    final String JOB_XML_NAME = "job-template-common.xml"
    static final GiteaApiRemoteRepo remoteRepo = new GiteaApiRemoteRepo('repoExample2')

    def setupSpec() {
        remoteRepo.setupRepo()
    }

    def "project scm export status must be clean after setup on empty project"(){
        given:
        setupProject(PROJECT_NAME)
        def args =["uuid": DUMMY_JOB_ID]
        JobUtils.jobImportFile(PROJECT_NAME,JobUtils.updateJobFileToImport(JOB_XML_NAME,PROJECT_NAME,args) as String,client)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(EXPORT_INTEGRATION).forProject(PROJECT_NAME)
        scmClient.callSetupIntegration(GitExportSetupRequest.defaultRequest().forProject(PROJECT_NAME).withRepo(remoteRepo))

        when:
        ScmJobStatusResponse status = scmClient.callGetJobStatus(DUMMY_JOB_ID).response

        then:
        verifyAll {
            status.actions.size() == 1
            status.commit == null
            status.id == DUMMY_JOB_ID
            status.integration == EXPORT_INTEGRATION
            status.message == "Created"
            status.project == PROJECT_NAME
            status.synchState == "CREATE_NEEDED"
        }
    }
}