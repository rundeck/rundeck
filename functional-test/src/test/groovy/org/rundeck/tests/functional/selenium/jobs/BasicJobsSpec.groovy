package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.jobs.NotificationEvent
import org.rundeck.util.gui.pages.jobs.NotificationType
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
class BasicJobsSpec extends SeleniumBase {

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def "create job has basic fields"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.validatePage()
            jobCreatePage.jobNameInput
            jobCreatePage.groupPathInput
            jobCreatePage.descriptionTextarea
    }

    def "create job invalid empty name"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.validatePage()
            jobCreatePage.jobNameInput.clear()
            jobCreatePage.createJobButton.click()
            jobCreatePage.errorAlert.getText().contains('Error saving Job')
            def validationMsg = jobCreatePage.formValidationAlert.getText()
            validationMsg.contains('"Job Name" parameter cannot be blank')
            validationMsg.contains('Workflow must have at least one step')
    }

    def "create job invalid empty workflow"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.validatePage()
            jobCreatePage.jobNameInput.sendKeys('a job with empty workflow')
            jobCreatePage.createJobButton.click()
        expect:
            jobCreatePage.errorAlert.getText().contains('Error saving Job')
            def validationMsg = jobCreatePage.formValidationAlert.getText()
            !validationMsg.contains('"Job Name" parameter cannot be blank')
            validationMsg.contains('Workflow must have at least one step')
        
    }

    def "create valid job basic workflow"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
            def jobShowPage = page JobShowPage
        then:
            jobCreatePage.fillBasicJob 'a valid job with basic workflow'
            jobCreatePage.createJobButton.click()
        expect:
            jobShowPage.validatePage()
            jobShowPage.jobLinkTitleLabel.getText() == 'a valid job with basic workflow'
    }

    def "create valid job basic options"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=nextUi
            jobCreatePage.go()
            def jobShowPage = page JobShowPage
            def optionName = 'seleniumOption1'
        then:
            jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
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
        where:
            nextUi<<[false,true]
    }

    def "edit job set description"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=nextUi
            jobCreatePage.go()
            def jobShowPage = page JobShowPage
            jobShowPage.nextUi=nextUi
        then:
            jobCreatePage.loadEditPath SELENIUM_BASIC_PROJECT, "b7b68386-3a52-46dc-a28b-1a4bf6ed87de", nextUi
            jobCreatePage.go()
            jobCreatePage.descriptionTextarea.sendKeys 'a new job description'
            jobCreatePage.updateJobButton.click()
        expect:
            'a new job description' == jobShowPage.descriptionTextLabel.getText()
        where:
            nextUi<<[false,true]
    }

    def "edit job set groups"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=nextUi
            jobCreatePage.go()
        then:
            jobCreatePage.loadEditPath SELENIUM_BASIC_PROJECT, "b7b68386-3a52-46dc-a28b-1a4bf6ed87de", nextUi
            jobCreatePage.go()
            jobCreatePage.jobGroupField.clear()
            jobCreatePage.jobGroupField.sendKeys 'testGroup'
        where:
            nextUi<<[false,true]
    }

    def "edit job set group via modal"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=nextUi
            jobCreatePage.go()
        then:
            jobCreatePage.loadEditPath SELENIUM_BASIC_PROJECT, "b7b68386-3a52-46dc-a28b-1a4bf6ed87de", nextUi
            jobCreatePage.go()
            jobCreatePage.groupChooseButton.click()
            jobCreatePage.waitForElementToBeClickable jobCreatePage.groupNameOption
            jobCreatePage.groupNameOption.click()
        expect:
            'test' == jobCreatePage.jobGroupField.getAttribute("value")
        where:
            nextUi<<[false, true]
    }

    def "edit job and set schedules tab"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.loadEditPath SELENIUM_BASIC_PROJECT, "b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
            jobCreatePage.go()
            jobCreatePage.tab JobTab.SCHEDULE click()
            jobCreatePage.scheduleRunYesField.click()
            if (!jobCreatePage.scheduleEveryDayCheckboxField.isSelected()) {
                jobCreatePage.scheduleEveryDayCheckboxField.click()
            }
            jobCreatePage.scheduleDaysCheckboxDivField.isDisplayed()
            jobCreatePage.updateJobButton.click()
    }

    def "edit job and set executions tab"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.loadEditPath SELENIUM_BASIC_PROJECT, "b7b68386-3a52-46dc-a28b-1a4bf6ed87de", nextUi
            jobCreatePage.go()
            jobCreatePage.tab JobTab.EXECUTION_PLUGINS click()
            if(jobCreatePage.executionPluginsRows.size() > 1){
                jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.killHandlerPluginPreviousRow
            }
            if (jobCreatePage.killHandlerPluginCheckbox.isSelected()) {
                jobCreatePage.killHandlerPluginCheckbox.click()
                jobCreatePage.killHandlerPluginKillSpawnedCheckbox.click()
            } else {
                jobCreatePage.killHandlerPluginCheckbox.click()
                jobCreatePage.killHandlerPluginCheckbox.isSelected()
                jobCreatePage.killHandlerPluginKillSpawnedCheckbox.click()
                jobCreatePage.killHandlerPluginKillSpawnedCheckbox.isSelected()
            }
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.updateJobButton
            jobCreatePage.updateJobButton.click()
        where:
            nextUi<<[false,true]
    }

    def "edit job and set other tab"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.loadEditPath SELENIUM_BASIC_PROJECT, "b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
            jobCreatePage.go()
            jobCreatePage.tab JobTab.OTHER click()
            if (jobCreatePage.multiExecFalseField.isSelected()) {
                jobCreatePage.multiExecTrueField.click()
                jobCreatePage.multiExecTrueField.isSelected()
            } else {
                jobCreatePage.multiExecFalseField.click()
                jobCreatePage.multiExecFalseField.isSelected()
            }
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.updateJobButton
            jobCreatePage.updateJobButton.click()
    }

    def "edit job and set notifications"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.loadEditPath SELENIUM_BASIC_PROJECT, "b7b68386-3a52-46dc-a28b-1a4bf6ed87de", false
            jobCreatePage.go()
            jobCreatePage.tab JobTab.NOTIFICATIONS click()
            jobCreatePage.addNotificationButtonByType NotificationEvent.START click()
            jobCreatePage.notificationDropDown.click()
            jobCreatePage.notificationByType NotificationType.MAIL click()
            jobCreatePage.notificationConfigByPropName "recipients" sendKeys 'test@rundeck.com'
            jobCreatePage.notificationSaveButton.click()
            jobCreatePage.waitNotificationModal 0
            jobCreatePage.updateJobButton.click()
    }

    def "showing the edited job"() {
        setup:
            def homePage = page HomePage
            def jobListPage = page JobListPage
            def jobShowPage = page JobShowPage
        when:
            homePage.goProjectHome"SeleniumBasic"
            jobListPage.loadPathToShowJob SELENIUM_BASIC_PROJECT, "b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
        then:
            jobListPage.go()
            jobShowPage.jobDefinitionModal.click()
        expect:
            jobShowPage.cronLabel.size() == 2
            jobShowPage.scheduleTimeLabel.isDisplayed()
            jobShowPage.multipleExecField.isDisplayed()
            jobShowPage.multipleExecYesField.getText() == 'Yes'
            jobShowPage.notificationDefinition.getText() == 'mail to: test@rundeck.com'
            jobShowPage.closeJobDefinitionModalButton.click()
    }

    @ExcludePro
    def "run job modal should show validation error"() {
        when:
            def jobShowPage = go JobShowPage, SELENIUM_BASIC_PROJECT
        then:
            jobShowPage.validatePage()
            jobShowPage.runJobLink '0088e04a-0db3-4b03-adda-02e8a4baf709' click()
            jobShowPage.waitForElementToBeClickable jobShowPage.runFormButton
            jobShowPage.runFormButton.click()
            jobShowPage.waitForElementVisible jobShowPage.optionValidationWarningText
        expect:
            jobShowPage.optionValidationWarningText.getText().contains 'Option \'reqOpt1\' is required'
    }

    def "run job modal should show node filter editable input"(){
        when:
            def jobShowPage = go JobShowPage, SELENIUM_BASIC_PROJECT
        then:
            jobShowPage.validatePage()
            jobShowPage.runJobLink '7a0d71b2-e096-4fbd-9efb-21bcbe826c0e' click()
            jobShowPage.waitForElementToBeClickable jobShowPage.nodeFilterInput
            jobShowPage.nodeFilterInput.click()
            jobShowPage.waitForElementToBeClickable jobShowPage.nodeFilterOverride
            jobShowPage.nodeFilterOverride.click()
        expect:
            jobShowPage.schedJobNodeFilter.isDisplayed()
    }

    def "job filter by name results"() {
        when:
            def jobShowPage = go JobShowPage, SELENIUM_BASIC_PROJECT
        then:
            jobShowPage.validatePage()
            jobShowPage.jobSearchButton.click()
            jobShowPage.waitForModal 1
            jobShowPage.jobSearchNameField.sendKeys 'option'
            jobShowPage.jobSearchSubmitButton.click()
            jobShowPage.waitForNumberOfElementsToBe jobShowPage.jobRowBy, expected.size()
            jobShowPage.jobRowLink.size() == expected.size()
            jobShowPage.jobRowLink.collect {
                it.getText()
            }.containsAll(expected)
        where:
            expected = [
                "selenium-option-test1",
                "predefined job with options",
                "create valid job basic options next ui",
                "create valid job basic options old ui"
            ]
    }

    def "job filter by name and group 1 results"() {
        when:
            def jobShowPage = go JobShowPage, SELENIUM_BASIC_PROJECT
        then:
            jobShowPage.validatePage()
            jobShowPage.jobSearchButton.click()
            jobShowPage.waitForModal 1
            jobShowPage.jobSearchNameField.sendKeys 'option'
            jobShowPage.jobSearchGroupField.sendKeys 'test'
            jobShowPage.jobSearchSubmitButton.click()
        expect:
            jobShowPage.waitForNumberOfElementsToBeOne jobShowPage.jobRowBy
            jobShowPage.jobRowLink.collect { it.getText() } == ["selenium-option-test1"]
    }

    def "job filter by name and - top group 2 results"() {
        given:
            def jobShowPage = go JobShowPage, SELENIUM_BASIC_PROJECT
        when:
            jobShowPage.validatePage()
            jobShowPage.jobSearchButton.click()
            jobShowPage.waitForModal 1
            jobShowPage.jobSearchNameField.sendKeys 'option'
            jobShowPage.jobSearchGroupField.sendKeys '-'
            jobShowPage.jobSearchSubmitButton.click()
        then:
            jobShowPage.waitForNumberOfElementsToBe jobShowPage.jobRowBy, expected.size()
            def names = jobShowPage.jobRowLink.collect { it.getText() }
            names.size() == expected.size()
            names.containsAll expected
        where:
            expected = [
                "predefined job with options",
                "create valid job basic options next ui",
                "create valid job basic options old ui"
            ]
    }

    def "view jobs list page"() {
        when:
            HomePage homePage = page HomePage
            homePage.validatePage()
        then:
            JobListPage jobsListPage = page JobListPage
            jobsListPage.loadPathToNextUI SELENIUM_BASIC_PROJECT
            jobsListPage.go()

            verifyAll {
                driver.currentUrl.contains('nextUi=true')
                driver.pageSource.contains('ui-type-next')
            }

            jobsListPage.bodyNextUI.isDisplayed()

            jobsListPage.createJobLink.isDisplayed()

            def actionsButton = jobsListPage.jobsActionsButton
            actionsButton.isDisplayed()
            actionsButton.getText().contains('Job Actions')
            jobsListPage.jobsHeader.isDisplayed()
            jobsListPage.activitySectionLink.isDisplayed()
            jobsListPage.activityHeader.isDisplayed()
        when:
            actionsButton.click()
        then:
            jobsListPage.getLink('Upload Definition').isDisplayed()
            jobsListPage.getLink('Bulk Edit').isDisplayed()
    }

}
