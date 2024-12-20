package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.activity.ActivityPage
import java.time.Duration


@SeleniumCoreTest
class JobActivityHistorySpec extends SeleniumBase {
    WebDriverWait wait

    def setupSpec() {
        setupProject(SELENIUM_BASIC_PROJECT)
    }

    def setup() {
        wait = new WebDriverWait(getDriver(), Duration.ofSeconds(20))
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
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
        assert activityList.size() > 0 : "No activity rows found"
        WebElement firstActivityRow = activityList.get(0)
        WebElement statusIcon = ExecutionShowPage.getActivityExecStatusIcon(firstActivityRow)
        String status = statusIcon.getAttribute("data-statusstring")
        assert status.equalsIgnoreCase("SUCCEEDED") : "Expected 'SUCCEEDED', but found: '${status}'"
    }

    /**
     Test 2: Validate Activity History from Job List Page with Saved Filters
     */
    def "validate activity history from Job List Page Saved Filters"() {
        when: "Navigate to the Job List Page and apply saved filter"
        def jobListPage = go JobListPage, SELENIUM_BASIC_PROJECT
        jobListPage.validatePage()
        // Save and apply the filter
        jobListPage.clickSaveFilterButton()
                .enterFilterName("MySavedFilter")
                .confirmFilterSave()
                .openFilterDropdown()
                .selectSavedFilter()
        then: "Validate that the filter is applied and results are filtered"
        assert jobListPage.getActivityRows().size() > 0 : "No filtered results found"
        // Verify first row status
        assert jobListPage.getFirstRowStatus() == 'succeeded' : "Expected 'succeeded' status"
    }

    /**
     Test 3: Review Job Activity History from Activity Page
     */
    def "review job activity history from Activity Page"() {
        when: "Navigate to the Activity Page"
        def activityPage = go ActivityPage, SELENIUM_BASIC_PROJECT
        activityPage.loadActivityPageForProject(SELENIUM_BASIC_PROJECT)
        then: "Validate job execution is listed in Activity History"
        activityPage.validatePage()
        def activityList = activityPage.getActivityRows()
        assert activityList.size() > 0 : "No activity rows found on the Activity Page"
        // Validate the first row status
        def firstActivityRow = activityList.get(0)
        def firstActivityStatus = ExecutionShowPage.getActivityExecStatusIcon(firstActivityRow)
                .getAttribute("data-statusstring")
        assert firstActivityStatus.equalsIgnoreCase("SUCCEEDED") :
                "Expected 'SUCCEEDED', but found: '${firstActivityStatus}'"
    }
}