package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.AdhocPage

/**
 * Test Authorization checks on the Adhoc page
 * acls in AdhocAuthSpec.aclpolicy
 * Common acls: project read required, adhoc run required
 *
 * AuthTest1: project read, event read
 * AuthTest2: project admin, event read
 * AuthTest3: project app_admin, event read
 * AuthTest4: project delete_execution, event read
 * AuthTest5: project read, NO event read
 */
@SeleniumCoreTest
class AdhocAuthSpec extends SeleniumBase {

    static final String PROJECT_NAME = 'AdhocAuthSpec'
    public static final String ACLPOLICY_FILE = PROJECT_NAME + ".aclpolicy"

    static final String USER_PASSWORD = 'password'

    def setupSpec() {
        setupProject(PROJECT_NAME)
        importSystemAcls("/${ACLPOLICY_FILE}", ACLPOLICY_FILE)
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

    def "activity section shown, user #user sees bulk delete based on auth #expected"() {
        given: "login as user"
            def login = page LoginPage
            login.go()
            login.login(user, USER_PASSWORD)
            waitForPageLoadComplete()
            def adhoc = page AdhocPage, PROJECT_NAME
        when: "view adhoc page"
            adhoc.go()
        then: "activity section bulk edit button displayed based on authz"
            //bulk edit
            adhoc.activitySection.displayed

            if (expected) {
                assert adhoc.activityBulkDeleteBtn.displayed
            } else {
                assert !adhoc.els(AdhocPage.activityBulkDeleteBtnBy)
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
            def adhoc = page AdhocPage, PROJECT_NAME
        when: "view adhoc page"
            adhoc.go()
        then: "activity section is not shown"
            !adhoc.els(AdhocPage.activitySectionBy)
        where:
            user = 'AuthTest5'
    }
}
