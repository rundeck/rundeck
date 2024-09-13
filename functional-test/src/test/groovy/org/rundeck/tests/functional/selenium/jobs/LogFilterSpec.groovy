package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class LogFilterSpec extends SeleniumBase{

    String projectName

    def setup() {
        projectName = UUID.randomUUID().toString()
        setupProject(projectName)

        def loginPage = page LoginPage
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)

        def homePage = page HomePage
        homePage.go()
    }

    def cleanup() {
        deleteProject(projectName)
    }

    /**
     * It adds a highlight log filter to the step and then it checks for the CSS to be present
     */
    def "job step filter highlight"(){
        given:
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
        def jobShowPage = page JobShowPage
        def executionShowPage = page ExecutionShowPage
        def pathToJob = JobUtils.generateFileToImport(yaml, "yaml")
        JobUtils.jobImportYamlFile(projectName, pathToJob, client)
        when:
        jobShowPage.go("/project/${projectName}/job/show/${jobUuid}")
        jobShowPage.getRunJobBtn().click()
        executionShowPage.getViewButtonOutput().click()
        then:
        executionShowPage.getLogOutput().size() == 1
        executionShowPage.getLogOutput()[0].getText() == "This is an example for highlight filter"
        executionShowPage.getElementByCss(".ansi-bg-green.ansi-fg-yellow.ansi-mode-bold").isDisplayed()
        executionShowPage.getElementByCss(".ansi-bg-green.ansi-fg-yellow.ansi-mode-bold").getText() == "This is an example"
    }

    def "job step log filter with basic capture"(){
        given:
        def jobShowPage = page JobShowPage
        def executionShowPage = page ExecutionShowPage
        def jobUuid = UUID.randomUUID().toString()
        def jobName = jobUuid

        // A job with 2 steps and a step log filter set on the first step.
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
        JobUtils.jobImportYamlFile(projectName, pathToJob, client)
        when:
        jobShowPage.go("/project/${projectName}/job/show/${jobUuid}")
        jobShowPage.getRunJobBtn().click()
        executionShowPage.getViewButtonOutput().click()

        then: "Checks that the second steps has the right captured value from the first step."
        executionShowPage.getLogOutput().size() == 2
        executionShowPage.getLogOutput()[0].getText() == "FIRST=data pass example from first step"
        executionShowPage.getLogOutput()[1].getText() == "data passed: data pass example from first step"
    }

    def "global log filter with advanced capture and replacement"(){
        given:
        def jobShowPage = page JobShowPage
        def executionShowPage = page ExecutionShowPage
        def jobUuid = UUID.randomUUID().toString()
        def jobName = jobUuid

        //  A job with 2 steps and a global log filter
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
        JobUtils.jobImportYamlFile(projectName, pathToJob, client)
        when:
        jobShowPage.go("/project/${projectName}/job/show/${jobUuid}")
        jobShowPage.getRunJobBtn().click()
        executionShowPage.getViewButtonOutput().click()

        then: "Checks that the second steps has the right replacement value"
        executionShowPage.getLogOutput().size() == 3
        executionShowPage.getLogOutput()[0].getText() == "Key contains not valid value which will be replaced"
        executionShowPage.getLogOutput()[1].getText() == "A B=data pass example from first step"
        executionShowPage.getLogOutput()[2].getText() == "data passed: data pass example from first step"
    }

    def "global log filter with a multiline regex data capture"(){
        given:
        def jobShowPage = page JobShowPage
        def executionShowPage = page ExecutionShowPage
        def jobUuid = UUID.randomUUID().toString()
        def jobName = jobUuid

        //  A job with 2 steps and a global log filter
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
                            - exec: echo 123
                            - exec: 'echo "\${data.test_capture_result}"'
                            keepgoing: false
                            pluginConfig:
                              LogFilter:
                              - config:
                                  captureMultipleKeysValues: 'true'
                                  hideOutput: 'false'
                                  logData: 'false'
                                  name: test_capture_result
                                  regex: \\s*(.+)
                                type: key-value-data-multilines
                            strategy: node-first
                          uuid: ${jobUuid}
                        """
        def pathToJob = JobUtils.generateFileToImport(yamlJob, "yaml")
        JobUtils.jobImportYamlFile(projectName, pathToJob, client)
        when:
        jobShowPage.go("/project/${projectName}/job/show/${jobUuid}")
        jobShowPage.getRunJobBtn().click()
        executionShowPage.getViewButtonOutput().click()

        then: "Checks that the second steps has the right replacement value"
        executionShowPage.getLogOutput().size() >= 2
        executionShowPage.getLogOutput()[0].getText() == "123"
        executionShowPage.getLogOutput()[1].getText() == "123"
    }

    def "step log filter with a multiline regex data capture"(){
        given:
        def jobShowPage = page JobShowPage
        def executionShowPage = page ExecutionShowPage
        def jobUuid = UUID.randomUUID().toString()
        def jobName = jobUuid

        //  A job with 2 steps and a  log filter on the first step.
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
                            - exec: echo 123
                              plugins:
                                LogFilter:
                                - config:
                                    captureMultipleKeysValues: 'false'
                                    hideOutput: 'false'
                                    logData: 'false'
                                    name: test_capture_result
                                    regex: \\s*(.+)
                                  type: key-value-data-multilines
                            - exec: 'echo "\${data.test_capture_result}"'
                            keepgoing: false
                            strategy: node-first
                          uuid: ${jobUuid}
                        """
        def pathToJob = JobUtils.generateFileToImport(yamlJob, "yaml")
        JobUtils.jobImportYamlFile(projectName, pathToJob, client)
        when:
        jobShowPage.go("/project/${projectName}/job/show/${jobUuid}")
        jobShowPage.getRunJobBtn().click()
        executionShowPage.getViewButtonOutput().click()

        then: "Checks that the second steps has the right replacement value"
        executionShowPage.getLogOutput().size() >= 2
        executionShowPage.getLogOutput()[0].getText() == "123"
        executionShowPage.getLogOutput()[1].getText() == "123"
    }
}

