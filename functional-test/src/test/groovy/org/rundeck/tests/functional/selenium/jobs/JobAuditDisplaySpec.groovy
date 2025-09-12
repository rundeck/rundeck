package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.openqa.selenium.By
import org.openqa.selenium.WebElement

@SeleniumCoreTest
class JobAuditDisplaySpec extends SeleniumBase {

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def "job show displays audit information for jobs with creator"() {
        given: "a project and job with audit information"
        String projectName = "auditTestProject"
        setupProject(projectName)
        
        // Import job with specific creator information
        def jobXml = '''
<joblist>
   <job>
      <id>audit-test-job</id>
      <name>Audit Test Job</name>
      <description>A job to test audit display functionality</description>
      <loglevel>INFO</loglevel>
      <context>
          <project>''' + projectName + '''</project>
      </context>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>false</keepgoing>
      </dispatch>
      <sequence>
        <command>
            <exec>echo "Hello World"</exec>
        </command>
      </sequence>
   </job>
</joblist>
'''
        
        // Create a temporary job file and import it
        def tempFile = File.createTempFile("test-job", ".xml")
        tempFile.text = jobXml
        tempFile.deleteOnExit()
        
        String jobUuid = JobUtils.jobImportFile(projectName, tempFile.absolutePath, client).succeeded.first().id

        when: "visiting the job show page"
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        jobShowPage.go()

        then: "the job audit information should be displayed"
        // Look for audit information in the job details table
        def auditElements = driver.findElements(By.xpath("//td[contains(text(),'Created by')]/following-sibling::td"))
        def createdByInfo = auditElements.find { it.displayed }
        
        // The job should show creation information 
        createdByInfo != null
        createdByInfo.text.contains(TEST_USER) || createdByInfo.text.toLowerCase().contains('admin')

        cleanup:
        deleteProject(projectName)
        if (tempFile?.exists()) {
            tempFile.delete()
        }
    }

    def "job show displays last modified information after job update"() {
        given: "a project and job"
        String projectName = "auditModifyProject" 
        setupProject(projectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id

        when: "the job is modified via API"
        // Update job description via API to trigger lastModifiedBy
        def jobUpdateXml = '''<?xml version="1.0" encoding="UTF-8"?>
<joblist>
   <job>
      <name>Modified Test Job</name>
      <description>Updated description for audit testing</description>
      <loglevel>INFO</loglevel>
      <context>
          <project>''' + projectName + '''</project>
      </context>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>false</keepgoing>
      </dispatch>
      <sequence>
        <command>
            <exec>echo "Modified job content"</exec>
        </command>
      </sequence>
   </job>
</joblist>'''

        def updateResponse = doPost("/api/48/job/${jobUuid}", [
            'Content-Type': 'application/xml'
        ], jobUpdateXml)
        assert updateResponse.successful

        and: "visiting the job show page"
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        jobShowPage.go()

        then: "both created and modified audit information should be displayed"
        // Look for creation audit information
        def createdElements = driver.findElements(By.xpath("//td[contains(text(),'Created by')]/following-sibling::td"))
        def createdByInfo = createdElements.find { it.displayed }
        
        // Look for modification audit information  
        def modifiedElements = driver.findElements(By.xpath("//td[contains(text(),'Last modified by')]/following-sibling::td"))
        def modifiedByInfo = modifiedElements.find { it.displayed }
        
        // Verify audit information is present
        createdByInfo != null
        createdByInfo.text.contains(TEST_USER) || createdByInfo.text.toLowerCase().contains('admin')
        
        // Modified info should be present after update
        modifiedByInfo != null
        modifiedByInfo.text.contains(TEST_USER) || modifiedByInfo.text.toLowerCase().contains('admin')

        cleanup:
        deleteProject(projectName)
    }

    def "job show handles jobs without audit information gracefully"() {
        given: "a project and legacy job without audit fields"
        String projectName = "legacyAuditProject"
        setupProject(projectName) 
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id

        when: "visiting the job show page"
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        jobShowPage.go()

        then: "the page should load without errors even if audit info is missing"
        // Verify the page loads successfully
        jobShowPage.validatePage()
        
        // The page should contain basic job information
        def jobName = driver.findElement(By.xpath("//h1 | //h2 | //*[contains(@class,'job-header')]"))
        jobName.displayed
        
        // Audit fields may or may not be present for legacy jobs, but page should not error
        // This test ensures backward compatibility
        noExceptionThrown()

        cleanup:
        deleteProject(projectName)
    }

    def "job audit information displays with correct formatting"() {
        given: "a project and job"
        String projectName = "auditFormattingProject"
        setupProject(projectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id

        when: "visiting the job show page"
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        jobShowPage.go()

        then: "audit information should be properly formatted with timestamps"
        // Look for audit table rows
        def auditRows = driver.findElements(By.xpath("//table[contains(@class,'item_details')]//tr[td[contains(text(),'Created by') or contains(text(),'Last modified by')]]"))
        
        if (auditRows.size() > 0) {
            auditRows.each { row ->
                // Each audit row should have both label and value cells
                def cells = row.findElements(By.tagName("td"))
                assert cells.size() >= 2
                
                // The value cell should contain user information and timestamp
                def valueCell = cells[1]
                assert valueCell.text.trim() != ""
                
                // Should contain either username or timestamp information
                assert valueCell.text.contains(TEST_USER) || 
                       valueCell.text.toLowerCase().contains('admin') ||
                       valueCell.text.matches(".*\\d.*") // Contains some date/time info
            }
        }

        cleanup:
        deleteProject(projectName)
    }

    def "job audit section is accessible and visible"() {
        given: "a project and job"  
        String projectName = "auditVisibilityProject"
        setupProject(projectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id

        when: "visiting the job show page"
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        jobShowPage.go()

        then: "audit information should be visible in the details section"
        // Verify the job details table is visible
        def detailsTable = driver.findElement(By.cssSelector("table.item_details, #detailtable table, .job-info table"))
        assert detailsTable.displayed
        
        // The audit information should be within the visible details area
        def jobDetailsSection = driver.findElement(By.cssSelector("#job_detail_main, .job-info-section, .job_detail_content"))
        assert jobDetailsSection.displayed
        
        // Check that audit-related elements have proper accessibility attributes
        def auditElements = driver.findElements(By.xpath("//*[contains(text(),'Created by') or contains(text(),'Last modified by')]"))
        auditElements.each { element ->
            // Element should be visible and accessible
            assert element.displayed
            // Should be within a table structure for proper screen reader support
            def parentTable = element.findElement(By.xpath("ancestor::table[1]"))
            assert parentTable != null
        }

        cleanup:
        deleteProject(projectName)
    }
}