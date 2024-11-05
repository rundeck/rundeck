package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.BasePage
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.activity.ActivityPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.AdhocPage
import spock.lang.Shared

/**
 * Verify Authorization checks for display of the Activity List section on these pages:
 * * Job Show Page
 * * Job List Page
 * * Adhoc run page
 * * Execution Show page
 * * Activity List page
 *
 * The Activity section should not be shown unless user has Project auth for admin, app_admin or Events auth for read actions
 * The Bulk Delete button should not be shown unless the user has Project auth for admin, app_admin or delete_execution actions
 *
 * ACLS:
 * acls in ActivitySectionAuthSpec.aclpolicy
 * Common acls: project read required, job read required, adhoc read and run required.
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
    @Shared
    String execId

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
        execId = exec.id as String
        JobUtils.waitForExecution(
            ExecutionStatus.SUCCEEDED.state,
            execId,
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

            def testPage = getTestPage(pageName)

        when: "view page"
            testPage.go()
            waitForPageLoadComplete()
            //if job show page, click Activity link
            if (pageName == 'jobShow' || pageName == 'execShow') {
                testPage.activitySectionActivityTabLink.click()
                waitForPageLoadComplete()
            }
        then: "activity section is shown, bulk edit button displayed based on authz"
            //bulk edit
            testPage.activityList.displayed

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
                [JobListPage, AdhocPage, ActivityPage, 'jobShow', 'execShow'],
                [
                    ['AuthTest1', false],
                    ['AuthTest2', true],
                    ['AuthTest3', true],
                    ['AuthTest4', true],
                ]
            ].combinations()
    }

    /**
     * Load correct page object, the execution show and job show pages are created dynamically, otherwise use the supplied Page class.
     * @param pageName name of page or Class for page
     * @return
     */
    BasePage getTestPage(def pageName) {
        if (pageName == 'jobShow') {
            return page(JobShowPage, PROJECT_NAME).forJob(jobId)
        } else if (pageName == 'execShow') {
            return page(ExecutionShowPage, "/project/${PROJECT_NAME}/execution/show/${execId}".toString())
        }
        return page(pageName, PROJECT_NAME)
    }

    def "user #user has no activity section on #pageName"() {
        given: "login as user"
            def login = page LoginPage
            login.go()
            login.login(user, USER_PASSWORD)
            waitForPageLoadComplete()
            def testPage = getTestPage(pageName)
        when: "view page"
            testPage.go()
            waitForPageLoadComplete()
        then: "activity section is not shown"
            !testPage.els(testPage.activityListBy)
        where:
            user = 'AuthTest5'
            pageName << [JobListPage, AdhocPage, ActivityPage, 'jobShow', 'execShow']

    }
}
