package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.AdhocPage
import spock.lang.Shared

/**
 * Verify Authorization checks on Job List page
 *
 * ACLS:
 * acls in ActivitySectionAuthSpec.aclpolicy
 * Common acls: project read required, job read required
 *
 * AuthTest1: project read, event read
 * AuthTest2: project admin, event read
 * AuthTest3: project app_admin, event read
 * AuthTest4: project delete_execution, event read
 * AuthTest5: project read, NO event read
 */
@SeleniumCoreTest
class ActivitySectionAuthSpec extends SeleniumBase {

    static final String PROJECT_NAME = 'ActivitySectionAuthSpec'
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
        //run adhoc execution in the project
        def adhoc = post("/project/${PROJECT_NAME}/run/command?exec=echo+testing+execution+bulk+delete+auth", Map)
        JobUtils.waitForExecution(
            ExecutionStatus.SUCCEEDED.state,
            adhoc.execution.id.toString(),
            client,
            WaitingTime.EXCESSIVE
        )
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
        deleteSystemAcl(ACLPOLICY_FILE)
    }


    def "activity section on page #pageName user #user sees bulk delete based on auth #expected"() {
        given: "login as user"
            def login = page LoginPage

            login.go()
            login.login(user, USER_PASSWORD)
            waitForPageLoadComplete()
            def testPage = page(pageName, PROJECT_NAME)

        when: "view page"
            testPage.go()
            waitForPageLoadComplete()
        then: "activity section is shown, bulk edit button displayed based on authz"
            //bulk edit
            testPage.activitySection.displayed

            if (expected) {
                assert testPage.activityBulkDeleteBtn.displayed
            } else {
                assert !testPage.els(AdhocPage.activityBulkDeleteBtnBy)
            }

        cleanup:
            def topMenuPage = page TopMenuPage
            topMenuPage.logOut()
            waitForPageLoadComplete()
        where:
            [pageName, [user, expected]] << [
                [JobListPage, AdhocPage],
                [
                    ['AuthTest1', false],
                    ['AuthTest2', true],
                    ['AuthTest3', true],
                    ['AuthTest4', true],
                ]
            ].combinations()
    }

    def "user #user has no activity section on #pageName"() {
        given: "login as user"
            def login = page LoginPage
            login.go()
            login.login(user, USER_PASSWORD)
            waitForPageLoadComplete()
            def page = page pageName, PROJECT_NAME
        when: "view page"
            page.go()
            waitForPageLoadComplete()
        then: "activity section is not shown"
            !page.els(page.activitySectionBy)
        where:
            user = 'AuthTest5'
            pageName << [JobListPage, AdhocPage]

    }
}
