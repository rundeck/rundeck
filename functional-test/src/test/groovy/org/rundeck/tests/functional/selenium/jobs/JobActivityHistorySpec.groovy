package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.activity.ActivityPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import spock.lang.Stepwise
import java.time.Duration

@SeleniumCoreTest
@Stepwise
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
        assert activityList.size() > 0: "No activity rows found"
        WebElement firstActivityRow = activityList.get(0)
        assert firstActivityRow.findElement(By.cssSelector(".exec-status.icon")).getAttribute("data-statusstring") == 'succeeded': "First activity row is not SUCCEEDED"
    }
    /**
     Test 2: Validate Activity History from Job List Page with Saved Filters
     */
    def "validate activity history from Job List Page Saved Filters"() {
        when: "Navigate to the Job List Page and apply saved filter"
        def jobListPage = go JobListPage, SELENIUM_BASIC_PROJECT
        jobListPage.validatePage()
        def saveFilterButton = getDriver().findElement(By.cssSelector("button[data-test-id='save-filter-button']"))
        saveFilterButton.click()
        def saveFilterNameField = getDriver().findElement(By.cssSelector("input.form-control[data-action='auto-focus']"))
        saveFilterNameField.sendKeys("MySavedFilter")
        def okButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[6]/div/div/div[3]/button[2]")))
        okButton.click()
        def filterDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("span.dropdown-toggle.btn.btn-secondary.btn-sm.text-secondary")))
        filterDropdown.click()
        def savedFilterLink = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a[data-test='filter-link']")))
        savedFilterLink.click()
        then: "Validate that the filter is applied and results are displayed"
        def activityRows = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("table.activity-list-table tbody tr.link.activity_row")))
        assert activityRows.size() > 0: "No filtered results found"
        def firstRowStatus = activityRows.get(0).findElement(By.cssSelector("td.eventicon")).getAttribute("title")
        assert firstRowStatus == 'succeeded': "Expected 'succeeded', but found: ${firstRowStatus}"


    }
    /**
     Test 3: Review Job Activity History from Activity Page
     */
    def "review job activity history from Activity Page"() {
        when: "Navigate to the Activity Page after validating saved filter"
        def activityPage = go ActivityPage, SELENIUM_BASIC_PROJECT
        activityPage.loadActivityPageForProject(SELENIUM_BASIC_PROJECT)
        then: "Validate job execution is listed in Activity History"
        activityPage.validatePage()
        def activityList = activityPage.getActivityRows()
        assert activityList.size() > 0: "No activity rows found on the Activity Page"
        def firstActivityRow = activityList.get(0)
        def firstActivityStatus = firstActivityRow.findElement(By.cssSelector(".exec-status.icon")).getAttribute("data-statusstring")
        assert firstActivityStatus.equalsIgnoreCase("SUCCEEDED"): "Expected 'SUCCEEDED', but found: '${firstActivityStatus}'"
    }
}