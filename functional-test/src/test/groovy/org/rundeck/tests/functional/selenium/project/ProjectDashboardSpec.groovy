package org.rundeck.tests.functional.selenium.project

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
    String projectName = "test-project-dashboard"

    def setupSpec() {
        setupProject(projectName)
        def jobDefinition = JobUtils.generateScheduledExecutionXml("DashboardTestJob")
        def client = getClient()
        def response = JobUtils.createJob(projectName, jobDefinition, client)
        def jobId = response.succeeded[0].id
        JobUtils.runExecuteJob(jobId, client)
    }

    def setup() {
        wait = new WebDriverWait(getDriver(), Duration.ofSeconds(35))
        (go LoginPage).login(TEST_USER, TEST_PASS)
    }


    def "Validate Execution Count, User Count, and User Details in Project Dashboard"() {
        given: "Expected execution count, user count, and user name"
        def expectedExecutionCount = 1
        def expectedUserCount = 1
        def expectedUserName = "admin"

        when: "Navigate to the Project Dashboard Page"
        def dashboardPage = go DashboardPage, projectName
        dashboardPage.validatePage()

        then: "Dashboard should display correct execution count, user count, and user details"
        wait.until {
            def executionCountElement = dashboardPage.getExecutionCountElement()
            def userCountElement = dashboardPage.getUserCountElement()
            def userElement = dashboardPage.getUserElement()
            executionCountElement.isDisplayed() &&
                    userCountElement.isDisplayed() &&
                    userElement.isDisplayed()
        }

        and: "Extract values from the page"
        def executionCountText = dashboardPage.getExecutionCountElement().getText().trim()
        def userCountText = dashboardPage.getUserCountElement().getText().trim()
        def userText = dashboardPage.getUserElement().getText().trim()

        // Extract numeric part for execution count
        def executionCountNumber = executionCountText.replaceAll("[^0-9]", "")

        expect:
        executionCountNumber == expectedExecutionCount.toString() // Validate exact execution count
        userCountText == expectedUserCount.toString() // Validate exact user count
        userText == expectedUserName // Validate exact username
    }

    def cleanupSpec() {
        deleteProject(projectName)
    }
}