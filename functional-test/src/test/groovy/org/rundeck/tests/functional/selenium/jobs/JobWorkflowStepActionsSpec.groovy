package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.By
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.UiModes
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobReferenceStep
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.jobs.StepType
import org.rundeck.util.gui.pages.login.LoginPage

import java.util.stream.Collectors

/**
 * Workflow step actions that drive the OSS workflow editor's step duplication control
 * ({@code .glyphicon-duplicate}).
 *
 * <p>Kept out of the {@code @Stepwise} {@link JobsSpec}: Spock re-includes every feature that
 * precedes an included one in a stepwise spec, which would override {@code @ExcludePro}.
 * Excluded from the Enterprise run because Enterprise renders its own workflow editor by default,
 * where duplication is a step card header menu action rather than this button.
 */
@SeleniumCoreTest
@ExcludePro
@UiModeFlag(
    featureName = "jobs-options-workflow",
    status      = UiModeStatus.PROMOTED,
    description = "Workflow tab step duplication in the OSS editor; iterates legacyUi via UI_MODES (default→Vue, legacy→KO)."
)
class JobWorkflowStepActionsSpec extends SeleniumBase {

    static final UI_MODES = UiModes.defaultAndLegacy()

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    /**
     * Checks the basic step duplication into the workflow container.
     *
     */
    def "Step duplication"(){
        given:
        def projectName = "step-duplication-test"
        JobShowPage jobShowPage = page JobShowPage
        ExecutionShowPage executionShowPage = page ExecutionShowPage

        when:
        setupProject(projectName)
        JobCreatePage jobCreatePage = go(JobCreatePage, projectName, [legacyUi: legacyUi])
        jobCreatePage.waitForElementVisible(By.id("schedJobName"))
        jobCreatePage.jobNameInput.sendKeys("test-duplication")
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        if(legacyUi) {
            jobCreatePage.addSimpleCommandStep "echo 'This is a simple job'", 0
        } else {
            jobCreatePage.addSimpleCommandStepNextUi "echo 'This is a simple job'", 0
        }
        jobCreatePage.createJobButton.click()
        jobShowPage.waitForElementVisible(jobShowPage.jobActionDropdownButton)
        jobShowPage.jobActionDropdownButton.click()
        jobShowPage.waitForElementToBeClickable(jobShowPage.editJobLink)
        jobShowPage.editJobLink.click()
        jobCreatePage.waitForElementVisible(jobCreatePage.tab(JobTab.WORKFLOW))
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.waitForNumberOfElementsToBeMoreThan(jobCreatePage.duplicateWfStepBy, 0)
        jobCreatePage.duplicateWfStepButton.click()
        jobCreatePage.waitForElementVisible(jobCreatePage.getWfStepByListPosition(1))
        jobCreatePage.updateBtn.click()
        jobShowPage.waitForElementVisible(jobShowPage.jobUuid)
        jobShowPage.runJob(true)
        executionShowPage.viewButtonOutput.click()
        def logLines = executionShowPage.logOutput.stream().map {
            it.text
        }.collect(Collectors.toList())

        then:
        logLines.size() == 2
        logLines.forEach {
            it == 'This is a simple job'
        }

        cleanup:
        deleteProject(projectName)

        where:
        [legacyUi] << UI_MODES
    }

    // Default-only: legacy parity is covered by the other workflow-step methods that iterate UI_MODES.
    def "Node steps"() {
        when: "Create a new job and add a node step"
        def jobCreatePage = go(JobCreatePage, SELENIUM_BASIC_PROJECT)
        String jobUuid = JobUtils.jobImportFile(SELENIUM_BASIC_PROJECT, '/test-files/simple-job-ref.xml', client).succeeded.first().id
        def jobShowPage = page JobShowPage
        jobCreatePage.fillBasicJob "job with node steps"
        jobCreatePage.expectNumberOfStepsToBe(1)
        then: "Duplicate a step"
        jobCreatePage.waitForElementToBeClickable jobCreatePage.duplicateWfStepButton
        jobCreatePage.duplicateWfStepButton.click()
        jobCreatePage.expectNumberOfStepsToBe(2)
        then: "Add a log filter to step"
        jobCreatePage.waitForElementToBeClickable jobCreatePage.stepDropdownTrigger(0)
        jobCreatePage.clickAddLogFilter(0)
        jobCreatePage.fillHighlightLogFilter()
        jobCreatePage.getLogFilterButtons('#logFilters').size() == 1
        then: "Add another step"
        jobCreatePage.scrollToElement(jobCreatePage.createJobButton)
        jobCreatePage.addStep(new JobReferenceStep([
                childJobUuid: jobUuid,
                stepType    : StepType.NODE
        ]))
        jobCreatePage.expectNumberOfStepsToBe(3)
        then: "Remove a step"
        jobCreatePage.waitForElementToBeClickable jobCreatePage.deleteStepBy
        jobCreatePage.removeStepByIndex(0)
        jobCreatePage.expectNumberOfStepsToBe(2)
        expect: "Save the job successfully"
        jobCreatePage.scrollToElement(jobCreatePage.createJobButton)
        jobCreatePage.createJobButton.click()
        jobShowPage.waitForElementToBeClickable jobShowPage.jobDefinitionModal
        jobShowPage.jobDefinitionModal.click()
        jobShowPage.expectNumberOfStepsToBe(2)
    }

    // Default-only: legacy parity is covered by the other workflow-step methods that iterate UI_MODES.
    def "Error handlers"() {
        when:
        def jobCreatePage = go(JobCreatePage, SELENIUM_BASIC_PROJECT)
        def jobShowPage = page JobShowPage
        jobCreatePage.fillBasicJob 'job with error handlers'
        then: "Add error handler and check that its not possible to add more error handlers to same step"
        jobCreatePage.addErrorHandler( 'exec-command',  StepType.NODE)
        assert jobCreatePage.waitForDropdownOptionAbsent(0, "add-error-handler")
        then: "Duplicate step and remove duplicated error handler"
        jobCreatePage.scrollToElement(jobCreatePage.duplicateWfStepButton)
        jobCreatePage.duplicateWfStepButton.click()
        jobCreatePage.expectNumberOfStepsToBe(2)
        assert jobCreatePage.waitForDropdownOptionAbsent(1, "add-error-handler")
        jobCreatePage.scrollToElement(jobCreatePage.workflowAlphaUiButton)
        jobCreatePage.removeErrorHandlerButton(1).click()
        assert jobCreatePage.waitForDropdownOptionPresent(1, "add-error-handler")
        expect: "Save the job successfully"
        jobCreatePage.scrollToElement(jobCreatePage.createJobButton);
        jobCreatePage.waitForElementToBeClickable jobCreatePage.createJobButton
        jobCreatePage.createJobButton.click()
        jobShowPage.waitForElementToBeClickable jobShowPage.jobDefinitionModal
        jobShowPage.jobDefinitionModal.click()
        jobShowPage.expectNumberOfStepsToBe(3) // it counts the error handler as a step due to class
    }
}
