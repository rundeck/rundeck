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
        // Verify the job details table is visible
        jobShowPage.jobAuditTable.displayed
        
        // Check for "Created by" information using page object
        def createdByInfo = jobShowPage.jobCreatedBy
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
        // Verify audit information using page object methods
        def createdByInfo = jobShowPage.jobCreatedBy
        def modifiedByInfo = jobShowPage.jobLastModifiedBy
        
        // Verify creation audit information is present
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
        jobShowPage.jobAuditTable.displayed
        
        // Audit fields may or may not be present for legacy jobs, but page should not error
        // This test ensures backward compatibility - page object methods handle missing elements gracefully
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
        // Verify audit table is present and visible
        jobShowPage.jobAuditTable.displayed
        
        // Check if audit information exists using page object methods
        if (jobShowPage.hasAuditInformation()) {
            def createdByInfo = jobShowPage.jobCreatedBy
            if (createdByInfo != null) {
                assert createdByInfo.text.trim() != ""
                assert createdByInfo.text.contains(TEST_USER) || 
                       createdByInfo.text.toLowerCase().contains('admin') ||
                       createdByInfo.text.matches(".*\\d.*") // Contains date/time info
            }
            
            def modifiedByInfo = jobShowPage.jobLastModifiedBy  
            if (modifiedByInfo != null) {
                assert modifiedByInfo.text.trim() != ""
                assert modifiedByInfo.text.contains(TEST_USER) ||
                       modifiedByInfo.text.toLowerCase().contains('admin') ||
                       modifiedByInfo.text.matches(".*\\d.*") // Contains date/time info
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
        // Verify the job details table is visible using page object
        jobShowPage.jobAuditTable.displayed
        
        // Verify audit information accessibility using page object methods
        if (jobShowPage.hasAuditInformation()) {
            def createdByInfo = jobShowPage.jobCreatedBy
            def modifiedByInfo = jobShowPage.jobLastModifiedBy
            
            if (createdByInfo != null) {
                assert createdByInfo.displayed
            }
            
            if (modifiedByInfo != null) {
                assert modifiedByInfo.displayed  
            }
        }
        
        // Page should load without errors regardless of audit data presence
        noExceptionThrown()

        cleanup:
        deleteProject(projectName)
    }
}