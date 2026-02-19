package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
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

    def setupSpec() {
        SELENIUM_EXPORT_IMPORT_PROJECT = "ExportImportSpec-" + System.currentTimeMillis()
        setupProject(SELENIUM_EXPORT_IMPORT_PROJECT)
    }
    def setup(){
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
        def jobCreatePage = go JobCreatePage, SELENIUM_EXPORT_IMPORT_PROJECT
        def jobShowPage = page JobShowPage
        def jobListPage = page JobListPage
        def jobUploadPage = page JobUploadPage
        jobUploadPage.loadPathToUploadPage SELENIUM_EXPORT_IMPORT_PROJECT
        jobUploadPage.legacyUi = legacyUi
        def sideBarPage = page SideBarPage
        def jobName = "jobWithOptionsToExport-nextui-${legacyUi}"
        def optName = "firstOption"
        when:
        jobCreatePage.fillBasicJob jobName
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys optName
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 0
        jobCreatePage.createJobButton.click()
        jobUploadPage.implicitlyWait(2000)
        then:
        jobShowPage.validatePage()
        jobShowPage.getLink "Action" click()
        jobShowPage.getLink "Download Job definition in YAML" click()
        jobShowPage.getLink "Action" click()
        jobShowPage.getLink "Delete this Job" click()
        jobShowPage.deleteJobBtn.click()
        jobUploadPage.implicitlyWait(2000)
        jobListPage.jobList.isEmpty()
        jobListPage.alertInfo.text.contains("Job was successfully deleted")
        when:
        jobUploadPage.go()
        jobUploadPage.implicitlyWait(2000)
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
        where:
        legacyUi<< [true, false]
    }
    def "import job with skip should show skip message"() {
        setup:
        def jobCreatePage = go JobCreatePage, SELENIUM_EXPORT_IMPORT_PROJECT
        def jobShowPage = page JobShowPage
        def jobUploadPage = page JobUploadPage
        jobUploadPage.loadPathToUploadPage SELENIUM_EXPORT_IMPORT_PROJECT
        jobUploadPage.legacyUi = legacyUi
        def jobName = 'ImportJobWithSkip-nextui-'+ legacyUi

        when: "create basic job"
        jobCreatePage.fillBasicJob jobName
        jobCreatePage.createJobButton.click()
        jobUploadPage.implicitlyWait(2000)

        then: "job created"
        jobShowPage.validatePage()
        jobShowPage.getLink "Action" click()
        jobShowPage.getLink "Download Job definition in YAML" click()

        when: "upload job with dupe option skip"
        jobUploadPage.go()
        jobUploadPage.implicitlyWait(2000)
        jobUploadPage.fileInputElement().sendKeys("${downloadFolder}${getSeparator()}${jobName}.yaml")
        jobUploadPage.dupeOptionSkip().click()
        jobUploadPage.fileUploadButtonElement().click()
        jobUploadPage.implicitlyWait(2000)
        then:

        jobUploadPage.headerTextInfo.text.contains("skipped due to existing jobs")
        where:
        legacyUi<< [true, false]

    }

    def cleanupSpec() {
        deleteProject(SELENIUM_EXPORT_IMPORT_PROJECT)
    }

}