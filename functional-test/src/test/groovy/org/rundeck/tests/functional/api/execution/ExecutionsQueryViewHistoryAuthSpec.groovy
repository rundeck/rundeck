package org.rundeck.tests.functional.api.execution

import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer
import spock.lang.Shared

/**
 * RUN-4665: a user granted only the job {@code view_history} ACL action (without {@code read})
 * must still see the execution list from {@code GET /project/{project}/executions}, not just the
 * total count. Regression coverage for the fix in
 * {@code ExecutionController.apiExecutionsQueryv14} (filterAuthorizedProjectExecutionsAny).
 */
@APITest
class ExecutionsQueryViewHistoryAuthSpec extends BaseContainer {
    static final String TEST_PROJECT = "ExecutionsQueryViewHistoryAuth"
    static final String ACLPOLICY_FILE = "ExecutionsQueryViewHistoryAuthSpec.aclpolicy"
    static final String VIEW_HISTORY_GROUP = "Run4665ViewHistory"
    static final String NO_ACCESS_GROUP = "Run4665NoAccess"
    static final int EXECUTION_COUNT = 3

    @Shared
    String jobId
    @Shared
    String viewHistoryToken
    @Shared
    String noAccessToken

    def setupSpec() {
        setupProject(TEST_PROJECT)
        importSystemAcls("/${ACLPOLICY_FILE}", ACLPOLICY_FILE)

        def jobXml = JobUtils.generateScheduledExecutionXml("run4665-history-job")
        jobId = JobUtils.createJob(TEST_PROJECT, jobXml, client).succeeded[0].id

        EXECUTION_COUNT.times {
            def execution = JobUtils.runExecuteJob(jobId, client)
            JobUtils.waitForSuccess(execution.id as String, client)
        }

        viewHistoryToken = client.post(
            "/tokens/run4665-view-history-user",
            [roles: [VIEW_HISTORY_GROUP]],
            Map
        ).token
        noAccessToken = client.post(
            "/tokens/run4665-no-access-user",
            [roles: [NO_ACCESS_GROUP]],
            Map
        ).token
    }

    def cleanupSpec() {
        deleteProject(TEST_PROJECT)
        deleteSystemAcl(ACLPOLICY_FILE)
    }

    def "user with only view_history sees the execution list, not just the count"() {
        given: "a client authenticated as a user with only the view_history job ACL action"
            def viewHistoryClient = clientWithToken(viewHistoryToken)

        when: "querying the executions API for the job"
            def result = viewHistoryClient.get(
                "/project/${TEST_PROJECT}/executions?jobIdListFilter=${jobId}",
                Map
            )

        then: "the full execution list is returned, matching the total count"
            result.paging.total == EXECUTION_COUNT
            result.executions.size() == EXECUTION_COUNT
    }

    def "user with neither read nor view_history sees an empty execution list"() {
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