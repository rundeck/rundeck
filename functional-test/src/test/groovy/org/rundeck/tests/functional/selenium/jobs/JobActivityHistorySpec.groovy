
package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.activity.ActivityPage
import org.rundeck.util.common.jobs.JobUtils
import spock.lang.Shared

import java.time.Duration

@SeleniumCoreTest
class JobActivityHistorySpec extends SeleniumBase {
    @Shared
    WebDriverWait wait

    @Shared
    String jobId

    def setupSpec() {
        setupProject(SELENIUM_BASIC_PROJECT)
        def jobDefinition = JobUtils.generateScheduledExecutionXml("TestJobActivityHistory")
        def client = getClient()
        def response = JobUtils.createJob(SELENIUM_BASIC_PROJECT, jobDefinition, client)
        jobId = response.succeeded[0].id
        JobUtils.runExecuteJob(jobId, client)
    }

    def setup() {
        wait = new WebDriverWait(getDriver(), Duration.ofSeconds(30))
        (go LoginPage).login(TEST_USER, TEST_PASS)
    }

    def "review job activity history from Job List Page"() {
        when: "Navigate to the Job List Page"
        def jobListPage = go JobListPage, SELENIUM_BASIC_PROJECT

        then: "Validate job execution is listed in Activity History"
        jobListPage.validatePage()
        wait.until {
            !jobListPage.getActivityRows().isEmpty()
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
        wait.until {
            jobListPage.getAppliedFilterName() == "TestFilter"
        }
    }

    def "review job activity history from Activity Page"() {
        when: "Navigate to the Activity Page"
        def activityPage = go ActivityPage, SELENIUM_BASIC_PROJECT
        activityPage.validatePage()
        activityPage.loadActivityPageForProject(SELENIUM_BASIC_PROJECT)

        then: "Validate job execution is listed in Activity History"
        wait.until {
            !activityPage.getActivityRows().isEmpty()
        }
    }
}