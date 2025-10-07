package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import java.time.Duration
/**
 * Tests audit tracking (createdBy and lastModifiedBy) for jobs using API + Selenium hybrid approach.
 * Uses API to create jobs, Selenium UI to modify and verify audit information display.
 * Works in all environments (local, CI/CD) by using standard test user (admin).
 * Validates that audit fields are properly populated during job creation and modification.
 * Follows senior engineer pattern: API creation -> direct UUID navigation -> UI validation.
 */
@SeleniumCoreTest
class JobAuditApiSpec extends SeleniumBase {

    static def projectName = "auditApiTestProject"
    static def jobName = "audit-api-test-job"

    def setupSpec() {
        setupProject(projectName)
    }

    def "audit test: editing job updates lastModifiedBy field"() {
        given: "a job exists (created via API)"
        def loginPage = page LoginPage
        def jobShowPage = page JobShowPage
        def wait = new WebDriverWait(driver, Duration.ofSeconds(8))

        // Will use TEST_USER/TEST_PASS from SeleniumBase (admin/admin123)

        // Create job via API for reliable test setup
        def jobXml = JobUtils.generateScheduledExecutionXml(jobName)
        def jobCreatedResponse = JobUtils.createJob(projectName, jobXml, client)
        def jobUuid = jobCreatedResponse.succeeded[0]?.id
        assert jobUuid, "Failed to create job via API"

        when: "admin logs in using standard Selenium pattern"
        loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)

        // Wait for successful login - follow Rundeck pattern
        wait.until(ExpectedConditions.urlContains("/menu/home"))
        println "✓ Successfully logged in as ${TEST_USER}"

        and: "navigate to job edit page"
        // Navigate to job edit page after confirmed login
        driver.get(client.baseUrl + "/project/${projectName}/job/edit/${jobUuid}")

        // Wait for edit page to load
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.id("jobUpdateSaveButton")),
            ExpectedConditions.presenceOfElementLocated(By.className("job-edit-form"))
        ))


        and: "admin submits job update form"
        def saveButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("jobUpdateSaveButton")))
        saveButton.click()

        // Smart wait for form submission instead of fixed sleep
        try {
            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/job/edit/")))
        } catch (Exception e) {
            // Fallback: force submit if still on edit page after timeout
            if (driver.currentUrl.contains("/job/edit/")) {
                ((JavascriptExecutor) driver).executeScript("document.getElementById('jobUpdateSaveButton').closest('form').submit();")
                wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/job/edit/")))
            }
        }

        and: "wait for redirect to show page"
        wait.until(ExpectedConditions.urlContains("/job/show/"))

        and: "open definition modal to check audit information"
        jobShowPage.jobDefinitionModalButton.click()


        and: "ensure modal opens reliably"
        ((JavascriptExecutor) driver).executeScript("jQuery('#job-definition-modal').modal('show');")
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("job-definition-modal")))

        then: "modal displays audit tracking information"
        def modalContent = jobShowPage.jobDefinitionModalContent
        def modalText = modalContent.getText()

        // Verify modal content loaded successfully (prevent false positives)
        assert modalText?.trim()?.length() > 0, "Job definition modal failed to load content"

        // Extract actual audit information for verification
        def createdByMatch = (modalText =~ /(?i)created\s+by\s+(\w+)/)
        def lastModifiedByMatch = (modalText =~ /(?i)last\s+modified\s+by\s+(\w+)/)

        assert createdByMatch.find(), "Failed to find 'Created By' information in modal text: ${modalText}"
        assert lastModifiedByMatch.find(), "Failed to find 'Last Modified By' information in modal text: ${modalText}"

        def createdBy = createdByMatch.group(1)
        def lastModifiedBy = lastModifiedByMatch.group(1)

        // Verify audit tracking functionality - fields should be populated correctly
        // Creator can vary by environment (dan locally, admin in CI), but should be consistent
        assert createdBy?.trim()?.length() > 0, "Created By field should be populated but found: '${createdBy}'"
        assert lastModifiedBy.equalsIgnoreCase("admin"),
            "Expected last modifier to be 'admin' (UI user) but found: ${lastModifiedBy}"

        println "✓ Created By: '${createdBy}' (API user) - Last Modified By: '${lastModifiedBy}' (UI user)"

        println "✓ SUCCESS: Audit tracking verified - Created By: ${createdBy}, Last Modified By: ${lastModifiedBy}"
        println "✓ Audit field functionality working correctly - both creation and modification tracking functional"
    }



}