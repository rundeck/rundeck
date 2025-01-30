package org.rundeck.tests.functional.selenium.project

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.DashboardPage
import spock.lang.Shared

import java.time.Duration

@SeleniumCoreTest
class ProjectDashboardSpec extends SeleniumBase {
    @Shared
    WebDriverWait wait

    @Shared
    String projectName = "dashboard-test-project"

    def setupSpec() {
        setupProject(projectName)
        def jobDefinition = JobUtils.generateScheduledExecutionXml("DashboardTestJob")
        def client = getClient()
        def response = JobUtils.createJob(projectName, jobDefinition, client)
        def jobId = response.succeeded[0].id
        assert JobUtils.executeJob(jobId, client).isSuccessful()
    }

    def setup() {
        wait = new WebDriverWait(getDriver(), Duration.ofSeconds(35))
        (go LoginPage).login(TEST_USER, TEST_PASS)
    }
    // Not ready for review yet. I need to finish the implementation of the test case.

    /**
     Test Case 1: Validate Running Jobs on Dashboard
     */
    def "Validate Execution Count and Users in Project Dashboard"() {
        when: "Navigate to the Project Dashboard Page"
        def dashboardPage = go DashboardPage, projectName
        then: "Dashboard should display execution count, user count, and user details correctly"

        wait.until {
            println("Waiting for execution count element...")
            def executionCountElement = getDriver().findElement(By.xpath("/html/body/section[1]/div/section/div[2]/div/div/div[2]/div/div/div/div/div/a"))
            println("Execution Count Element Found: ${executionCountElement != null}")
            println("Execution Count Element Text: ${executionCountElement.getText()}")

            println("Waiting for user count element...")
            def userCountElement = getDriver().findElement(By.xpath("//p[contains(text(),'by')]/span[@class='text-info']"))
            println("User Count Element Found: ${userCountElement != null}")
            println("User Count Element Text: ${userCountElement.getText()}")

            println("Waiting for user element...")
            def userElement = getDriver().findElement(By.xpath("/html/body/section[1]/div/section/div[2]/div/div/div[2]/div/div/div/div/div/div/ul/li"))
            println("User Element Found: ${userElement != null}")
            println("User Element Text: ${userElement.getText()}")

            executionCountElement.isDisplayed() &&
                    userCountElement.isDisplayed() &&
                    userElement.isDisplayed()
        }

        and: "Execution count should be a valid number"
        def executionCountElement = getDriver().findElement(By.xpath("/html/body/section[1]/div/section/div[2]/div/div/div[2]/div/div/div/div/div/a"))
        def executionCountText = executionCountElement.getText().trim()
        println("Execution Count Text: ${executionCountText}")
        and: "User count should be a valid number"
        def userCountElement = getDriver().findElement(By.xpath("//p[contains(text(),'by')]/span[@class='text-info']"))
        def userCountText = userCountElement.getText().trim()
        println("User Count Text: ${userCountText}")


        and: "User should not be empty"
        def userElement = getDriver().findElement(By.xpath("/html/body/section[1]/div/section/div[2]/div/div/div[2]/div/div/div/div/div/div/ul/li"))
        def userText = userElement.getText().trim()
        println("User Text: ${userText}")
        assert !userText.isEmpty()
    }
}