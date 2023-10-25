package org.rundeck.tests.functional.selenium

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.tests.functional.selenium.pages.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.JobShowPage
import org.rundeck.tests.functional.selenium.pages.JobTab
import org.rundeck.tests.functional.selenium.pages.JobsListPage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.NotificationEvent
import org.rundeck.tests.functional.selenium.pages.NotificationType
import org.rundeck.tests.functional.selenium.pages.ProjectCreatePage
import org.rundeck.tests.functional.selenium.pages.ProjectListPage
import org.rundeck.tests.functional.selenium.pages.SideBar
import org.rundeck.tests.functional.selenium.pages.SideBarNavLinks
import org.rundeck.tests.functional.selenium.pages.StepName
import org.rundeck.tests.functional.selenium.pages.StepType
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import spock.lang.Shared
import spock.lang.Unroll

import java.time.Duration

@SeleniumCoreTest
class JobNotificationSpec extends SeleniumBase {

    @Shared
    LoginPage loginPage = page LoginPage
    @Shared
    ProjectCreatePage projectCreatePage = page ProjectCreatePage
    @Shared
    SideBar sideBar = page SideBar
    @Shared
    JobsListPage jobListPage = page JobsListPage
    @Shared
    JobCreatePage jobCreatePage = page JobCreatePage
    @Shared
    JobShowPage jobShowPage = page JobShowPage

    def cleanupSpec() {
        driver.quit()
    }

    @Unroll
    def "create job with notifications"() {
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
        // add wait for this jobCreatePage.getNotificationModal()
        jobCreatePage.getNotificationDropDown().click()
        jobCreatePage.getNotificationByType(NotificationType.MAIL).click()
        jobCreatePage.getNotificationConfigByPropName("recipients").sendKeys('test@rundeck.com')
        jobCreatePage.getNotificationSaveButton().click()
        jobCreatePage.getCreateButton().click()
        jobCreatePage.waitForJobShow()
        jobCreatePage.getJobDefinitionModal().click()

        then:
        jobCreatePage.getNotificationDefinition().getText() == 'mail to: test@rundeck.com'

    }

}
