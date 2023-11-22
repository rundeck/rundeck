package org.rundeck.tests.functional.selenium.tests.jobs

import org.rundeck.tests.functional.selenium.pages.jobs.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.jobs.JobListPage
import org.rundeck.tests.functional.selenium.pages.home.HomePage
import org.rundeck.tests.functional.selenium.pages.jobs.JobShowPage
import org.rundeck.tests.functional.selenium.pages.jobs.JobTab
import org.rundeck.tests.functional.selenium.pages.jobs.NotificationEvent
import org.rundeck.tests.functional.selenium.pages.jobs.NotificationType
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
class BasicJobsSpec extends SeleniumBase {

    def setupSpec(){
        setupProject("SeleniumBasic", "/projects-import/SeleniumBasic.zip")
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def "create job has basic fields"() {
        when:
            def jobCreatePage = go JobCreatePage, "/project/SeleniumBasic"
        then:
            jobCreatePage.validatePage()
            jobCreatePage.jobNameInput
            jobCreatePage.groupPathInput
            jobCreatePage.descriptionTextarea
    }

    def "create job invalid empty name"() {
        when:
            def jobCreatePage = go JobCreatePage, "/project/SeleniumBasic"
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
            def jobCreatePage = go JobCreatePage, "/project/SeleniumBasic"
        then:
            jobCreatePage.validatePage()
            jobCreatePage.jobNameInput.sendKeys('a job')
            jobCreatePage.createJobButton.click()
        expect:
            jobCreatePage.errorAlert.getText().contains('Error saving Job')
            def validationMsg = jobCreatePage.formValidationAlert.getText()
            !validationMsg.contains('"Job Name" parameter cannot be blank')
            validationMsg.contains('Workflow must have at least one step')
    }

    def "create valid job basic workflow"() {
        when:
            def jobCreatePage = go JobCreatePage, "/project/SeleniumBasic"
        then:
            jobCreatePage.validatePage()
            jobCreatePage.jobNameInput.sendKeys('a job')
            jobCreatePage.addNewWfStepCommand('echo selenium test')
            jobCreatePage.createJobButton.click()
            jobCreatePage.waitForUrlToContain('/job/show')
            def jobShowPage = page JobShowPage
            jobShowPage.jobLinkTitleLabel.getText() == 'a job'
    }

    def "create valid job basic options"() {
        when:
            def jobCreatePage = go JobCreatePage, "/project/SeleniumBasic"
        then:
            jobCreatePage.validatePage()
            jobCreatePage.jobNameInput.sendKeys('a job with options')
            jobCreatePage.addNewWfStepCommand('echo selenium test')

            jobCreatePage.optionButton.click()
            def optionName = 'seleniumOption1'
            jobCreatePage.optionName 0 sendKeys optionName
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.createJobButton.click()

        then:
            jobCreatePage.waitForUrlToContain('/job/show')
            def jobShowPage = page JobShowPage
            jobShowPage.jobLinkTitleLabel.getText().contains('a job with options')
            jobShowPage.optionInputText(optionName) != null
    }

    def "edit job set description"() {
        when:
            def jobCreatePage = go JobCreatePage, "SeleniumBasic##b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
        then:
            jobCreatePage.descriptionTextarea.clear()
            jobCreatePage.descriptionTextarea.sendKeys 'a new job description'
            jobCreatePage.updateJobButton.click()
        expect:
            def jobShowPage = page JobShowPage
            'a new job description' == jobShowPage.descriptionTextLabel.getText()
    }

    def "edit job set groups"() {
        when:
            def jobCreatePage = go JobCreatePage, "SeleniumBasic##b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
        then:
            jobCreatePage.jobGroupField.clear()
            jobCreatePage.jobGroupField.sendKeys 'testGroup'
            jobCreatePage.updateJobButton.click()
        expect:
            def jobShowPage = page JobShowPage
            'testGroup' == jobShowPage.jobInfoGroupLabel.getText()
    }

    def "edit job and set schedules tab"() {
        when:
            def jobCreatePage = go JobCreatePage, "SeleniumBasic##b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
        then:
            jobCreatePage.tab JobTab.SCHEDULE click()
            jobCreatePage.scheduleRunYesField.click()
            if (!jobCreatePage.scheduleEveryDayCheckboxField.isSelected()) {
                jobCreatePage.scheduleEveryDayCheckboxField.click()
            }
            jobCreatePage.scheduleDaysCheckboxDivField.isDisplayed()
            jobCreatePage.updateJobButton.click()
    }

    def "edit job and set other tab"() {
        when:
            def jobCreatePage = go JobCreatePage, "SeleniumBasic##b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
        then:
            jobCreatePage.tab JobTab.OTHER click()
            if (jobCreatePage.multiExecFalseField.isSelected()) {
                jobCreatePage.multiExecTrueField.click()
                jobCreatePage.multiExecTrueField.isSelected()
            } else {
                jobCreatePage.multiExecFalseField.click()
                jobCreatePage.multiExecFalseField.isSelected()
            }
            jobCreatePage.updateJobButton.click()
    }

    def "edit job and set notifications"() {
        when:
            def jobCreatePage = go JobCreatePage, "SeleniumBasic##b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
        then:
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
            homePage.goProjectHome"SeleniumBasic"
        when:
            def jobListPage = page JobListPage
            jobListPage.loadPathToShowJob "SeleniumBasic", "b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
            jobListPage.go()
        then:
            def jobShowPage = page JobShowPage
            jobShowPage.jobDefinitionModal.click()
        expect:
            jobShowPage.cronLabel.size() == 2
            jobShowPage.scheduleTimeLabel.isDisplayed()
            jobShowPage.multipleExecField.isDisplayed()
            jobShowPage.multipleExecYesField.getText() == 'Yes'
            jobShowPage.notificationDefinition.getText() == 'mail to: test@rundeck.com'
            jobShowPage.closeJobDefinitionModalButton.click()
    }

    def "run job modal should show validation error"() {
        when:
            def jobShowPage = go JobShowPage, "SeleniumBasic"
        then:
            jobShowPage.validatePage()
            jobShowPage.runJobLink '0088e04a-0db3-4b03-adda-02e8a4baf709' click()
            sleep 3000
            jobShowPage.runFormButton.click()
            sleep 3000
            jobShowPage.optionValidationWarningText.getText().contains 'Option \'reqOpt1\' is required'
    }

    def "job filter by name 3 results"() {
        when:
            def jobShowPage = go JobShowPage, "SeleniumBasic"
        then:
            jobShowPage.validatePage()
            jobShowPage.jobRowLink.size() == 3
            jobShowPage.jobRowLink.collect {
                it.getText()
            } == ['selenium-option-test1', 'a job with options', 'predefined job with options']
    }

    def "job filter by name and group 1 results"() {
        when:
            def jobShowPage = go JobShowPage, "SeleniumBasic"
        then:
            jobShowPage.validatePage()
            jobShowPage.jobSearchButton.click()
            jobShowPage.waitForModal 1
            jobShowPage.jobSearchNameField.sendKeys 'option'
            jobShowPage.jobSearchGroupField.sendKeys 'test'
            jobShowPage.jobSearchSubmitButton.click()
        expect:
            jobShowPage.jobRowLink.size() == 1
            jobShowPage.jobRowLink.collect { it.getText() } == ['selenium-option-test1']
    }

    def "job filter by name and - top group 2 results"() {
        when:
            def jobShowPage = go JobShowPage, "SeleniumBasic"
        then:
            jobShowPage.validatePage()
            jobShowPage.jobSearchButton.click()
            jobShowPage.waitForModal 1
            jobShowPage.jobSearchNameField.sendKeys 'option'
            jobShowPage.jobSearchGroupField.sendKeys '-'
            jobShowPage.jobSearchSubmitButton.click()
        expect:
            jobShowPage.jobRowLink.size() == 2
            jobShowPage.jobRowLink.collect { it.getText() } == ['a job with options', 'predefined job with options']
    }

}
