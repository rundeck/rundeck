package org.rundeck.tests.functional.selenium.jobs

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.SideBarPage

@SeleniumCoreTest
class JobDeleteSpec extends SeleniumBase {

    /**
     * It validates that the job is shown un the job list page
     * then deletes the job using the UI in the Job Show Page
     * and validates that the job list page has no jobs
     */
    def "delete job"(){
        given:
        def projectName = "deleteJobProject"
        setupProject(projectName)
        def homePage = page HomePage
        def loginPage = page LoginPage
        def jobListPage = page JobListPage
        def sideBarPage = page SideBarPage
        def jobShowPage = page JobShowPage
        def jobName = "exampleJobToDelete"
        def yamlJob = """
                        -
                          project: ${projectName}
                          loglevel: INFO
                          sequence:
                            keepgoing: false
                            strategy: node-first
                            commands:
                            - exec: echo hello there
                          description: ''
                          name: ${jobName}
                        """
        def pathToJob = JobUtils.generateFileToImport(yamlJob, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${projectName}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        jobListPage.go("/project/${projectName}/jobs")
        then: "check for 1 job to be shown in job list page"
        jobListPage.getCountJobList() == 1
        when:
        jobListPage.getJobLink(jobName).click()
        jobShowPage.getJobActionsButtonList().click()
        jobShowPage.getJobDeleteButtons().click()
        jobShowPage.waitForJobDeleteModalToBeShown()
        jobShowPage.getJobDeleteConfirmBy().click()
        then:
        jobListPage.getCountJobList() == 0
        jobListPage.getDeleteAlertMessage().getText().contains("Job was successfully deleted")
        cleanup:
        deleteProject(projectName)
    }
}
