package org.rundeck.tests.functional.selenium.project


import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.DashboardPage
import spock.lang.Unroll
/**
 * Test authorization checks used in GUI for Project Home page.
 * ACL Policy used:
 *
 * AuthTest1: project read only
 * AuthTest2: project admin
 * AuthTest3: project app_admin
 * AuthTest4: project read, events read
 */
@SeleniumCoreTest
class ProjectDashboardAuthSpec extends SeleniumBase {
    static final String PROJECT_NAME = 'ProjectDashboardAuthSpec'
    static final String ACLPOLICY_NAME = PROJECT_NAME + '.aclpolicy'

    def setupSpec() {
        setupProject(PROJECT_NAME)
        importSystemAcls("/${ACLPOLICY_NAME}", ACLPOLICY_NAME)
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
        deleteSystemAcl(ACLPOLICY_NAME)
    }

    @Unroll
    def "home page shows summary #expected for user #user"() {
        given:
            def loginPage = go LoginPage
            loginPage.login(user, 'password')
            waitForPageLoadComplete()
        when:
            def dashboardPage = go DashboardPage, PROJECT_NAME
            waitForPageLoadComplete()
        then:

            if (expected) {
                assert dashboardPage.projectSummaryCountLink.displayed
            } else {
                assert !dashboardPage.els(dashboardPage.projectSummaryCountLinkBy)
            }
        where:
            user        | expected
            'AuthTest1' | false
            'AuthTest2' | true
            'AuthTest3' | true
            'AuthTest4' | true
    }
}
