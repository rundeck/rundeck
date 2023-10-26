package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.JobListPage
import org.rundeck.tests.functional.selenium.pages.JobShowPage
import org.rundeck.tests.functional.selenium.pages.JobTab

import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.NotificationEvent
import org.rundeck.tests.functional.selenium.pages.NotificationType
import org.rundeck.tests.functional.selenium.pages.ProjectHomePage
import org.rundeck.tests.functional.selenium.pages.SideBarPage
import org.rundeck.tests.functional.selenium.pages.StepName
import org.rundeck.tests.functional.selenium.pages.StepType
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.setup.NavLinkTypes
import spock.lang.Unroll


@SeleniumCoreTest
class JobNotificationSpec extends SeleniumBase {

    def setupSpec(){
        setupProject("SeleniumBasic", "/projects-import/SeleniumBasic.zip")
    }

    @Unroll
    def "create job with notifications"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def projectHomePage = page ProjectHomePage
            projectHomePage.goProjectHome"SeleniumBasic"
            def sideBarPage = page SideBarPage
            sideBarPage.goTo NavLinkTypes.JOBS
            def jobListPage = page JobListPage
            jobListPage.newJobButton.click()
            def jobCreatePage = page JobCreatePage
            jobCreatePage.jobNameField.sendKeys "a job with notifications"
            jobCreatePage.tab JobTab.WORKFLOW click()
            jobCreatePage.selectStep StepName.COMMAND, StepType.NODE
            jobCreatePage.waitForNumberOfElementsToBe jobCreatePage.commandBy
            jobCreatePage.commandField.sendKeys "echo 'example job'"
            jobCreatePage.saveStep 0
            jobCreatePage.tab JobTab.NOTIFICATIONS click()
            jobCreatePage.addNotificationButtonByType NotificationEvent.START click()
            jobCreatePage.notificationDropDown.click()
            jobCreatePage.notificationByType NotificationType.MAIL click()
            jobCreatePage.notificationConfigByPropName "recipients" sendKeys 'test@rundeck.com'
            jobCreatePage.notificationSaveButton.click()
            jobCreatePage.waitNotificationModal 0
            jobCreatePage.createButton.click()
            def jobShowPage = page JobShowPage
            jobShowPage.jobDefinitionModal.click()

        then:
            jobShowPage.notificationDefinition.getText() == 'mail to: test@rundeck.com'
            jobShowPage.closeJobDefinitionModalButton.click()

        cleanup:
            sideBarPage.deleteProject()
            sideBarPage.waitForModal 1
    }

}
