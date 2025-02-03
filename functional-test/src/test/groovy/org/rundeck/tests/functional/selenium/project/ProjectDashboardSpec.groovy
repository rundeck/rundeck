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


    def "Validate Execution Count in Project Dashboard"() {
        when: "Navigate to the Project Dashboard Page"
        def dashboardPage = go DashboardPage, projectName

        then: "Dashboard should display execution count correctly and it should be a valid number"
        wait.until {
            def executionCountElement = dashboardPage.getExecutionCountElement()
            executionCountElement.isDisplayed()
        }

        def executionCountElement = dashboardPage.getExecutionCountElement()
        def executionCountText = executionCountElement.getText().trim()

        // Extract the numeric part from the text
        def executionCountNumber = executionCountText.replaceAll("[^0-9]", "")

        expect:
        executionCountNumber.isNumber()
    }

    def "Validate User Count and Details in Project Dashboard"() {
        when: "Navigate to the Project Dashboard Page"
        def dashboardPage = go DashboardPage, projectName

        then: "Dashboard should display user count and user details correctly and user count should be a valid number and user should not be empty"
        wait.until {
            def userCountElement = dashboardPage.getUserCountElement()
            def userElement = dashboardPage.getUserElement()
            userCountElement.isDisplayed() && userElement.isDisplayed()
        }

        def userCountElement = dashboardPage.getUserCountElement()
        def userCountText = userCountElement.getText().trim()
        def userElement = dashboardPage.getUserElement()
        def userText = userElement.getText().trim()

        expect:
        userCountText.isNumber()
        !userText.isEmpty()
    }
}