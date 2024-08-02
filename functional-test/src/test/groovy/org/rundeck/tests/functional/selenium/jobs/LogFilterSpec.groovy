package org.rundeck.tests.functional.selenium.jobs

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class LogFilterSpec extends SeleniumBase{

    /**
     * It adds a highlight log filter to the step and then it checks for the CSS to be present
     */
    def "job step filter highlight"(){
        given:
        def projectName = "stepLogFilterProject"
        def jobUuid = "b37751f9-4d20-42c2-984f-212a615f4ba2"
        def yaml = """
                        - defaultTab: nodes
                          description: ''
                          executionEnabled: true
                          id: ${jobUuid}
                          loglevel: INFO
                          name: stepLogFilter
                          nodeFilterEditable: false
                          plugins:
                            ExecutionLifecycle: {}
                          scheduleEnabled: true
                          sequence:
                            commands:
                            - exec: echo "This is an example for highlight filter"
                              plugins:
                                LogFilter:
                                - config:
                                    bgcolor: green
                                    fgcolor: yellow
                                    mode: bold
                                    regex: This is an example
                                  type: highlight-output
                            keepgoing: false
                            strategy: node-first
                          uuid: ${jobUuid}
                    """
        setupProject(projectName)
        def homePage = page HomePage
        def loginPage = page LoginPage
        def jobShowPage = page JobShowPage
        def executionShowPage = page ExecutionShowPage
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
        jobShowPage.go("/project/${projectName}/job/show/${jobUuid}")
        jobShowPage.getRunJobBtn().click()
        executionShowPage.getViewButtonOutput().click()
        then:
        executionShowPage.getLogOutput().size() == 1
        executionShowPage.getLogOutput()[0].getText() == "This is an example for highlight filter"
        executionShowPage.getElementByCss(".ansi-bg-green.ansi-fg-yellow.ansi-mode-bold").isDisplayed()
        executionShowPage.getElementByCss(".ansi-bg-green.ansi-fg-yellow.ansi-mode-bold").getText() == "This is an example"
        cleanup:
        deleteProject(projectName)

    }
}
