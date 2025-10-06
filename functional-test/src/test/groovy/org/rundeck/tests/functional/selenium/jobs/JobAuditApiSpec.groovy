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
 * Tests cross-user audit tracking using API + Selenium hybrid approach
 * Uses API for job creation/navigation, Selenium only for UI verification
 *
 * The test can be run with different users via system properties:
 * -Dtest.modifying.user=myuser -Dtest.modifying.password=mypass
 *
 * This verifies that ANY user can modify a job and the audit tracking works correctly.
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
        def wait = new WebDriverWait(driver, Duration.ofSeconds(15))

        // Use configurable test user (can be overridden via system properties)
        def modifyingUser = System.getProperty("test.modifying.user", "jaya")
        def modifyingPassword = System.getProperty("test.modifying.password", "jaya123")

        // Create job via API for reliable test setup
        def jobXml = JobUtils.generateScheduledExecutionXml(jobName)
        def jobCreatedResponse = JobUtils.createJob(projectName, jobXml, client)
        def jobUuid = jobCreatedResponse.succeeded[0]?.id

        when: "${modifyingUser} logs in and goes directly to edit page"
        loginPage.go()
        loginPage.login(modifyingUser, modifyingPassword)

        // Go directly to edit page with #workflow fragment for efficiency
        driver.get(client.baseUrl + "/project/${projectName}/job/edit/${jobUuid}#workflow")

        and: "${modifyingUser} submits job update form"
        def saveButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("jobUpdateSaveButton")))
        saveButton.click()

        // Check if form submitted, if not use JavaScript fallback
        Thread.sleep(2000)
        def jsExecutor = driver as JavascriptExecutor
        if (driver.currentUrl.contains("/job/edit/")) {
            jsExecutor.executeScript("document.getElementById('jobUpdateSaveButton').closest('form').submit();")
            Thread.sleep(2000)
        }

        and: "wait for redirect to show page"
        wait.until(ExpectedConditions.urlContains("/job/show/"))

        and: "open definition modal to check audit information"
        jobShowPage.jobDefinitionModalButton.click()

        and: "ensure modal opens reliably"
        jsExecutor.executeScript("jQuery('#job-definition-modal').modal('show');")
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("job-definition-modal")))

        then: "modal displays correct cross-user audit tracking"
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

        // Verify cross-user audit tracking with actual values
        assert lastModifiedBy.equalsIgnoreCase(modifyingUser),
            "Expected 'Last Modified By: ${modifyingUser}' but found 'Last Modified By: ${lastModifiedBy}'"

        assert !createdBy.equalsIgnoreCase(modifyingUser),
            "Cross-user verification failed - Expected creator to NOT be '${modifyingUser}' but found 'Created By: ${createdBy}'"

        println "✓ Audit tracking verified: Created By: ${createdBy}, Last Modified By: ${lastModifiedBy}"
    }

    def cleanupSpec() {
        deleteProject(projectName)
    }
}