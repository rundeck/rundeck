package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage

/**
 * Workflow strategy selection in the default (Vue) OSS workflow editor.
 *
 * <p>Kept out of the {@code @Stepwise} {@link JobsSpec}: Spock re-includes every feature that
 * precedes an included one in a stepwise spec, which would override {@code @ExcludePro}.
 * Excluded from the Enterprise run because Enterprise renders its own strategy editor by default
 * (no native select or strategy description panel).
 */
@SeleniumCoreTest
@ExcludePro
@UiModeFlag(
    featureName = "jobs-options-workflow",
    status      = UiModeStatus.PROMOTED,
    description = "Default (Vue) workflow tab strategy select; the legacy variant stays in JobsSpec."
)
class JobWorkflowStrategySpec extends SeleniumBase {

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def "change workflow strategy"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
            def jobShowPage = page JobShowPage
        then:
            jobCreatePage.go()
            jobCreatePage.jobNameInput.sendKeys 'jobs workflow strategy'
            jobCreatePage.tab JobTab.WORKFLOW click()
            jobCreatePage.workFlowStrategyField.sendKeys 'Parallel'
            jobCreatePage.waitIgnoringForElementVisible jobCreatePage.strategyPluginParallelField
            jobCreatePage.strategyPluginParallelMsgField.getText() == 'Run all steps in parallel'

            jobCreatePage.addSimpleCommandStepNextUi 'echo selenium test', 0
            jobCreatePage.createJobButton.click()
        expect:
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.workflowDetailField.getText() == 'Parallel Run all steps in parallel'
    }
}
