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
import okhttp3.RequestBody
import okhttp3.MediaType
import okhttp3.Request

/**
 * Tests audit tracking (createdBy and lastModifiedBy) for jobs using API + Selenium hybrid approach.
 * Uses API to create and modify jobs, Selenium to verify UI display of audit information.
 * Works in CI/CD environments by using only existing realm users (admin).
 * Verifies that audit fields are properly set during job creation and modification.
 */
@SeleniumCoreTest
class JobAuditApiSpec extends SeleniumBase {

    static def projectName = "auditApiTestProject"
    static def jobName = "audit-api-test-job"

    def setupSpec() {
        setupProject(projectName)
    }

    def "audit test: verify job creation and modification tracking"() {
        given: "a job created by admin and page objects initialized"
        def loginPage = page LoginPage
        def jobShowPage = page JobShowPage
        def wait = new WebDriverWait(driver, Duration.ofSeconds(15))

        // Step 1: Create job via API (will be created by admin)
        def jobXml = JobUtils.generateScheduledExecutionXml(jobName)
        def jobCreatedResponse = JobUtils.createJob(projectName, jobXml, client)
        def jobUuid = jobCreatedResponse.succeeded[0]?.id
        assert jobUuid, "Failed to create job via API"
        println "✓ Job created by admin (UUID: ${jobUuid})"

        when: "we update the job via API to test audit tracking"
        // Step 2: Update job via API call (authenticated as admin)
        // This tests that lastModifiedBy field gets updated when job is modified
        def updatedJobXml = "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobName}</name>\n" +
                "      <group>api-test</group>\n" +
                "      <description>Job edited via API for audit test</description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <multipleExecutions>true</multipleExecutions>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>echo hello there - updated by admin</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"

        // Use standard API call to update the job
        RequestBody requestBody = RequestBody.create(updatedJobXml, MediaType.parse("application/xml"))
        Request request = new Request.Builder()
                .url(client.baseUrl + "/api/${client.apiVersion}/job/${jobUuid}")
                .method("PUT", requestBody)
                .header("Accept", "application/json")
                .build()
        def response = client.httpClient.newCall(request).execute()
        println "✓ Job updated via API v55, response code: ${response.code()}"

        then: "the API update should succeed"
        assert response.code() in [200, 204], "API update failed with code: ${response.code()}"
        println "✓ API update successful"

        when: "admin logs in to verify audit information in UI"
        loginPage.go()
        loginPage.login("admin", "admin")
        println "✓ Logged in as admin"

        // Navigate to job show page
        driver.get(client.baseUrl + "/project/${projectName}/job/show/${jobUuid}")

        // Wait for page load and open definition modal
        wait.until(ExpectedConditions.elementToBeClickable(jobShowPage.jobDefinitionModalButton))
        jobShowPage.jobDefinitionModalButton.click()

        // Ensure modal opens reliably
        def jsExecutor = driver as JavascriptExecutor
        jsExecutor.executeScript("jQuery('#job-definition-modal').modal('show');")
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("job-definition-modal")))
        println "✓ Job definition modal opened"

        then: "modal should display correct audit tracking information"
        def modalContent = jobShowPage.jobDefinitionModalContent
        def modalText = modalContent.getText()

        // Verify modal content loaded
        assert modalText?.trim()?.length() > 0, "Job definition modal failed to load content"
        println "Modal content preview: ${modalText.take(200)}..."

        // Extract audit information
        def createdByMatch = (modalText =~ /(?i)created\s+by[:\s]+(\w+)/)
        def lastModifiedByMatch = (modalText =~ /(?i)last\s+modified\s+by[:\s]+(\w+)/)

        assert createdByMatch.find(), "Failed to find 'Created By' information in modal text"
        assert lastModifiedByMatch.find(), "Failed to find 'Last Modified By' information in modal text"

        def createdBy = createdByMatch.group(1)
        def lastModifiedBy = lastModifiedByMatch.group(1)

        // Verify audit tracking shows admin as creator
        assert createdBy.equalsIgnoreCase("admin"),
            "Expected creator to be 'admin' but found: ${createdBy}"

        // Verify last modifier shows admin (who performed the API update)
        // This tests that lastModifiedBy field is properly set based on the authenticated user
        assert lastModifiedBy.equalsIgnoreCase("admin"),
            "Expected last modifier to be 'admin' (API authenticated user) but found: ${lastModifiedBy}"

        println "✓ SUCCESS: Audit tracking verified - Created By: ${createdBy}, Last Modified By: ${lastModifiedBy}"
        println "✓ Audit field updates working correctly - both creation and modification tracking functional"
    }

    def cleanupSpec() {
        deleteProject(projectName)
    }
}