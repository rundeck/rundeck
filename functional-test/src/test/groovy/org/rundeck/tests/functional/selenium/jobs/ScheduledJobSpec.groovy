package org.rundeck.tests.functional.selenium.jobs

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ActivityPage

@SeleniumCoreTest
class ScheduledJobSpec extends SeleniumBase{

    /**
     * Schedules a job and checks if with time passing, executions reflects in activity section.
     *
     */
    def "Scheduled job"(){
        given:
        def projectName = "scheduled-job-test"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        HomePage homePage = page HomePage
        ActivityPage activityPage = page ActivityPage
        def jobUuid = "2de51941-605c-435b-b128-4dfa8142f111"
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        def yaml = """
            - 
              defaultTab: nodes
              description: ''
              executionEnabled: true
              id: ${jobUuid}
              loglevel: INFO
              name: Run job later
              nodeFilterEditable: false
              plugins:
                ExecutionLifecycle: {}
              schedule:
                month: '*'
                time:
                  hour: '*'
                  minute: '*'
                  seconds: '*/5'
                weekday:
                  day: '*'
                year: '*'
              scheduleEnabled: true
              sequence:
                commands:
                - exec: echo selenium test
                - exec: echo asd
                keepgoing: false
                strategy: node-first
              uuid: ${jobUuid}
        """
        def pathToJob = JobUtils.generateFileToImport(yaml, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${projectName}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        jobShowPage.go()
        jobShowPage.waitForElementVisible(jobShowPage.jobUuidBy)
        jobShowPage.validatePage()
        activityPage.loadActivityPageForProject(projectName)
        activityPage.go()
        Thread.sleep(10000) // Auto refresh doesn't work
        activityPage.go()

        then:
        Integer.parseInt(activityPage.executionCount.text) > 0

        cleanup:
        deleteProject(projectName)

    }

}
