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

    def setup(){
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
            validationMsg.contains('"Job Name" parameter cannot be blank') == true
            validationMsg.contains('Workflow must have at least one step') == true
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

}
