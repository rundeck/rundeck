package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.NoSuchElementException
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

        // Use test credentials from SeleniumBase

        // Create job via API for test setup
        def jobXml = JobUtils.generateScheduledExecutionXml(jobName)
        def jobCreatedResponse = JobUtils.createJob(projectName, jobXml, client)
        def jobUuid = jobCreatedResponse.succeeded[0]?.id
        assert jobUuid, "Failed to create job via API"

        when: "admin logs in using standard Selenium pattern"
        loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)

        // Wait for successful login
        wait.until(ExpectedConditions.urlContains("/menu/home"))
        println "✓ Successfully logged in as ${TEST_USER}"

        and: "navigate to job edit page"
        // Navigate to job edit page
        driver.get(client.baseUrl + "/project/${projectName}/job/edit/${jobUuid}")

        // Wait for page to load
        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.id("jobUpdateSaveButton")),
            ExpectedConditions.presenceOfElementLocated(By.className("job-edit-form"))
        ))


        and: "admin submits job update form"
        def saveButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("jobUpdateSaveButton")))
        saveButton.click()

        // Wait for form submission
        try {
            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/job/edit/")))
        } catch (Exception e) {
            // Force submit if needed
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

        // Verify modal content loaded
        assert modalText?.trim()?.length() > 0, "Job definition modal failed to load content"

        // Hybrid approach: try CSS selectors first, fall back to regex for compatibility
        def createdBy, lastModifiedBy

        try {
            // Try robust CSS selectors first (preferred method)
            def createdByElem = modalContent.findElement(By.cssSelector("[data-testid='created-by']"))
            def lastModifiedByElem = modalContent.findElement(By.cssSelector("[data-testid='last-modified-by']"))

            createdBy = createdByElem.getText()
            lastModifiedBy = lastModifiedByElem.getText()

            println "✓ Using CSS selector approach for audit extraction"
        } catch (NoSuchElementException e) {
            // Fallback to regex pattern matching for compatibility
            println "⚠ CSS selectors not found, falling back to regex pattern matching"

            def createdByMatch = (modalText =~ /(?i)created\s+by\s+(\w+)/)
            def lastModifiedByMatch = (modalText =~ /(?i)last\s+modified\s+by\s+(\w+)/)

            assert createdByMatch.find(), "Failed to find 'Created By' information in modal text: ${modalText}"
            assert lastModifiedByMatch.find(), "Failed to find 'Last Modified By' information in modal text: ${modalText}"

            createdBy = createdByMatch.group(1)
            lastModifiedBy = lastModifiedByMatch.group(1)

            println "✓ Using regex pattern approach for audit extraction"
        }

        // Verify audit fields populated (works with both extraction methods)
        assert createdBy?.trim()?.length() > 0, "Created By field should be populated but found: '${createdBy}'"
        assert lastModifiedBy?.trim()?.toLowerCase() == "admin",
            "Expected last modifier to be 'admin' (UI user) but found: ${lastModifiedBy}"

        println "✓ Created By: '${createdBy}' (API user) - Last Modified By: '${lastModifiedBy}' (UI user)"

        println "✓ SUCCESS: Audit tracking verified - Created By: ${createdBy}, Last Modified By: ${lastModifiedBy}"
        println "✓ Audit field functionality working correctly - both creation and modification tracking functional"
    }



}