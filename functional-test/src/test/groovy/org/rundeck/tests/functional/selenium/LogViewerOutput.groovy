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
class LogViewerOutput extends SeleniumBase{

    def setupSpec(){
        setupProject("AutoFollowTest")
    }

    def "auto scroll on log viewer page show last input"() {

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
        projectHomePage.goProjectHome("AutoFollowTest")
        projectHomePage.waitForUrlToContain("AutoFollowTest")
        sideBar.goTo(NavLinkTypes.JOBS)
        jobListPage.getCreateJobLink().click()
        jobCreatePage.getJobNameField().sendKeys("loop job")
        jobCreatePage.getTab(JobTab.WORKFLOW).click()
        jobCreatePage.getStepByType(StepName.COMMAND, StepType.NODE).click()
        jobCreatePage.waitForStepToBeShown(By.name("pluginConfig.adhocRemoteString"))
        jobCreatePage.el(By.name("pluginConfig.adhocRemoteString")).sendKeys("for i in {1..60}; do echo NUMBER \$i; sleep 0.2; done")
        jobCreatePage.getSaveStepButton().click()
        jobCreatePage.waitForSavedStep(0)
        jobCreatePage.getCreateButton().click()
        jobShowPage.getRunJobBtn().click()
        jobShowPage.getLogOutputBtn().click()
        jobShowPage.waitForLogOutput(By.xpath("//span[contains(text(),'NUMBER ')]"),3,5)
        def firstLocation = jobShowPage.el(By.xpath("//span[contains(text(),'NUMBER 1')]")).getLocation()
        jobShowPage.waitForLogOutput(By.xpath("//span[contains(text(),'NUMBER ')]"),35,40)
        def finalLocation = jobShowPage.el(By.xpath("//span[contains(text(),'NUMBER 1')]")).getLocation()

        then:
        firstLocation != finalLocation

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
        projectHomePage.goProjectHome("AutoFollowTest")
        projectHomePage.waitForUrlToContain("AutoFollowTest")
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
