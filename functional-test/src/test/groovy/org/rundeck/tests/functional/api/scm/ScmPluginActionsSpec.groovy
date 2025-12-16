package org.rundeck.tests.functional.api.scm

import groovy.util.logging.Slf4j
import org.rundeck.util.annotations.APITest
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo
import org.rundeck.util.api.scm.httpbody.IntegrationStatusResponse
import org.rundeck.util.api.scm.httpbody.ScmActionInputFieldsResponse
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.ScmActionPerformRequest
import org.rundeck.util.api.scm.httpbody.SetupIntegrationResponse
import org.rundeck.util.api.storage.KeyStorageApiClient
import org.rundeck.util.common.scm.ScmActionId
import org.rundeck.util.common.scm.ScmIntegration
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.WaitUtils
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.common.jobs.JobUtils

@Slf4j
@APITest
@ExcludePro
class ScmPluginActionsSpec extends ScmBaseContainer {
    static final String PROJECT_NAME = 'ScmPluginActionsSpec'
    static final String BASE_EXPORT_PROJECT_LOCATION = '/projects-import/scm/project-scm-export-one-job.rdproject'
    static final GiteaApiRemoteRepo remoteRepo = new GiteaApiRemoteRepo('repoExample')

    def setupSpec() {
        remoteRepo.setupRepo()
    }

    /**
     * Pattern #48: SCM Git-Export Plugin Setup Race Condition
     * 
     * Race condition between Gitea repo creation and Rundeck SCM plugin setup:
     * - Gitea creates the git repo successfully
     * - Rundeck's git-export plugin tries to clone/connect immediately
     * - Git repo file system may not be fully initialized
     * - Results in intermittent 500 errors during callSetupIntegration
     * 
     * Fix: Add retry logic (similar to GiteaApiRemoteRepo.setupRepo)
     */
    private boolean setupScmIntegrationWithRetry(GitScmApiClient scmClient, GitExportSetupRequest request) {
        def setupCall = {
            return scmClient.callSetupIntegration(request, 200..599) // Accept all codes for retry logic
        }
        def successVerify = { response ->
            return response?.successful && response?.response?.success
        }
        
        try {
            WaitUtils.waitFor(setupCall, successVerify, WaitingTime.EXCESSIVE)
            return true
        } catch (Exception e) {
            log.warn("SCM setup failed after retries: ${e.message}", e)
            return false
        }
    }

    def "project scm status must be export needed when having a new job"(){
        given:
        ScmIntegration integration = ScmIntegration.EXPORT
        String projectName = "${PROJECT_NAME}-P1"
        setupProjectArchiveDirectoryResource(projectName, BASE_EXPORT_PROJECT_LOCATION)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)

        expect:
        setupScmIntegrationWithRetry(scmClient, GitExportSetupRequest.defaultRequest().forProject(projectName).withRepo(remoteRepo))

        when:
        IntegrationStatusResponse retrievedStatus = scmClient.callGetIntegrationStatus().response

        then:
        verifyAll {
            retrievedStatus.integration == integration
            retrievedStatus.message == 'Some changes have not been committed'
            retrievedStatus.synchState == 'EXPORT_NEEDED'
            retrievedStatus.project == projectName
            retrievedStatus.actions.size() == 1
            retrievedStatus.actions.first() == 'project-commit'
        }
    }

    def "retrieve all input fields on scm action for project with new job"(){
        given:
        String projectName = "${PROJECT_NAME}-P2"
        ScmIntegration integration = ScmIntegration.EXPORT
        ScmActionId actionId = ScmActionId.PROJECT_COMMIT
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id

        expect:
        setupScmIntegrationWithRetry(scmClient, GitExportSetupRequest.defaultRequest().forProject(projectName).withRepo(remoteRepo))

        when:
        ScmActionInputFieldsResponse actionFields = scmClient.callGetFieldsForAction(actionId).response

        then:
        verifyAll {
            actionFields.actionId == actionId
            actionFields.integration == integration
            actionFields.title == 'Commit Changes to Git'
            actionFields.description == 'Commit changes to local git repo.'
            !actionFields.importItems

            actionFields.fields.size() == 3
            actionFields.fields[0].toMap() == [
                    defaultValue: null,
                    description: "Enter a commit message. Committing to branch: `master`",
                    name: "message",
                    renderingOptions: [
                            displayType: 'MULTI_LINE'
                    ],
                    required: true,
                    scope: null,
                    title: "Commit Message",
                    type: "String",
                    values: null
            ]
            actionFields.fields[1].toMap() == [
                    defaultValue: null,
                    description: "Enter a tag name to include, will be pushed with the branch.",
                    name: "tagName",
                    renderingOptions: [:],
                    required: false,
                    scope: null,
                    title: "Tag",
                    type: "String",
                    values: null
            ]
            actionFields.fields[2].toMap() == [
                    defaultValue: "true",
                    description: "Check to push to the remote",
                    name: "push",
                    renderingOptions: [:],
                    required: false,
                    scope: null,
                    title: "Push Remotely?",
                    type: "Boolean",
                    values: null
            ]

            actionFields.exportItems.size() == 1
            actionFields.exportItems.first().toMap() == [
                    deleted: false,
                    itemId: "api-test/cli job-${jobUuid}.xml",
                    job: [
                            groupPath: 'api-test',
                            jobId: "${jobUuid}",
                            jobName: "cli job"
                    ],
                    status: "CREATE_NEEDED",
                    originalId: null,
                    renamed: false
            ]
        }
    }

    def "perform project scm action with success"(){
        given:
        String projectName = "${PROJECT_NAME}-autopush-${useAutoPush}"
        setupProject(projectName)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(integration).forProject(projectName)
        def jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id
        String keyStoragePath = 'keys/scm.password'

        ScmActionPerformRequest actionRequest = new ScmActionPerformRequest([
                input: [ message : 'Commit msg example', push: useAutoPush.toString()],
                jobs: [ jobUuid ]
        ])


        expect:
        if(useAutoPush)
            remoteRepo.storeRepoPassInRundeck(new KeyStorageApiClient(clientProvider), keyStoragePath).successful
        setupScmIntegrationWithRetry(scmClient, GitExportSetupRequest.defaultRequest().forProject(projectName).withRepo(remoteRepo))

        when:
        SetupIntegrationResponse performActionResult = scmClient.callPerformAction(actionId, actionRequest).response

        then:
        IntegrationStatusResponse finalScmStatus = scmClient.callGetIntegrationStatus().response
        verifyAll {
            performActionResult.success
            performActionResult.message == "SCM ${integration.name} Action was Successful: ${actionId}"
            finalScmStatus.actions == expectedFinalScmActions
            finalScmStatus.synchState == expectedFinalSynchState
        }

        where:
        integration           | actionId          | useAutoPush | expectedFinalSynchState | expectedFinalScmActions
        ScmIntegration.EXPORT | 'project-commit'  | false       | 'EXPORT_NEEDED'         | ['project-push']
        ScmIntegration.EXPORT | 'project-commit'  | true        | 'CLEAN'                 | null
    }
}
