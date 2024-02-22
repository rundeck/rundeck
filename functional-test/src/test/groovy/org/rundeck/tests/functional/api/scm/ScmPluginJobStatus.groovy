package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.IntegrationStatusResponse
import org.rundeck.util.api.scm.httpbody.ScmJobStatusResponse
import org.rundeck.util.container.BaseContainer

@APITest
class ScmPluginJobStatus extends BaseContainer{

    static final String PROJECT_NAME = "ScmPluginJobStatus-project"
    final String EXPORT_INTEGRATION= "export"
    final String DUMMY_JOB_ID = "383d0599-3ea3-4fa6-ac3a-75a53d611111"
    final String JOB_XML_NAME = "job-template-common.xml"


    def "project scm export status must be clean after setup on empty project"(){
        given:
        setupProject(PROJECT_NAME)
        jobImportFile(PROJECT_NAME, updateFile(JOB_XML_NAME,null,null,null,null, null,null,DUMMY_JOB_ID))

        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(EXPORT_INTEGRATION).forProject(PROJECT_NAME)
        scmClient.callSetupIntegration(GitExportSetupRequest.defaultRequest(PROJECT_NAME))

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
