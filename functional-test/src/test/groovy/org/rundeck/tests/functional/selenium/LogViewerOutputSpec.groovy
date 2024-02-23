package org.rundeck.tests.functional.selenium

import org.openqa.selenium.By
import org.rundeck.tests.functional.selenium.pages.*
import org.rundeck.tests.functional.selenium.pages.home.HomePage
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.tests.functional.selenium.pages.project.SideBarPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.setup.NavLinkTypes

@SeleniumCoreTest
class LogViewerOutputSpec extends SeleniumBase{

    private static final def longOutPutProjectName = "test-long-job-output"

    def setupSpec(){
        setupProjectArchiveDirectoryResource(longOutPutProjectName, "/projects-import/long-job-output-project")
    }

    def "auto scroll on log viewer page show last input"() {

        given:
        def loginPage = page LoginPage
        def sideBar = page SideBarPage
        def projectHomePage = page HomePage
        def jobShowPage = page JobShowPage

        when: "We run the job to have multiple lines in log output"
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        projectHomePage.validatePage()
        projectHomePage.goProjectHome(longOutPutProjectName)
        sideBar.goTo(NavLinkTypes.JOBS)
        jobShowPage.goToJob("f44481c4-159d-4176-869b-e4a9bd898fe5")
        jobShowPage.getRunJobBtn().click()
        jobShowPage.getLogOutputBtn().click()
        jobShowPage.waitForLogOutput(By.xpath("//span[contains(text(),'log output ')]"),3,5)
        jobShowPage.waitForLogOutput(By.xpath("//span[contains(text(),'log output ')]"),49,40)
        def firstLogLine = jobShowPage.el(By.xpath("//span[contains(text(),'log output 1')]"))
        def lastLogLine = jobShowPage.el(By.xpath("//span[contains(text(),'log output 50')]"))

        then: "The user view must follow up to the last log output."
        !firstLogLine.isDisplayed() // First line is not reachable or visible by user
        lastLogLine.isDisplayed() // And the last line is

    }

    def "click on gutter and refresh should highlight correct line"() {

        setup:
        def loginPage = page LoginPage
        def sideBar = page SideBarPage
        def jobListPage = page JobsListPage
        def jobCreatePage = page JobCreatePage
        def projectHomePage = page HomePage
        def jobShowPage = page JobShowPage

        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        projectHomePage.validatePage()
        projectHomePage.goProjectHome(longOutPutProjectName)
        sideBar.goTo(NavLinkTypes.JOBS)
        jobListPage.getCreateJobLink().click()
        jobCreatePage.getJobNameField().sendKeys("loop job")
        jobCreatePage.getTab(JobTab.WORKFLOW).click()
        jobCreatePage.getStepByType(StepName.COMMAND, StepType.NODE).click()
        jobCreatePage.waitForStepToBeShown(By.name("pluginConfig.adhocRemoteString"))
        jobCreatePage.el(By.name("pluginConfig.adhocRemoteString")).sendKeys("for i in {1..6}; do echo NUMBER \$i; sleep 0.5; done")
        jobCreatePage.getSaveStepButton().click()
        jobCreatePage.waitForSavedStep(0)
        jobCreatePage.getCreateButton().click()
        jobShowPage.getRunJobBtn().click()
        jobShowPage.getLogOutputBtn().click()
        jobShowPage.waitForLogOutput(By.xpath("//span[contains(text(),'NUMBER ')]"),3,5)
        def lineToClick = jobShowPage.el(By.xpath("//span[contains(text(),'NUMBER 1')]/ancestor::div[contains(@class, 'execution-log__line')]/div[@class='execution-log__gutter']"))

        lineToClick.click();
        jobShowPage.waitForUrlToContain("#outputL1")
        driver.navigate().refresh();
//        def href = commandPage.runningButtonLink().getAttribute("href")
//        jobShowPage.driver.get href + "#outputL1"

        jobShowPage.waitForLogOutput(By.xpath("//span[contains(text(),'NUMBER ')]"),3,5)

        then:
        def selectedLine = jobShowPage.waitForElementVisible(By.xpath("//div[contains(@class, 'execution-log__line--selected')]"))

        assert selectedLine != null : "Expected at least one element with the specified class to be present"
    }
}
