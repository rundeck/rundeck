package org.rundeck.tests.functional.selenium

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.tests.functional.selenium.pages.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.JobShowPage
import org.rundeck.tests.functional.selenium.pages.JobTab
import org.rundeck.tests.functional.selenium.pages.JobsListPage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.NotificationEvent
import org.rundeck.tests.functional.selenium.pages.NotificationType
import org.rundeck.tests.functional.selenium.pages.ProjectHomePage
import org.rundeck.tests.functional.selenium.pages.ProjectListPage
import org.rundeck.tests.functional.selenium.pages.SideBar
import org.rundeck.tests.functional.selenium.pages.SideBarNavLinks
import org.rundeck.tests.functional.selenium.pages.StepName
import org.rundeck.tests.functional.selenium.pages.StepType
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import spock.lang.Unroll

import java.time.Duration


@SeleniumCoreTest
class JobNotificationSpec extends SeleniumBase {

    def setupSpec(){
        setupProject("SeleniumBasic", "/projects-import/SeleniumBasic.zip")
    }

    @Unroll
    def "create job with notifications"() {
        setup:
        LoginPage loginPage = page LoginPage
        SideBar sideBar = page SideBar
        JobsListPage jobListPage = page JobsListPage
        JobCreatePage jobCreatePage = page JobCreatePage
        ProjectHomePage projectHomePage = page ProjectHomePage

        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        page(ProjectListPage).waitUntilPageLoaded()
        projectHomePage.goProjectHome("SeleniumBasic")
        sideBar.goTo(SideBarNavLinks.JOBS).click()
        jobListPage.getCreateJobLink().click()
        jobCreatePage.getJobNameField().sendKeys("a job with notifications")
        jobCreatePage.getTab(JobTab.WORKFLOW).click()

        def type = jobCreatePage.getStepByType(StepName.COMMAND, StepType.NODE)
        def panel = jobCreatePage.addStepButtonsPanel
        ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView(true);", panel)
        type.click()
        jobCreatePage.waitForStepToBeShown(By.id("adhocRemoteStringField"))
        jobCreatePage.el(By.id("adhocRemoteStringField")).sendKeys("echo 'example job'")
        jobCreatePage.getSaveStepButton().click()
        jobCreatePage.waitForSavedStep(0)
        addNotificationEmail(jobCreatePage)
        jobCreatePage.getCreateButton().click()
        jobCreatePage.waitForJobShow()
        jobCreatePage.getJobDefinitionModal().click()

        then:
        jobCreatePage.getNotificationDefinition().getText() == 'mail to: test@rundeck.com'

    }

    private void addNotificationEmail(JobCreatePage jobCreatePage) {
        jobCreatePage.getTab(JobTab.NOTIFICATIONS).click()
        jobCreatePage.getAddNotificationButtonByType(NotificationEvent.START).click()
        jobCreatePage.getNotificationDropDown().click()
        jobCreatePage.getNotificationByType(NotificationType.MAIL).click()
        jobCreatePage.getNotificationConfigByPropName("recipients").sendKeys('test@rundeck.com')
        jobCreatePage.getNotificationSaveButton().click()
        jobCreatePage.waitForModal(0)
    }

    def "edit job notifications"(){
        setup:
            LoginPage loginPage = page LoginPage
            SideBar sideBar = page SideBar
            JobsListPage jobListPage = page JobsListPage
            JobCreatePage jobCreatePage = page JobCreatePage
            ProjectHomePage projectHomePage = page ProjectHomePage

        when:
            loginPage.go()
            loginPage.login(TEST_USER, TEST_PASS)
            page(ProjectListPage).waitUntilPageLoaded()
            projectHomePage.goProjectHome("SeleniumBasic")
            sideBar.goTo(SideBarNavLinks.JOBS).click()
            jobListPage.getCreateJobLink().click()
            jobCreatePage.getJobNameField().sendKeys("a job without notifications")
            jobCreatePage.getTab(JobTab.WORKFLOW).click()

            def type = jobCreatePage.getStepByType(StepName.COMMAND, StepType.NODE)
            def panel = jobCreatePage.addStepButtonsPanel
            ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView(true);", panel)
            type.click()
            jobCreatePage.waitForStepToBeShown(By.id("adhocRemoteStringField"))
            jobCreatePage.el(By.id("adhocRemoteStringField")).sendKeys("echo 'example job'")
            jobCreatePage.getSaveStepButton().click()
            jobCreatePage.waitForSavedStep(0)

            //now save job without notifications
            jobCreatePage.createButton.click()
            jobCreatePage.waitForJobShow()
            jobCreatePage.getJobDefinitionModal().click()

            wait(Duration.ofSeconds(5)){
                until(
                    ExpectedConditions.numberOfElementsToBe(
                        jobCreatePage.notificationDefinitionBy,
                        0
                    )
                )
            }

            //now edit job and add notifications
            JobShowPage jobShowPage = page JobShowPage

            def jobUUID= jobShowPage.jobUuidText.text
            assert jobUUID
            jobCreatePage.goJobEditPage("SeleniumBasic", jobUUID)
            jobCreatePage.validateJobEditPage()

            //add notification
            addNotificationEmail(jobCreatePage)
            jobCreatePage.jobSaveButton.click()
            jobCreatePage.waitForJobShow()
            jobCreatePage.getJobDefinitionModal().click()

        then:
            jobCreatePage.getNotificationDefinition().getText() == 'mail to: test@rundeck.com'


    }

}