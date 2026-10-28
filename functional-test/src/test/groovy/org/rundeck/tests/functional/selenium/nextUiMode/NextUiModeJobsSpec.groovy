package org.rundeck.tests.functional.selenium.nextUiMode

import org.rundeck.util.annotations.NextUiModeSeleniumCoreTest
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.login.LoginPage

/**
 * Runs with the nextUiMode feature flag enabled for the whole container
 * (Gradle task nextUiModeSeleniumCoreTest). Covers the jobs page with Next UI
 * enabled globally and no nextUi cookie, where the layout and the controller
 * previously picked different templates and showed two job list headers.
 */
@NextUiModeSeleniumCoreTest
@UiModeFlag(
    featureName = "next-ui-mode-global",
    status      = UiModeStatus.NEXT_UI,
    description = "Enabled by the global nextUiMode feature flag, not a URL param or cookie"
)
class NextUiModeJobsSpec extends SeleniumBase {

    static final String PROJECT_NAME = 'next_ui_mode_jobs'

    def setupSpec() {
        setupProject(PROJECT_NAME)
    }

    void setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
    }

    void "jobs page renders only the Next UI job list with the global flag and no cookie"() {
        given:
        def jobListPage = page(JobListPage, PROJECT_NAME)
        jobListPage.clearNextUiCookie()

        when:
        jobListPage.go()

        then:
        jobListPage.pageUiType == 'next'
        jobListPage.hasNextUiJobListTemplate()
        !jobListPage.hasCurrentUiJobListTemplate()
    }
}
