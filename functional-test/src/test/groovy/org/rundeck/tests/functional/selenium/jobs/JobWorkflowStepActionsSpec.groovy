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
 * Workflow step editor interactions specific to the OSS workflow editor: the per-step dropdown
 * and duplicate control (log filters, error handlers), step edit/cancel in the step modal, and
 * the workflow section label.
 *
 * <p>Kept out of the {@code @Stepwise} {@link JobsSpec}: Spock re-includes every feature that
 * precedes an included one in a stepwise spec, which would override {@code @ExcludePro}.
 * Excluded from the Enterprise run because Enterprise renders its own workflow editor by default,
 * which edits steps inline in step cards and moves step actions to the step card header; equivalent
 * Enterprise coverage lives in {@code ConditionalStepSpec}, {@code ConditionalStepSubstepsSpec} and
 * {@code WorkflowStepNumberingSpec}. Step duplication itself stays in {@link JobsSpec}, where it runs
 * against both editors.
 */
@SeleniumCoreTest
@ExcludePro
@UiModeFlag(
    featureName = "jobs-options-workflow",
    status      = UiModeStatus.PROMOTED,
    description = "OSS workflow tab step editor (per-step dropdown, modal edit/cancel); iterates legacyUi via UI_MODES where applicable."
)
class JobWorkflowStepActionsSpec extends SeleniumBase {

    static final UI_MODES = UiModes.defaultAndLegacy()

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
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

    def "job workflow"() {
        when:
        def jobCreatePage = go(JobCreatePage, SELENIUM_BASIC_PROJECT)
        jobCreatePage.tab JobTab.WORKFLOW click()
        then:
        jobCreatePage.waitForTextToBePresentBySelector(jobCreatePage.workflowContentControlLabelBy, "Workflow",60)
        expect:
        // Grails 7: Verify NextUI workflow UI is loaded by checking for add step button
        // The button is rendered by CommonUndoRedoDraggableList.vue with data-testid="add-button"
        jobCreatePage.waitForElementVisible(By.cssSelector("[data-testid='add-button']"))
    }

    def "edit existing step - change command and save"() {
        // Workflow-tab step editor — same `uiType!='legacy'` GSP gate as the parent
        // `jobs-options-workflow` feature; iterate `[legacyUi] << UI_MODES`.
        when:
            def jobCreatePage = go(JobCreatePage, SELENIUM_BASIC_PROJECT, [legacyUi: legacyUi])
            def jobShowPage = page JobShowPage
        then:
            jobCreatePage.fillBasicJob "edit step regression ${legacyUi ? 'legacy' : 'default'}"
            jobCreatePage.expectNumberOfStepsToBe(1)
        when: "edit step and change command"
            jobCreatePage.clickStepToEdit(0)
            jobCreatePage.waitForElementVisible (legacyUi ? jobCreatePage.adhocRemoteStringBy : JobCreatePage.NextUi.adhocRemoteStringBy)
            jobCreatePage.adhocRemoteStringField.clear()
            jobCreatePage.adhocRemoteStringField.sendKeys 'echo updated command'
            if (legacyUi) {
                jobCreatePage.saveStep(0)
            } else {
                jobCreatePage.workflowSaveStepButton.click()
            }
        then:
            jobCreatePage.createJobButton.click()
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.expectNumberOfStepsToBe(1)
        expect:
            jobShowPage.els(jobShowPage.stepsInJobDefinitionBy).any { it.text.contains('echo updated command') }
        where:
            [legacyUi] << UI_MODES
    }

    def "cancel editing new step - step not added"() {
        // Workflow-tab step editor — covered by class-level `jobs-options-workflow` PROMOTED.
        when:
            def jobCreatePage = go(JobCreatePage, SELENIUM_BASIC_PROJECT, [legacyUi: legacyUi])
        then:
            jobCreatePage.jobNameInput.sendKeys "cancel new step ${legacyUi ? 'legacy' : 'default'}"
            jobCreatePage.tab(JobTab.WORKFLOW).click()
        when: "add step, configure, then cancel"
            if (legacyUi) {
                jobCreatePage.executeScript "window.location.hash = '#addnodestep'"
                jobCreatePage.stepLink('exec-command', StepType.NODE).click()
                jobCreatePage.byAndWaitClickable jobCreatePage.adhocRemoteStringBy
                jobCreatePage.adhocRemoteStringField.sendKeys 'echo should not appear'
                jobCreatePage.byAndWaitClickable(jobCreatePage.cancelNewStepFormBy).click()
            } else {
                jobCreatePage.clickAddStep()
                jobCreatePage.byAndWaitClickable(By.xpath("//*[@${StepType.NODE.getStepType()}='exec-command']"))
                jobCreatePage.stepLink('exec-command', StepType.NODE).click()
                jobCreatePage.byAndWaitClickable JobCreatePage.NextUi.adhocRemoteStringBy
                jobCreatePage.adhocRemoteStringField.sendKeys 'echo should not appear'
                jobCreatePage.clickCancelStepEdit()
            }
        then:
            jobCreatePage.waitForNumberOfElementsToBe(legacyUi ? jobCreatePage.numberOfStepsBy : JobCreatePage.NextUi.numberOfStepsBy, 0)
        expect:
            jobCreatePage.workFlowList.size() == 0
        where:
            [legacyUi] << UI_MODES
    }

    def "cancel editing existing step - changes discarded"() {
        // Workflow-tab step editor — covered by class-level `jobs-options-workflow` PROMOTED.
        when:
            def jobCreatePage = go(JobCreatePage, SELENIUM_BASIC_PROJECT, [legacyUi: legacyUi])
            def jobShowPage = page JobShowPage
        then:
            jobCreatePage.fillBasicJob "cancel edit step ${legacyUi ? 'legacy' : 'default'}"
            jobCreatePage.expectNumberOfStepsToBe(1)
        when: "edit step, change command, cancel"
            jobCreatePage.clickStepToEdit(0)
            jobCreatePage.waitForElementVisible (legacyUi ? jobCreatePage.adhocRemoteStringBy : JobCreatePage.NextUi.adhocRemoteStringBy)
            jobCreatePage.adhocRemoteStringField.clear()
            jobCreatePage.adhocRemoteStringField.sendKeys 'echo discarded change'
            jobCreatePage.clickCancelStepEdit()
        then:
            jobCreatePage.createJobButton.click()
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.expectNumberOfStepsToBe(1)
        expect:
            jobShowPage.els(jobShowPage.stepsInJobDefinitionBy).any { it.text.contains('echo selenium test') }
        where:
            [legacyUi] << UI_MODES
    }
}
