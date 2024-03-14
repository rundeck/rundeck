package org.rundeck.tests.functional.selenium.jobs

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class JobEditSpec extends SeleniumBase{

    /**
     * It add and remove steps from a job and verifies
     */
    def "add and remove steps"(){
        given:
            def projectName = "addRemoveStepsProject"
            setupProject(projectName)
            def loginPage = page LoginPage
            def homePage = page HomePage
            def jobListPage = page JobListPage
            def jobShowPage = page JobShowPage
            def jobCreatePage = page JobCreatePage
            def jobName = "test-job"
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
            jobListPage.getLink(jobName).click()
            jobShowPage.validatePage()
            jobShowPage.getJobActionDropdownButton().click()
            jobShowPage.getEditJobLink().click()
            jobCreatePage.tab(JobTab.WORKFLOW).click()
            jobCreatePage.addSimpleCommandStepButton.click()
            jobCreatePage.addSimpleCommandStep 'echo selenium test 2', 1
            jobCreatePage.getUpdateJobButton().click()
        then:
            jobCreatePage.waitForUrlToContain('/job/show')
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.expectNumberOfStepsToBe(2)

        when:
            jobShowPage.closeDefinitionModalButton.click()
            jobShowPage.jobActionDropdownButton.click()
            jobShowPage.getEditJobLink().click()
            jobCreatePage.tab(JobTab.WORKFLOW).click()
            jobCreatePage.removeStepByIndex(1)
            hold(2)
            jobCreatePage.expectNumberOfStepsToBe(1)
            jobCreatePage.getUpdateJobButton().click()
        then:
            jobCreatePage.waitForUrlToContain('/job/show')
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.expectNumberOfStepsToBe(1)
        cleanup:
            deleteProject(projectName)
    }
}
