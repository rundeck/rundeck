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
class KeyValueDataLogFilterSpec extends SeleniumBase{

    /**
     * Runs a job with 2 steps and a step log filter set on the first step.
     * Checks that the second steps has the right captured value from the first step.
     */
    def "job step log filter with basic capture"(){
        given:
        def projectName = UUID.randomUUID().toString()
        setupProject(projectName)
        def homePage = page HomePage
        def loginPage = page LoginPage
        def jobShowPage = page JobShowPage
        def executionShowPage = page ExecutionShowPage
        def jobName = "globalLogFilterJob"
        def jobUuid = UUID.randomUUID().toString()
        def yamlJob = """
                        - defaultTab: nodes
                          description: ''
                          executionEnabled: true
                          id: ${jobUuid}
                          loglevel: INFO
                          name: ${jobName}
                          nodeFilterEditable: false
                          plugins:
                            ExecutionLifecycle: {}
                          scheduleEnabled: true
                          schedules: []
                          sequence:
                            commands:
                            - exec: echo "FIRST=data pass example from first step"
                              plugins:
                                LogFilter:
                                - config:
                                    invalidKeyPattern: \\s|\\\$|\\{|\\}|\\\\
                                    logData: 'false'
                                    regex: ^(FIRST|SECOND)\\s*=\\s*(.+)\$
                                    replaceFilteredResult: 'false'
                                  type: key-value-data
                            - exec: 'echo "data passed: \${data.FIRST}"'
                            keepgoing: false
                            strategy: node-first
                          uuid: ${jobUuid}
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
        jobShowPage.go("/project/${projectName}/job/show/${jobUuid}")
        jobShowPage.getRunJobBtn().click()
        executionShowPage.getViewButtonOutput().click()
        then:
        executionShowPage.getLogOutput().size() == 2
        executionShowPage.getLogOutput()[0].getText() == "FIRST=data pass example from first step"
        executionShowPage.getLogOutput()[1].getText() == "data passed: data pass example from first step"
        cleanup:
        deleteProject(projectName)
    }

    /**
     * Runs a job with 2 steps and a global log filter.
     * Checks that the second steps has the right replacement value.
     */
    def "global log filter with advanced capture and replacement"(){
        given:
        def projectName = UUID.randomUUID().toString()
        setupProject(projectName)
        def homePage = page HomePage
        def loginPage = page LoginPage
        def jobShowPage = page JobShowPage
        def executionShowPage = page ExecutionShowPage
        def jobName = "globalLogFilterJob"
        def jobUuid = UUID.randomUUID().toString()
        def yamlJob = """
                        - defaultTab: nodes
                          description: ''
                          executionEnabled: true
                          id: ${jobUuid}
                          loglevel: INFO
                          name: ${jobName}
                          nodeFilterEditable: false
                          plugins:
                            ExecutionLifecycle: {}
                          scheduleEnabled: true
                          schedules: []
                          sequence:
                            commands:
                            - exec: echo "A B=data pass example from first step"
                            - exec: 'echo "data passed: \${data.AXB}"'
                            keepgoing: false
                            pluginConfig:
                              LogFilter:
                              - config:
                                  invalidCharactersReplacement: X
                                  invalidKeyPattern: \\s|\\\$|\\{|\\}|\\\\
                                  logData: 'false'
                                  regex: ^(A\\s*B)\\s*=\\s*(.+)\$
                                  replaceFilteredResult: 'true'
                                type: key-value-data
                            strategy: node-first
                          uuid: ${jobUuid}
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
        jobShowPage.go("/project/${projectName}/job/show/${jobUuid}")
        jobShowPage.getRunJobBtn().click()
        executionShowPage.getViewButtonOutput().click()
        then:
        executionShowPage.getLogOutput().size() == 3
        executionShowPage.getLogOutput()[0].getText() == "Key contains not valid value which will be replaced"
        executionShowPage.getLogOutput()[1].getText() == "A B=data pass example from first step"
        executionShowPage.getLogOutput()[2].getText() == "data passed: data pass example from first step"
        cleanup:
        deleteProject(projectName)
    }
}
