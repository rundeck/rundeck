package org.rundeck.tests.functional.api.execution

import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer
import spock.lang.Shared

/**
 * A user granted only the job {@code view} ACL action (without {@code read} or
 * {@code view_history}) must still see the execution list from
 * {@code GET /project/{project}/executions}, not just the total count. Regression coverage for
 * the fix in {@code ExecutionController.apiExecutionsQueryv14} (filterAuthorizedProjectExecutionsAny).
 */
@APITest
class ExecutionsQueryViewAuthSpec extends BaseContainer {
    static final String TEST_PROJECT = "ExecutionsQueryViewAuth"
    static final String ACLPOLICY_FILE = "ExecutionsQueryViewAuthSpec.aclpolicy"
    static final String VIEW_GROUP = "ExecViewOnlyGroup"
    static final String NO_ACCESS_GROUP = "ExecNoAccessGroup"
    static final int EXECUTION_COUNT = 3

    @Shared
    String jobId
    @Shared
    String viewToken
    @Shared
    String noAccessToken

    def setupSpec() {
        setupProject(TEST_PROJECT)
        importSystemAcls("/${ACLPOLICY_FILE}", ACLPOLICY_FILE)

        def jobXml = JobUtils.generateScheduledExecutionXml("view-only-repro-job")
        jobId = JobUtils.createJob(TEST_PROJECT, jobXml, client).succeeded[0].id

        EXECUTION_COUNT.times {
            def execution = JobUtils.runExecuteJob(jobId, client)
            JobUtils.waitForSuccess(execution.id as String, client)
        }

        viewToken = client.post(
            "/tokens/exec-view-only-user",
            [roles: [VIEW_GROUP]],
            Map
        ).token
        noAccessToken = client.post(
            "/tokens/exec-no-access-user",
            [roles: [NO_ACCESS_GROUP]],
            Map
        ).token
    }

    def cleanupSpec() {
        deleteProject(TEST_PROJECT)
        deleteSystemAcl(ACLPOLICY_FILE)
    }

    def "user with only view sees the execution list, not just the count"() {
        given: "a client authenticated as a user with only the view job ACL action"
            def viewClient = clientWithToken(viewToken)

        when: "querying the executions API for the job"
            def result = viewClient.get(
                "/project/${TEST_PROJECT}/executions?jobIdListFilter=${jobId}",
                Map
            )

        then: "the full execution list is returned, matching the total count"
            result.paging.total == EXECUTION_COUNT
            result.executions.size() == EXECUTION_COUNT
    }

    def "user with neither read, view, nor view_history sees an empty execution list"() {
        given: "a client authenticated as a user with project access but no job-level authorization"
            def noAccessClient = clientWithToken(noAccessToken)

        when: "querying the executions API for the job"
            def result = noAccessClient.get(
                "/project/${TEST_PROJECT}/executions?jobIdListFilter=${jobId}",
                Map
            )

        then: "no executions are visible, regardless of the reported total"
            result.executions.size() == 0
    }
}
