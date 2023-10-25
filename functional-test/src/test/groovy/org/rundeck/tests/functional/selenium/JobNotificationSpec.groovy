package org.rundeck.tests.functional.selenium

import org.openqa.selenium.By
import org.rundeck.tests.functional.selenium.pages.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.JobTab
import org.rundeck.tests.functional.selenium.pages.JobsListPage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.NotificationEvent
import org.rundeck.tests.functional.selenium.pages.NotificationType
import org.rundeck.tests.functional.selenium.pages.ProjectCreatePage
import org.rundeck.tests.functional.selenium.pages.SideBar
import org.rundeck.tests.functional.selenium.pages.SideBarNavLinks
import org.rundeck.tests.functional.selenium.pages.StepName
import org.rundeck.tests.functional.selenium.pages.StepType
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import spock.lang.Unroll


@SeleniumCoreTest
class JobNotificationSpec extends SeleniumBase {

    @Unroll
    def "create job with notifications"() {
        setup:
        LoginPage loginPage = page LoginPage
        ProjectCreatePage projectCreatePage = page ProjectCreatePage
        SideBar sideBar = page SideBar
        JobsListPage jobListPage = page JobsListPage
        JobCreatePage jobCreatePage = page JobCreatePage

        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        projectCreatePage.getCreateNewProjectButton().click()
        projectCreatePage.getProjectNameField().sendKeys("SeleniumBasic")
        projectCreatePage.getCreateProjectButton().click()
        sideBar.goTo(SideBarNavLinks.JOBS).click()
        jobListPage.getCreateJobButton().click()
        jobCreatePage.getJobNameField().sendKeys("a job with notifications")
        jobCreatePage.getTab(JobTab.WORKFLOW).click()
        jobCreatePage.getStepByType(StepName.COMMAND, StepType.NODE).click()
        jobCreatePage.waitForStepToBeShown(By.id("adhocRemoteStringField"))
        jobCreatePage.el(By.id("adhocRemoteStringField")).sendKeys("echo 'example job'")
        jobCreatePage.getSaveStepButton().click()
        jobCreatePage.waitForSavedStep(0)
        jobCreatePage.getTab(JobTab.NOTIFICATIONS).click()
        jobCreatePage.getAddNotificationButtonByType(NotificationEvent.START).click()
        jobCreatePage.getNotificationDropDown().click()
        jobCreatePage.getNotificationByType(NotificationType.MAIL).click()
        jobCreatePage.getNotificationConfigByPropName("recipients").sendKeys('test@rundeck.com')
        jobCreatePage.getNotificationSaveButton().click()
        jobCreatePage.waitNotificationModal(0)
        jobCreatePage.getCreateButton().click()
        jobCreatePage.waitForJobShow()
        jobCreatePage.getJobDefinitionModal().click()

        then:
        jobCreatePage.getNotificationDefinition().getText() == 'mail to: test@rundeck.com'

    }

}
