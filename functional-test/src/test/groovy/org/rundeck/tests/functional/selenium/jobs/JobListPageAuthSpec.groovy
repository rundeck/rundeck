package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.AdhocPage
import spock.lang.Shared

/**
 * Verify Authorization checks on Job List page
 *
 * ACLS:
 * acls in JobListPageAuthSpec.aclpolicy
 * Common acls: project read required, job read required
 *
 * AuthTest1: project read, event read
 * AuthTest2: project admin, event read
 * AuthTest3: project app_admin, event read
 * AuthTest4: project delete_execution, event read
 * AuthTest5: project read, NO event read
 */
@SeleniumCoreTest
class JobListPageAuthSpec extends SeleniumBase {

    static final String PROJECT_NAME = 'JobListPageAuthSpec'
    public static final String ACLPOLICY_FILE = PROJECT_NAME + ".aclpolicy"

    static final String USER_PASSWORD = 'password'
    @Shared
    String jobId

    def setupSpec() {
        setupProject(PROJECT_NAME)
        importSystemAcls("/${ACLPOLICY_FILE}", ACLPOLICY_FILE)
        //run job in the project
        def jobXml1 = JobUtils.generateScheduledExecutionXml("test job 1")

        def job1CreatedParsedResponse = JobUtils.createJob(PROJECT_NAME, jobXml1, client)
        jobId = job1CreatedParsedResponse.succeeded[0].id
        def result = JobUtils.executeJob(jobId, client)
        assert result.successful
        Execution exec = MAPPER.readValue(result.body().string(), Execution.class)
        JobUtils.waitForExecution(
            ExecutionStatus.SUCCEEDED.state,
            exec.id as String,
            client,
            WaitingTime.EXCESSIVE
        )
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
        deleteSystemAcl(ACLPOLICY_FILE)
    }


    def "activity section shown, user #user sees bulk delete based on auth #expected"() {
        given: "login as user"
            def login = page LoginPage
            login.go()
            login.login(user, USER_PASSWORD)
            waitForPageLoadComplete()
            def jobs = page JobListPage, PROJECT_NAME
        when: "view jobs list page"
            jobs.go()
        then: "activity section is shown, bulk edit button displayed based on authz"
            //bulk edit
            jobs.activitySection.displayed

            if (expected) {
                assert jobs.activityBulkDeleteBtn.displayed
            } else {
                assert !jobs.els(AdhocPage.activityBulkDeleteBtnBy)
            }

        where:
            user        | expected
            'AuthTest1' | false
            'AuthTest2' | true
            'AuthTest3' | true
            'AuthTest4' | true
    }
    def "user #user has no activity section"() {
        given: "login as user"
            def login = page LoginPage
            login.go()
            login.login(user, USER_PASSWORD)
            waitForPageLoadComplete()
            def jobs = page JobListPage, PROJECT_NAME
        when: "view jobs list page"
            jobs.go()
        then: "activity section is not shown"
            !jobs.els(AdhocPage.activitySectionBy)
        where:
            user = 'AuthTest5'
    }
}
