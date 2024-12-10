package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.execution.HtmlRenderedOutputPage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.activity.ActivityPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.jobs.JobShowPage
import spock.lang.Stepwise
@SeleniumCoreTest
@Stepwise
class JobActivityHistorySpec extends SeleniumBase {
    def setupSpec() {
        setupProject(SELENIUM_BASIC_PROJECT)
    }
    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        println "Login successful"
    }
    def "create job has basic fields"() {
        when:
        def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
        jobCreatePage.validatePage()
        jobCreatePage.jobNameInput
        jobCreatePage.groupPathInput
        jobCreatePage.descriptionTextarea
        println "job created successfully"
    }
    def "run job and validate execution"() {
        def executionShowPage = page ExecutionShowPage
        def jobShowPage = go JobShowPage, SELENIUM_BASIC_PROJECT
        when: "Navigate to the Job Show Page and run the job"
        println "Navigating to Job Show Page"
        jobShowPage.validatePage()
        println "Job Show Page validated"
        def runJobButton = driver.findElement(By.cssSelector(".btn.btn-success.btn-simple.btn-hover.btn-xs.act_execute_job"))
        println "Run Job button located"
        runJobButton.click()
        println "Run Job button clicked"
        def requestOptionField = driver.findElement(By.cssSelector("input[name='extra.option.reqOpt1']"))
        requestOptionField.sendKeys("test-value")
        println "Filled required option field"

        def createJobButton = driver.findElement(By.id("execFormRunButton"))
       createJobButton.click()
        println "Clicked Run Job Now"
        then: "Wait for the execution to complete and validate success"
        println "Waiting for execution to complete"
        executionShowPage.waitForElementVisible(executionShowPage.executionStateDisplayLabel)
        println "Execution state display label visible"
        println "Execution completed"
        executionShowPage.validateStatus('SUCCEEDED')
        println "Job executed successfully"
    }
    def "validate activity history from Job List Page"() {
        when: "Navigate to the Job List Page"
        def activityPage = go ActivityPage, SELENIUM_BASIC_PROJECT
        activityPage.loadActivityPageForProject(SELENIUM_BASIC_PROJECT)
        println "Navigating to Activity Page: ${driver.currentUrl}"
        then: "Validate job execution is listed in Activity History"
        activityPage.validatePage()
        println "Activity Page validated "
        List<WebElement> activityList = activityPage.getActivityRows()
        assert activityList.size() > 0 : "No activity rows found"
        println( "Found ${activityList.size()} activities rows")
        WebElement firstActivityRow = activityList.get(0)
        println "First activity row found DOM : ${firstActivityRow.getAttribute('outerHTML')}"
        assert firstActivityRow.findElement(By.cssSelector(".exec-status.icon")).getAttribute("data-statusstring") == 'succeeded': "First activity row is not SUCCEEDED"
        println "Activity history loaded successfully from Activity Page"

    }
    }
