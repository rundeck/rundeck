
package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.activity.ActivityPage
import org.rundeck.util.common.jobs.JobUtils
import java.time.Duration

@SeleniumCoreTest
class JobActivityHistorySpec extends SeleniumBase {
    WebDriverWait wait
    String jobId

    def setupSpec() {
        setupProject(SELENIUM_BASIC_PROJECT)
    }

    def setup() {
        wait = new WebDriverWait(getDriver(), Duration.ofSeconds(20))
        // Login
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        // Create a job
        def jobDefinition = JobUtils.generateScheduledExecutionXml("TestJobActivityHistory")
        def client = getClient() // Assuming a method exists to get RdClient
        def response = JobUtils.createJob(SELENIUM_BASIC_PROJECT, jobDefinition, client)
        assert response.succeeded.size() > 0: "Failed to create job"
        jobId = response.succeeded[0].id
        // Run the job
        def runResponse = JobUtils.executeJob(jobId, client)
        assert runResponse.code() == 200: "Failed to execute job"
    }

    def cleanup() {
        getDriver().quit()
    }

    /**
     Test 1: Review Job Activity History from Job List Page
     */
    def "review job activity history from Job List Page"() {
        when: "Navigate to the Job List Page"
        def activityPage = go ActivityPage, SELENIUM_BASIC_PROJECT
        activityPage.loadActivityPageForProject(SELENIUM_BASIC_PROJECT)
        then: "Validate job execution is listed in Activity History"
        activityPage.validatePage()
        List<WebElement> activityList = activityPage.getActivityRows()
        assert activityList.size() > 0: "No activity rows found"
        WebElement firstActivityRow = activityList.get(0)
        WebElement statusIcon = ExecutionShowPage.getActivityExecStatusIcon(firstActivityRow)
        String status = statusIcon.getAttribute("data-statusstring")
        assert status.equalsIgnoreCase("SUCCEEDED"): "Expected 'SUCCEEDED', but found: '${status}'"
    }
    /**
     Test 2: Validate Activity History from Job List Page with Saved Filters
     */
    def "validate activity history from Job List Page Saved Filters"() {
        when: "Navigate to the Job List Page and apply a saved filter"
        def jobListPage = go JobListPage, SELENIUM_BASIC_PROJECT
        jobListPage.validatePage()
        // Save and apply the filter
        jobListPage.clickSaveFilterButton()
                .enterFilterName("MySavedFilter")
                .confirmFilterSave()
        jobListPage.openFilterDropdown()
                .selectSavedFilter()
        then: "Validate that the saved filter is applied and results are filtered"
        def filteredRows = jobListPage.getActivityRows()
        assert filteredRows.size() > 0: "No filtered results found"
        // Verify the status of the first filtered row
        def firstRowStatus = jobListPage.getFirstRowStatus()
        assert firstRowStatus.equalsIgnoreCase('SUCCEEDED'):
                "Expected 'SUCCEEDED' status, but found: '${firstRowStatus}'"
    }

    def "review job activity history from Activity Page"() {
        when: "Navigate to the Activity Page"
        def activityPage = go ActivityPage, SELENIUM_BASIC_PROJECT
        activityPage.loadActivityPageForProject(SELENIUM_BASIC_PROJECT)
        then: "Validate job execution is listed in Activity History"
        activityPage.validatePage()
        def activityList = activityPage.getActivityRows()
        assert activityList.size() > 0: "No activity rows found on the Activity Page"
        // Validate the first row status
        def firstActivityRow = activityList.get(0)
        def firstActivityStatusIcon = ExecutionShowPage.getActivityExecStatusIcon(firstActivityRow)
        def firstActivityStatus = firstActivityStatusIcon.getAttribute("data-statusstring")
        assert firstActivityStatus.equalsIgnoreCase("SUCCEEDED"):
                "Expected 'SUCCEEDED', but found: '${firstActivityStatus}'"
    }
}