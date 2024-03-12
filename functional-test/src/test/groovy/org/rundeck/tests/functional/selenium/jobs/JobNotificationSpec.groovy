package org.rundeck.tests.functional.selenium.jobs

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class JobNotificationSpec extends SeleniumBase{

    /**
     * It only checks for the notification to be shown in the job edit page
     */
    def "check notification in job edit page"(){
        given:
        def projectName = "notificationProject"
        setupProject(projectName)
        def jobUuid = "91dc46f4-1894-4c2f-9d50-7e30a6861c72"
        def loginPage = page LoginPage
        def homePage = page HomePage
        def jobCreatePage = page JobCreatePage
        def yaml = """
        - defaultTab: nodes
          description: ''
          executionEnabled: true
          id: ${jobUuid}
          loglevel: INFO
          name: jobNotification
          nodeFilterEditable: false
          notification:
            onsuccess:
              email:
                attachType: file
                recipients: admin@rundeck.example
          notifyAvgDurationThreshold: null
          plugins:
            ExecutionLifecycle: {}
          scheduleEnabled: true
          sequence:
            commands:
            - exec: echo test
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
        jobCreatePage.loadEditPath(projectName, jobUuid)
        jobCreatePage.go()
        jobCreatePage.tab(JobTab.NOTIFICATIONS).click()
        then:
        jobCreatePage.getNotificationList().size() == 1
        jobCreatePage.getNotificationChilds(jobCreatePage.getNotificationList().get(0)).get(0).getText() == "admin@rundeck.example"
        cleanup:
        deleteProject(projectName)

    }
}
