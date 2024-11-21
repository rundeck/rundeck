package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo
import org.rundeck.util.api.scm.httpbody.GitImportSetupRequest
import org.rundeck.util.api.scm.httpbody.ScmActionPerformRequest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionUtils
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.common.scm.ScmIntegration
import org.rundeck.util.container.BaseContainer

@APITest
@ExcludePro
class ScmPluginImportScheduledJobSpec extends BaseContainer {

    static final String PROJECT_NAME = UUID.randomUUID().toString()
    static final GiteaApiRemoteRepo remoteRepo = new GiteaApiRemoteRepo("repo-${UUID.randomUUID()}")

    def setupSpec() {
        remoteRepo.setupRepo()
    }

    def "scheduled job executes after import and after update on remote"() {
        given:
        // Initial setup of the project and action parameters
        setupProject(PROJECT_NAME)
        final def jobId = UUID.randomUUID().toString()
        final def jobItemId = "group-test/job-test-${jobId}.xml".toString()

        final String initialJobName = "sample-job-1"
        final String updatedJobName = "updated-sample-job-1"

        final String initialJobSchedule = "*/3 * * ? * * *"
        final String updatedJobSchedule = "*/4 * * ? * * *"

        // Generate an XML definition for a scheduled job
        String jobXmlDefinitionFile = JobUtils.updateJobFileToImport("api-test-executions-running-scheduled.xml",
                PROJECT_NAME,
                ["uuid": jobId, "job-name": initialJobName, "schedule-crontab": initialJobSchedule])
        def jobXmlDefinition = new File(jobXmlDefinitionFile).text

        // Create the job file in the git repo
        remoteRepo.createFile(jobItemId, jobXmlDefinition)

        // Configure the project to import from the git repo
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(ScmIntegration.IMPORT).forProject(PROJECT_NAME)
        scmClient.callSetupIntegration(GitImportSetupRequest.defaultRequest().forProject(PROJECT_NAME).withRepo(remoteRepo))

        when: "scheduled job is imported from the scm repo"
        scmClient.callPerformAction("import-jobs", new ScmActionPerformRequest([ items: [jobItemId] ])).response

        then: "scheduled job is executing"
        // Waits for at least one execution to start
        def executionsAfterCreation = waitFor(ExecutionUtils.Retrievers.executionsForProject(client, PROJECT_NAME),
                { !!it},
                WaitingTime.EXCESSIVE)
        executionsAfterCreation.size() > 0

        when: "scheduled job definition and schedule is updated in the repo and re-imported"
        remoteRepo.updateFile(jobItemId, jobXmlDefinition.replace(initialJobName, updatedJobName).replace(initialJobSchedule, updatedJobSchedule))

        // It takes time for the job changes to get propagated through gitea, thus wait for the job name to be updated
        waitFor( {
            scmClient.callPerformAction("remote-fetch", new ScmActionPerformRequest()).response
            scmClient.callPerformAction("import-jobs", new ScmActionPerformRequest([ items: [jobItemId] ])).response
            JobUtils.getJobDetailsById(jobId, MAPPER, client)},
                { it.name == updatedJobName },
                WaitingTime.EXCESSIVE)

        then: "the scheduled job still executes"
        // Wait for more executions to be created
        def executionsAfterNameUpdate = waitFor(ExecutionUtils.Retrievers.executionsForProject(client, PROJECT_NAME),
                { it.size() > executionsAfterCreation.size()},
                WaitingTime.EXCESSIVE)

        executionsAfterNameUpdate.collect( { it.id } ).containsAll(executionsAfterCreation.collect( { it.id } ))
    }
}
