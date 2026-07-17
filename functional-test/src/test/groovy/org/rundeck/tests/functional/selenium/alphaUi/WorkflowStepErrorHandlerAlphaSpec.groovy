package org.rundeck.tests.functional.selenium.alphaUi

import org.rundeck.util.annotations.AlphaUiSeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.StepType
import org.rundeck.util.gui.pages.login.LoginPage

/**
 * RUN-4317: with rundeck.feature.earlyAccessJobConditional.enabled on (hardcoded in this
 * Gradle task's dedicated docker-compose-early-access-conditional.yml via
 * RUNDECK_FEATURE_EARLYACCESSJOBCONDITIONAL_ENABLED), WorkflowSteps.vue renders the inline
 * error handler form directly instead of opening the legacy EditPluginModal. Counterpart to
 * JobsSpec's "Error handlers" test, which covers the flag-disabled (default) legacy-modal
 * path under seleniumCoreTest.
 */
@AlphaUiSeleniumCoreTest
class WorkflowStepErrorHandlerAlphaSpec extends SeleniumBase {

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def "error handler renders inline when earlyAccessJobConditional is enabled"() {
        given: "a job with a command step"
            String projectName = "WorkflowStepErrorHandlerAlpha-${UUID.randomUUID()}"
            setupProject(projectName)
            def jobCreatePage = go(JobCreatePage, projectName)
            jobCreatePage.fillBasicJob "inline error handler alpha test"

        when: "adding a non-job-reference error handler"
            jobCreatePage.addErrorHandler('exec-command', StepType.NODE)

        then: "the inline form renders in place of the legacy modal"
            jobCreatePage.isInlineErrorHandlerFormVisible()

        cleanup:
            deleteProject(projectName)
    }
}
