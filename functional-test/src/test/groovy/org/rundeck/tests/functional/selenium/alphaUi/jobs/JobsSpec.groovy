package org.rundeck.tests.functional.selenium.alphaUi.jobs

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select
import org.rundeck.util.annotations.AlphaUiSeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobReferenceStep
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.StepType
import org.rundeck.util.gui.pages.login.LoginPage

@AlphaUiSeleniumCoreTest
class JobsSpec extends SeleniumBase {
    String projectName

    def setup() {
        projectName = UUID.randomUUID().toString()
        setupProject(projectName)
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def cleanup() {
        deleteProject(projectName)
    }

    /**
     * Checks that new workflow tab is active
     */
    def "job workflow alphaUi"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        then:
        jobCreatePage.tab JobTab.WORKFLOW click()
        jobCreatePage.waitForTextToBePresentBySelector(By.xpath("//section[@id='workflowContent']//div[contains(@class, 'control-label')]"), "Workflow",60)
        expect:
        jobCreatePage.workflowAlphaUiContainer.isDisplayed()
    }

    def "Create option form next ui"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        def optName = 'test'
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" next ui"
        jobCreatePage.optionButton.click()

        then: "create form is shown"
        jobCreatePage.optionNameNew() displayed
        jobCreatePage.saveOptionButton.displayed
    }
    def "Duplicate option create form next ui"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        def optName = 'test'
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" next ui"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys optName
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.executeScript "window.location.hash = '#workflowKeepGoingFail'"
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 0

        jobCreatePage.duplicateButton( optName, 0) click()


        then: "create form is shown"
        jobCreatePage.optionNameNew() displayed
        jobCreatePage.optionNameNew().getAttribute('value') == optName + '_copy'
        jobCreatePage.saveOptionButton.displayed
        when:
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 1

        then:
        jobCreatePage.optionNameSaved 0 getText() equals optName
        jobCreatePage.optionNameSaved 1 getText() equals optName + '_copy'

    }
    def "create valid job basic options"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        def jobShowPage = page JobShowPage
        def optionName = 'seleniumOption1'
        then:
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" next ui"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys optionName
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 0
        jobCreatePage.createJobButton.click()
        then:
        jobCreatePage.waitForUrlToContain('/job/show')
        jobShowPage.jobLinkTitleLabel.getText().contains('create valid job basic options')
        jobShowPage.optionInputText(optionName) != null
    }

    def "job options config - check usage session"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        then:
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" next ui"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.sessionSectionLabel
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 0
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
        jobCreatePage.createJobButton.click()
    }
    def "job options config - check storage session"() {
        given:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        when:
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" next ui"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.secureInputTypeRadio.click()
        jobCreatePage.optionOpenKeyStorageButton.click()
        jobCreatePage.optionCloseKeyStorageButton.click()
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 0
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
        jobCreatePage.createJobButton.click()
        then:
        jobCreatePage.waitForOptionsToBe 1, 0
        jobCreatePage.optionLis 0 isEmpty()
    }
    def "job option simple redo"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        then:
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" next ui"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 0
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew(1) sendKeys 'seleniumOption2'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 1
        jobCreatePage.waitForElementAttributeToChange jobCreatePage.optionUndoButton, 'disabled', null
        jobCreatePage.optionUndoButton.click()
        expect:
        jobCreatePage.waitForOptionsToBe 1, 0
        jobCreatePage.optionLis 1 isEmpty()
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
        jobCreatePage.createJobButton.click()
    }
    def "No default value field shown in secure job option section"() {
        given:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        when:
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" next ui"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.secureInputTypeRadio.click()
        jobCreatePage.storagePathInput.sendKeys("test")
        jobCreatePage.secureInputTypeRadio.click()
        jobCreatePage.storagePathInput.clear()
        jobCreatePage.secureInputTypeRadio.click()

        then:
        driver.findElements(jobCreatePage.defaultValueBy).isEmpty() || !jobCreatePage.defaultValueInput.isDisplayed()
    }
    def "job option revert all"() {
        given:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" next ui"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 0
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew(1) sendKeys 'seleniumOption2'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 1
        jobCreatePage.executeScript "window.location.hash = '#optundoredo'"
        jobCreatePage.waitForElementAttributeToChange jobCreatePage.optionUndoButton, 'disabled', null
        jobCreatePage.optionUndoButton
        when:
        jobCreatePage.optionRevertAllButton.click()
        then:
        jobCreatePage.waitForOptionsToBe 0, 0
        jobCreatePage.waitForOptionsToBe 1, 0
        jobCreatePage.optionLis 0 isEmpty()
        jobCreatePage.optionLis 1 isEmpty()
    }
    def "job option undo redo"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        then:
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 0
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew(1) sendKeys 'seleniumOption2'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 1
        jobCreatePage.executeScript "window.location.hash = '#optundoredo'"
        jobCreatePage.waitForElementAttributeToChange jobCreatePage.optionUndoButton, 'disabled', null
        jobCreatePage.optionUndoButton.click()
        jobCreatePage.waitForElementToBeClickable jobCreatePage.optionRedoButton
        sleep 1000
        jobCreatePage.optionRedoButton.click()
        expect:
        !(jobCreatePage.optionLis 0 isEmpty())
        !(jobCreatePage.optionLis 1 isEmpty())
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
        jobCreatePage.createJobButton.click()
        where:
        nextUi << [true]
    }

    def "change workflow strategy"() {
        when:
        def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        def jobShowPage = page JobShowPage
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        then:
        jobCreatePage.fillBasicJob 'parallel workflow strategy'
        jobCreatePage.waitForElementVisible jobCreatePage.workFlowStrategyField

        def select = new Select(jobCreatePage.workFlowStrategyField)
        select.selectByValue('parallel')
        jobCreatePage.waitForElementVisible jobCreatePage.strategyPluginParallelField
        jobCreatePage.strategyPluginParallelMsgField.getText() == 'Run all steps in parallel'
        jobCreatePage.createJobButton.click()
        expect:
        jobShowPage.jobDefinitionModal.click()
        jobShowPage.workflowDetailField.getText() == 'Parallel Run all steps in parallel'
    }

    def "add global log filters"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=true
            jobCreatePage.go()
            then:
            jobCreatePage.fillBasicJob 'job with global log filter'
            jobCreatePage.addGlobalLogFilter.click()
            jobCreatePage.fillHighlightLogFilter();
        expect:
            jobCreatePage.getLogFilterButtons('#globalLogFilters').size() == 1
    }

    def "Node steps"() {
        when: "Create a new job and add a node step"
            String jobUuid = JobUtils.jobImportFile(SELENIUM_BASIC_PROJECT, '/test-files/simple-job-ref.xml', client).succeeded.first().id
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
            def jobShowPage = page JobShowPage
            jobCreatePage.nextUi = true
            jobCreatePage.go()
            jobCreatePage.fillBasicJob 'job with node steps'
            jobCreatePage.expectNumberOfStepsToBe(1)
        then: "Duplicate a step"
            jobCreatePage.waitForElementToBeClickable jobCreatePage.duplicateWfStepButton;
            jobCreatePage.duplicateWfStepButton.click()
            jobCreatePage.expectNumberOfStepsToBe(2)
        then: "Add a log filter to step"
            jobCreatePage.waitForElementToBeClickable jobCreatePage.stepDropdownTrigger(0);
            jobCreatePage.clickAddLogFilter(0);
            jobCreatePage.fillHighlightLogFilter();
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
            jobCreatePage.scrollToElement(jobCreatePage.createJobButton);
            jobCreatePage.createJobButton.click()
            jobShowPage.waitForElementToBeClickable jobShowPage.jobDefinitionModal
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.expectNumberOfStepsToBe(2)
    }

    def "Error handlers"() {
        when:
        def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        def jobShowPage = page JobShowPage
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        jobCreatePage.fillBasicJob 'job with error handlers'
        then: "Add error handler and check that its not possible to add more error handlers to same step"
            jobCreatePage.addErrorHandler( 'exec-command',  StepType.NODE)
            assert jobCreatePage.doesntHasDropdownOption(0, "add-error-handler")
        then: "Duplicate step and remove duplicated error handler"
            jobCreatePage.scrollToElement(jobCreatePage.duplicateWfStepButton)
            jobCreatePage.duplicateWfStepButton.click()
            jobCreatePage.expectNumberOfStepsToBe(2)
            assert jobCreatePage.doesntHasDropdownOption(1, "add-error-handler")
            jobCreatePage.scrollToElement(jobCreatePage.workflowAlphaUiButton)
            jobCreatePage.removeErrorHandlerButton(1).click()
            assert !jobCreatePage.doesntHasDropdownOption(1, "add-error-handler")
        expect: "Save the job successfully"
            jobCreatePage.scrollToElement(jobCreatePage.createJobButton);
            jobCreatePage.waitForElementToBeClickable jobCreatePage.createJobButton
            jobCreatePage.createJobButton.click()
            jobShowPage.waitForElementToBeClickable jobShowPage.jobDefinitionModal
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.expectNumberOfStepsToBe(3) // it counts the error handler as a step due to class
    }
}
