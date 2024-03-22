package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.api.responses.jobs.JobDefinition
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.jobs.JobUploadPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.SideBarPage
import spock.lang.Shared

@SeleniumCoreTest
class ExportImportSpec extends SeleniumBase {

    @Shared String SELENIUM_EXPORT_IMPORT_PROJECT

    def setup() {
        SELENIUM_EXPORT_IMPORT_PROJECT = specificationContext.currentIteration.name.tokenize().collect { it.capitalize() }.join()
        setupProject(SELENIUM_EXPORT_IMPORT_PROJECT)
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    /**
     * Test case for exporting and importing a job with options.
     * Verifies the creation, export, deletion, upload, and modification of a job with options.
     *
     * Steps:
     * 1. Setup:
     *    - Initializes necessary pages and variables.
     *
     * 2. Creation of the Job:
     *    - Fills in basic job details, such as the name.
     *    - Adds an option to the job and saves it.
     *    - Creates the job.
     *
     * 3. Verifications After Creation:
     *    - Validates that the job was created successfully.
     *    - Downloads the job definition in YAML format.
     *    - Deletes the newly created job.
     *    - Ensures the job list is empty and contains a success message.
     *
     * 4. Uploading and Verification of Job Load:
     *    - Navigates to the upload page.
     *    - Loads the path for uploading the job definition.
     *    - Validates the upload page.
     *    - Selects the file to upload and uploads it.
     *
     * 5. Verification After Upload:
     *    - Ensures a success message indicating the job was created or modified.
     *
     * 6. Additional Actions and Verifications:
     *    - Navigates to the jobs page.
     *    - Clicks on the newly created job.
     *    - Verifies the presence of additional job options.
     *    - Performs additional actions such as editing the job and verifies option details.
     *
     * Note: SELENIUM_EXPORT_IMPORT_PROJECT is the project used for testing.
     */
    def "export import job with options"() {
        setup:
        def uuid = UUID.randomUUID().toString()
        def jobCreatePage = page JobCreatePage, SELENIUM_EXPORT_IMPORT_PROJECT
        def jobShowPage = page(JobShowPage, SELENIUM_EXPORT_IMPORT_PROJECT).forJob(uuid)
        def jobListPage = page JobListPage
        def jobUploadPage = page JobUploadPage
        def sideBarPage = page SideBarPage
        def jobName = "jobWithOptionsToExport"
        def optName = "firstOption"
        def job = new JobDefinition(
                id: uuid,
                defaultTab: 'nodes',
                description: '',
                executionEnabled: true,
                loglevel: 'INFO',
                name: jobName,
                nodeFilterEditable: false,
                plugins: new JobDefinition.Plugins(),
                options: [new JobDefinition.Option(name: optName)],
                sequence: new JobDefinition.Sequence(commands: [new JobDefinition.Command(exec: 'echo selenium test')],
                        keepgoing: false, strategy: 'node-first'),
                schedule: new JobDefinition.Schedule(),
                uuid: uuid
        )
        when:
        def yaml = JobUtils.generateJobDefinitionYML(job)
        def filePath = JobUtils.generateFileToImport(yaml, "yaml")
        JobUtils.importJob filePath, SELENIUM_EXPORT_IMPORT_PROJECT, client
        then:
        jobShowPage.go()
        jobShowPage.waitForElementVisible jobShowPage.jobUuidBy
        jobShowPage.validatePage()
        jobShowPage.getLink "Action" click()
        jobShowPage.getLink "Download Job definition in YAML" click()
        jobShowPage.getLink "Action" click()
        jobShowPage.getLink "Delete this Job" click()
        jobShowPage.deleteJobBtn.click()
        jobListPage.jobList.isEmpty()
        jobListPage.alertInfo.text.contains("Job was successfully deleted")
        when:
        jobListPage.getLink "Upload a Job definition" click()
        jobUploadPage.loadPathToUploadPage SELENIUM_EXPORT_IMPORT_PROJECT
        jobUploadPage.validatePage()
        jobUploadPage.fileInputElement().sendKeys("${downloadFolder}${getSeparator()}${jobName}.yaml")
        jobUploadPage.fileUploadButtonElement().click()
        then:
        jobUploadPage.headerTextSuccess.text.contains("1 Job was successfully created/modified")
        when:
        sideBarPage.goTo NavLinkTypes.JOBS
        jobListPage.getLink jobName click()
        then:
        jobShowPage.getExtraOptFirsts(optName).size() == 1
        when:
        jobShowPage.getLink "Action" click()
        jobShowPage.getLink "Edit this Job" click()
        jobCreatePage.tab JobTab.WORKFLOW click()
        then:
        jobCreatePage.optDetails.size() == 1
        jobCreatePage.options.size() == 1
    }

    def cleanup() {
        deleteProject(SELENIUM_EXPORT_IMPORT_PROJECT)
    }

}