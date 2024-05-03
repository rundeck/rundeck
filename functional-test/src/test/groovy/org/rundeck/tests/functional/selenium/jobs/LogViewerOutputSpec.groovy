package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.By
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.SideBarPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes

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
        jobShowPage.waitForLogOutput('log output ',3,5)
        jobShowPage.waitForLogOutput('log output ',49,40)
        def firstLogLine = jobShowPage.el(By.xpath("//span[contains(text(),'log output 1')]"))
        def lastLogLine = jobShowPage.el(By.xpath("//span[contains(text(),'log output 50')]"))

        then: "The user view must follow up to the last log output."
        !firstLogLine.isDisplayed() // First line is not reachable or visible by user
        lastLogLine.isDisplayed() // And the last line is

    }

    def "click on gutter and refresh should highlight correct line"() {

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
        jobShowPage.goToJob("f44481c4-159d-4176-869b-e4a9bd898fe4")
        jobShowPage.getRunJobBtn().click()
        jobShowPage.getLogOutputBtn().click()
        jobShowPage.waitForLogOutput('test output ',9,5)
        def lineToClick = jobShowPage.el(By.xpath("//span[contains(text(),'test output 5')]/ancestor::div[contains(@class, 'execution-log__line')]/div[@class='execution-log__gutter']"))
        lineToClick.click();
        def checkAfterClick = jobShowPage.waitForUrlToContain("#outputL5")
        driver.navigate().refresh();
        def checkAfterRefresh = jobShowPage.waitForUrlToContain("#outputL5")
        def selectedLine = jobShowPage.waitForElementVisible(By.xpath("//div[contains(@class, 'execution-log__line--selected')]")).isDisplayed()

        then:
        verifyAll {
            checkAfterClick
            checkAfterRefresh
            selectedLine
        }
    }

    def "doesnt show whale log warning message if maxLogSize is set"() {

        given:
        def loginPage = page LoginPage
        def sideBar = page SideBarPage
        def projectHomePage = page HomePage
        def jobShowPage = page JobShowPage
        def executionShowPage = page ExecutionShowPage

        when: "We run the job to have multiple lines in log output"
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        projectHomePage.validatePage()
        projectHomePage.goProjectHome(longOutPutProjectName)
        sideBar.goTo(NavLinkTypes.JOBS)
        jobShowPage.goToJob("1f0306cd-e123-44f3-8977-9e52edad8ce7")
        jobShowPage.getRunJobBtn().click()
        executionShowPage.validateStatus 'SUCCEEDED'
        executionShowPage.getLink 'Log Output' click()
        def showWarningMessage = jobShowPage.waitForElementVisible(By.xpath("//div[contains(@class, 'execution-log__warning')]")).isDisplayed()
        then:
        verifyAll {
            showWarningMessage
        }
    }

    def "show whale log warning message"() {

        given:
        def loginPage = page LoginPage
        def sideBar = page SideBarPage
        def projectHomePage = page HomePage
        def jobShowPage = page JobShowPage
        def executionShowPage = page ExecutionShowPage
        executionShowPage.setLoadPath("/execution/show/1?maxLogSize=5000000")

        when: "We run the job to have multiple lines in log output"
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        projectHomePage.validatePage()
        projectHomePage.goProjectHome(longOutPutProjectName)
        sideBar.goTo(NavLinkTypes.JOBS)
        jobShowPage.goToJob("1f0306cd-e123-44f3-8977-9e52edad8ce7")
        jobShowPage.getRunJobBtn().click()
        executionShowPage.validateStatus 'SUCCEEDED'
        executionShowPage.go()
        executionShowPage.getLink 'Log Output' click()
        def showWarningMessage = jobShowPage.waitForElementVisible(By.xpath("//div[contains(@class, 'execution-log__warning')]")).isDisplayed()
        then:
        verifyAll {
            showWarningMessage
        }
    }
}
