
package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
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
        // Set up WebDriverWait to wait up to 30 seconds for elements that take time to appear or change
        wait = new WebDriverWait(getDriver(), Duration.ofSeconds(30))
        (go LoginPage).login(TEST_USER, TEST_PASS)
        def jobDefinition = JobUtils.generateScheduledExecutionXml("TestJobActivityHistory")
        def client = getClient()
        def response = JobUtils.createJob(SELENIUM_BASIC_PROJECT, jobDefinition, client)
        jobId = response.succeeded[0].id
        assert JobUtils.executeJob(jobId, client).isSuccessful()
    }


    def "review job activity history from Job List Page"() {
        when: "Navigate to the Job List Page"
        def JobListPage = go JobListPage, SELENIUM_BASIC_PROJECT

        then: "Validate job execution is listed in Activity History"
        JobListPage.validatePage()
        wait.until {
        !JobListPage.getActivityRows().isEmpty()

        }
    }

    def "review job activity history from Job List Page Saved Filters"() {
        when: "Navigate to the Job List Page and apply a saved filter"
        def jobListPage = go JobListPage, SELENIUM_BASIC_PROJECT
        jobListPage.clickAnyTimeButton()
                .clickLastWeekButton()
                .clickSaveFilterButton()
                .enterFilterName("TestFilter")
                .confirmFilterSave()
                .openFilterDropdown()
                .selectSavedFilter()
        then: "Validate the saved filter is applied"
        wait.until{
            jobListPage.getAppliedFilterName()=="TestFilter"
        }

        }


    def "review job activity history from Activity Page"() {
        when: "Navigate to the Activity Page"
        def activityPage = go ActivityPage, SELENIUM_BASIC_PROJECT
        activityPage.validatePage()
        activityPage.loadActivityPageForProject(SELENIUM_BASIC_PROJECT)

        then: "Validate job execution is listed in Activity History"
        !activityPage.getActivityRows().isEmpty()

    }
}