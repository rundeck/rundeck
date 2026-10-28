package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.UiModes
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage

/**
 * Verifies that the base layout and the page template agree on uiType for the
 * nextUi URL parameter on pages not matched by NextUiInterceptor (RUN-5124).
 * A disagreement leaves Next UI placeholders that never mount.
 */
@SeleniumCoreTest
@UiModeFlag(featureName = "job-stats-nextui", status = UiModeStatus.NEXT_UI)
class NextUiParamSpec extends SeleniumBase {

    static final UI_MODES = UiModes.nextUiAndDefault()
    static final String PROJECT_NAME = 'next_ui_param'

    def setupSpec() {
        startEnvironment()
        setupProject(PROJECT_NAME)
    }

    void setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
    }

    void "job show page uiType matches template for nextUi=#nextUi"() {
        given:
        String jobUuid = JobUtils.jobImportFile(PROJECT_NAME, '/test-files/test.xml', client).succeeded.first().id
        def jobShowPage = page(JobShowPage, PROJECT_NAME)

        when:
        jobShowPage.go("/project/${PROJECT_NAME}/job/show/${jobUuid}?nextUi=${nextUi}")

        then:
        jobShowPage.pageUiType == (nextUi ? 'next' : 'current')
        jobShowPage.hasNextUiJobStats() == nextUi
        jobShowPage.hasCurrentUiJobStats() == !nextUi

        where:
        [nextUi] << UI_MODES
    }

    void "execution show page uiType matches template for nextUi=#nextUi"() {
        given:
        String jobUuid = JobUtils.jobImportFile(PROJECT_NAME, '/test-files/test.xml', client).succeeded.first().id
        String execId = JobUtils.runExecuteJob(jobUuid, client).id as String
        def executionShowPage = page(ExecutionShowPage)

        when:
        executionShowPage.go("/project/${PROJECT_NAME}/execution/show/${execId}?nextUi=${nextUi}")

        then:
        executionShowPage.pageUiType == (nextUi ? 'next' : 'current')
        executionShowPage.hasNextUiJobStats() == nextUi
        executionShowPage.hasCurrentUiJobStats() == !nextUi

        where:
        [nextUi] << UI_MODES
    }
}
