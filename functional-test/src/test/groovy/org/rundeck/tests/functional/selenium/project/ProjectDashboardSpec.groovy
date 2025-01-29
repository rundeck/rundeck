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

    @Shared
    String jobId

    def setupSpec() {
        setupProject(projectName)
        // Create a scheduled job that runs every 2 seconds
        def jobDefinition = JobUtils.generateScheduledJobsXml("DashboardTestJob", "<time hour='*' seconds='*/2' minute='*' />")
        def client = getClient()
        def response = JobUtils.createJob(projectName, jobDefinition, client)
        jobId = response.succeeded[0].id
        assert JobUtils.executeJob(jobId, client).isSuccessful()
    }

    def setup() {
        wait = new WebDriverWait(getDriver(), Duration.ofSeconds(35))
        (go LoginPage).login(TEST_USER, TEST_PASS)
    }

    /**
     Test Case 1: Validate Running Jobs on Dashboard
     */
    def "Validate Running Jobs in Project Dashboard"() {
        when: "Navigate to the Project Dashboard Page"
        def dashboardPage = go DashboardPage, projectName
        then: "Dashboard should contain Running Jobs"

        wait.until {
            def projectSummaryCountLink = dashboardPage.getProjectSummaryCountLink()
            projectSummaryCountLink.isDisplayed()
        }
    }
    def cleanupSpec() {
        deleteProject(projectName)
    }
}
