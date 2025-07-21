package org.rundeck.tests.functional.selenium.execution

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.openqa.selenium.By
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.pages.activity.ActivityPage
import org.rundeck.util.gui.pages.execution.CommandPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectEditPage
import org.rundeck.util.gui.pages.project.SideBarPage
import spock.lang.Shared

@SeleniumCoreTest
class ExecutionSpec extends SeleniumBase {

    @Shared String SELENIUM_EXEC_PROJECT

    def setup() {
        SELENIUM_EXEC_PROJECT = specificationContext.currentIteration.name.tokenize().collect { it.capitalize() }.join()
        setupProject(SELENIUM_EXEC_PROJECT)
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    /**
     * Verifies the execution cleanup process.
     *
     * Steps:
     * 1. Creation of the Job:
     *    - Creates a job named 'executionCleanUpJob'.
     *    - Runs the job.
     *    - Re-runs the job multiple times.
     * 2. Validation After Creation:
     *    - Verifies the presence of 20 activity rows.
     * 3. Configuration of Clean Execution History:
     *    - Enables the clean execution history feature.
     *    - Configures the cleanup interval and saves the changes.
     * 4. Verification After Configuration:
     *    - Validates that only 2 activity rows remain after the cleanup.
     */
    def "auto execution clean up"() {
        when:
        def executionShowPage = page ExecutionShowPage
        def sideBarPage = page SideBarPage
        def activityPage = page ActivityPage
        def projectEditPage = page ProjectEditPage
        def jobUuid = "2de51941-605c-435b-b128-4dfa8142f142"
        JobShowPage jobShowPage = page(JobShowPage, SELENIUM_EXEC_PROJECT).forJob(jobUuid)
        def yaml = """
            - 
              defaultTab: nodes
              description: ''
              executionEnabled: true
              id: ${jobUuid}
              loglevel: INFO
              name: testJob
              nodeFilterEditable: false
              plugins:
                ExecutionLifecycle: {}
              scheduleEnabled: true
              sequence:
                commands:
                - script: |2-
                    echo 'this is my line'
              keepgoing: false
              strategy: node-first
              uuid: ${jobUuid}
        """
        def pathToJob = JobUtils.generateFileToImport(yaml, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${SELENIUM_EXEC_PROJECT}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        then:
        jobShowPage.go()
        jobShowPage.runJobBtn.click()
        then:
        executionShowPage.validateStatus 'SUCCEEDED'
        when:
        (0..19).each {
            executionShowPage.getLink 'Run Again' click()
            jobShowPage.runJobBtn.click()
            executionShowPage.validateStatus 'SUCCEEDED'
        }
        then:
        sideBarPage.goTo NavLinkTypes.ACTIVITY
        activityPage.activityRows.size() == 20
        sideBarPage.goTo NavLinkTypes.PROJECT_CONFIG
        when:
        projectEditPage.enableCleanExecutionHistory()
        projectEditPage.configureCleanExecutionHistory(0, 2, "*/10 * * * * ? *")
        projectEditPage.save()
        projectEditPage.waitForUrlToContain "project/${SELENIUM_EXEC_PROJECT}/home"
        then:
        hold 15
        sideBarPage.goTo NavLinkTypes.ACTIVITY
        activityPage.activityRows.size() == 2
    }

    /**
     * Verifies the execution of an adhoc command.
     *
     * Steps:
     * - Executes the command 'echo 'Hello world'' and waits for it to succeed.
     * - Validates various elements on the command page, such as the execution content and log gutters.
     */
    def "viewer execution check adhoc page"() {
        when:
        def commandPage = go CommandPage, SELENIUM_EXEC_PROJECT
        commandPage.runCommandAndWaitToBe("echo 'Hello world'", "SUCCEEDED")
        then:
        commandPage.runContent.isDisplayed()
        commandPage.runArgument.text == "echo 'Hello world'"
        commandPage.getExecLogGutters().size() == 2
        commandPage.execLogContent.text == "Hello world"
        commandPage.runningExecState == "SUCCEEDED"
    }

    /**
     * Verifies the log node view during command execution.
     *
     * Steps:
     * - Executes the command 'echo 'Hello world'' and waits for it to succeed.
     * - Navigates to the log node view and validates its contents.
     */
    def "viewer execution check log node view"() {
        setup:
        def commandPage = go CommandPage, SELENIUM_EXEC_PROJECT
        def sideBarPage = page SideBarPage
        def executionShowPage = page ExecutionShowPage
        when:
        commandPage.runCommandAndWaitToBe("echo 'Hello world'", "SUCCEEDED")
        then:
        sideBarPage.goTo NavLinkTypes.COMMANDS
        when:
        commandPage.activityRowAdhoc.click()
        executionShowPage.validatePage()
        executionShowPage.autoCaret.get(0).click()
        then:
        executionShowPage.waitForNumberOfElementsToBe executionShowPage.autoCaretBy, 2
        executionShowPage.autoCaret.get(1).click()
        executionShowPage.execLogNode.isDisplayed()
        executionShowPage.execLogNode.text == "Hello world"
        executionShowPage.execLogGutterEntryAttribute.matches("\\d{2}:\\d{2}:\\d{2}")
    }

    /**
     * Verifies the log view during script execution.
     *
     * Steps:
     * - Creates a script job with colored output.
     * - Runs the job and verifies the output log.
     * - Toggles log settings and verifies the log view changes.
     */
    def "viewer execution check log view"() {
        setup:
        def executionShowPage = page ExecutionShowPage
        def jobUuid = "2de51941-605c-435b-b128-4dfa8142f142"
        JobShowPage jobShowPage = page(JobShowPage, SELENIUM_EXEC_PROJECT).forJob(jobUuid)
        def yaml = """
            - 
              defaultTab: nodes
              description: ''
              executionEnabled: true
              id: ${jobUuid}
              loglevel: INFO
              name: testJob
              nodeFilterEditable: false
              plugins:
                ExecutionLifecycle: {}
              scheduleEnabled: true
              sequence:
                commands:
                - script: |2-
                    RED='\\033[0;31m'
                    NC='\\033[0m'
                    printf "Hello \${RED}World\${NC} rundeck"
              keepgoing: false
              strategy: node-first
              uuid: ${jobUuid}
        """
        def pathToJob = JobUtils.generateFileToImport(yaml, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${SELENIUM_EXEC_PROJECT}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        jobShowPage.go()
        jobShowPage.runJobBtn.click()
        executionShowPage.validateStatus 'SUCCEEDED'
        executionShowPage.viewButtonOutput.click()
        then:
        executionShowPage.execRedColorText.isDisplayed()
        executionShowPage.execRedColorText.getCssValue("color") == "rgba(255, 0, 0, 1)"
        when:
        executionShowPage.execLogSettings.click()
        executionShowPage.settingsInputOptions.findAll { !it.isSelected() }.each { it.click() }
        executionShowPage.closePopupSettingsButton.click()
        then:
        executionShowPage.execRedColorText.isDisplayed()
        executionShowPage.execRedColorText.getCssValue("color") == "rgba(255, 0, 0, 1)"
        when:
        executionShowPage.execLogSettings.click()
        executionShowPage.settingsInputOptions.findAll { !it.isSelected() }.each { it.click() }
        executionShowPage.closePopupSettingsButton.click()
        then:
        executionShowPage.logNodeSetting.isDisplayed()
        executionShowPage.logNodeSettings.size() == 1
    }

    /**
     * Verifies the line wrapping functionality in the log view.
     *
     * Steps:
     * - Executes a command with a long echo statement.
     * - Verifies the presence of line wrapping in the log view.
     */
    def "check line wrap"() {
        setup:
        def commandPage = go CommandPage, SELENIUM_EXEC_PROJECT
        def sideBarPage = page SideBarPage
        def executionShowPage = page ExecutionShowPage
        when:
        def longEcho = "echo '" + (1..20).collect { " Hello World " }.join() + "'"
        commandPage.runCommandAndWaitToBe(longEcho, "SUCCEEDED")
        then:
        sideBarPage.goTo NavLinkTypes.COMMANDS
        commandPage.activityRowAdhoc.click()
        executionShowPage.validatePage()
        executionShowPage.getLink 'Log Output' click()
        when:
        executionShowPage.execLogSettings.click()
        executionShowPage.settingsInputOptions.findAll { !it.isSelected() }.each { it.click() }
        executionShowPage.closePopupSettingsButton.click()
        then:
        executionShowPage.waitForNumberOfElementsToBe executionShowPage.maskSettingsOptionsBy, 0
        executionShowPage.waitForNumberOfElementsToBe executionShowPage.logContentTextBy, 1
    }

    /**
     * Verifies the log output for a long-running job.
     *
     * Steps:
     * - Creates a job with multiple echo commands.
     * - Runs the job and verifies the log output.
     */
    def "check running job follow output"() {
        setup:
        def executionShowPage = page ExecutionShowPage
        def jobUuid = "2de51941-605c-435b-b128-4dfa8142f140"
        JobShowPage jobShowPage = page(JobShowPage, SELENIUM_EXEC_PROJECT).forJob(jobUuid)
        def yaml = """
            - 
              defaultTab: nodes
              description: ''
              executionEnabled: true
              id: ${jobUuid}
              loglevel: INFO
              name: testJob
              nodeFilterEditable: false
              plugins:
                ExecutionLifecycle: {}
              scheduleEnabled: true
              sequence:
                commands:
                - script: |2-
                    for i in {1..41}
                    do
                    echo 'Hello world'
                    done
                    sleep 10
                    echo 'this is my last line'
              keepgoing: false
              strategy: node-first
              uuid: ${jobUuid}
        """
        def pathToJob = JobUtils.generateFileToImport(yaml, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${SELENIUM_EXEC_PROJECT}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        jobShowPage.go()
        jobShowPage.runJobBtn.click()
        then:
        executionShowPage.validatePage()
        when:
        executionShowPage.getLink 'Log Output' click()
        then:
        executionShowPage.validateStatus("SUCCEEDED")
        executionShowPage.execLogGutters.text.size() > 39
        executionShowPage.waitForLineTobeShownNumberOfTimes("this is my last line", 3)
    }

    /**
     * Verifies the URL behavior with line numbers in the log view.
     *
     * Steps:
     * - Creates a job with multiple echo commands.
     * - Runs the job and navigates to the log output.
     * - Checks the URL with line numbers and verifies its correctness.
     */
    def "check url with line number"() {
        setup:
        def executionShowPage = page ExecutionShowPage
        def jobUuid = "2de51941-605c-435b-b128-4dfa8142f141"
        JobShowPage jobShowPage = page(JobShowPage, SELENIUM_EXEC_PROJECT).forJob(jobUuid)
        def yaml = """
            - 
              defaultTab: nodes
              description: ''
              executionEnabled: true
              id: ${jobUuid}
              loglevel: INFO
              name: testJob
              nodeFilterEditable: false
              plugins:
                ExecutionLifecycle: {}
              scheduleEnabled: true
              sequence:
                commands:
                - script: |2-
                    for i in {1..41}
                    do
                    echo 'Hello world'
                    done
              keepgoing: false
              strategy: node-first
              uuid: ${jobUuid}
        """
        def pathToJob = JobUtils.generateFileToImport(yaml, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${SELENIUM_EXEC_PROJECT}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        jobShowPage.go()
        jobShowPage.validatePage()
        jobShowPage.runJobBtn.click()
        then:
        executionShowPage.validateStatus 'SUCCEEDED'
        executionShowPage.getLink 'Log Output' click()
        executionShowPage.execLogSettings.click()
        executionShowPage.settingsInputOptions.findAll { !it.isSelected() }.each { it.click() }
        then:
        executionShowPage.refresh()
        executionShowPage.execLogEntryGutters.get(9).click()
        executionShowPage.currentUrl().endsWith("#outputL5")
    }

    /**
     * Verifies the URL behavior with highlighted line numbers in the log view.
     *
     * Steps:
     * - Creates a job with multiple echo commands.
     * - Runs the job and navigates to the log output.
     * - Verifies the URL with highlighted line numbers.
     */
    def "check url with line number high lighted"() {
        setup:
        def executionShowPage = page ExecutionShowPage
        def sideBarPage = page SideBarPage
        def jobUuid = "2de51941-605c-435b-b128-4dfa8142f143"
        JobShowPage jobShowPage = page(JobShowPage, SELENIUM_EXEC_PROJECT).forJob(jobUuid)
        def yaml = """
            - 
              defaultTab: nodes
              description: ''
              executionEnabled: true
              id: ${jobUuid}
              loglevel: INFO
              name: testJob
              nodeFilterEditable: false
              plugins:
                ExecutionLifecycle: {}
              scheduleEnabled: true
              sequence:
                commands:
                - script: |2-
                    for i in {1..41}
                    do
                    echo 'Hello world'
                    done
                    sleep 10
                    echo 'this is my last line'
              keepgoing: false
              strategy: node-first
              uuid: ${jobUuid}
        """
        def pathToJob = JobUtils.generateFileToImport(yaml, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${SELENIUM_EXEC_PROJECT}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        jobShowPage.go()
        jobShowPage.runJobBtn.click()
        executionShowPage.validateStatus 'SUCCEEDED'
        executionShowPage.getLink 'Log Output' click()
        executionShowPage.waitForElementVisible executionShowPage.execLogEntryGutters.get(9) click()
        then:
        def url = executionShowPage.currentUrl()
        assert url.endsWith("#outputL5")
        when:
        executionShowPage.execLogEntryGutters.get(9).click()
        sideBarPage.goTo NavLinkTypes.JOBS
        executionShowPage.redirectTo url
        then:
        executionShowPage.waitForElementVisible executionShowPage.execLogLines.get(4)
        executionShowPage.execLogLines.get(4).getAttribute("class") == "execution-log__line execution-log__line--selected"
        executionShowPage.execLogLines.text.any { it == "Hello world" }
    }

    /**
     * Verifies the log view settings persistence after refreshing the page.
     *
     * Steps:
     * - Creates a script job with colored output.
     * - Runs the job and navigates to the log output.
     * - Toggles log settings, refreshes the page, and verifies settings persistence.
     */
    def "check after refresh"() {
        setup:
        def executionShowPage = page ExecutionShowPage
        def jobUuid = "2de51941-605c-435b-b128-4dfa8142f142"
        JobShowPage jobShowPage = page(JobShowPage, SELENIUM_EXEC_PROJECT).forJob(jobUuid)
        def yaml = """
            - 
              defaultTab: nodes
              description: ''
              executionEnabled: true
              id: ${jobUuid}
              loglevel: INFO
              name: testJob
              nodeFilterEditable: false
              plugins:
                ExecutionLifecycle: {}
              scheduleEnabled: true
              sequence:
                commands:
                - script: |2-
                    RED='\\033[0;31m'
                    NC='\\033[0m'
                    printf "Hello \${RED}World\${NC} rundeck"
              keepgoing: false
              strategy: node-first
              uuid: ${jobUuid}
        """
        def pathToJob = JobUtils.generateFileToImport(yaml, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${SELENIUM_EXEC_PROJECT}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        jobShowPage.go()
        jobShowPage.runJobBtn.click()
        executionShowPage.validateStatus 'SUCCEEDED'
        executionShowPage.getLink 'Log Output' click()
        executionShowPage.execLogSettings.click()
        def inputSelection = [:]
        executionShowPage.settingsInputOptions.findAll { !it.isSelected()}.each {
            it.click()
            inputSelection[it.getAttribute("id")] = it.isSelected()
        }
        executionShowPage.refresh()
        executionShowPage.execLogSettings.click()
        executionShowPage.waitForElementVisible executionShowPage.settingsOptionsBy
        then:
        inputSelection.each { key, value ->
            executionShowPage.el(By.id(key as String)).isSelected() == value
        }
    }

    /**
     * Verifies every option in the log view settings.
     *
     * Steps:
     * - Creates a script job with colored output.
     * - Runs the job and navigates to the log output.
     * - Toggles all log view settings options and verifies their effects.
     */
    def "check every option"() {
        setup:
        def executionShowPage = page ExecutionShowPage
        def jobUuid = "2de51941-605c-435b-b128-4dfa8142f142"
        JobShowPage jobShowPage = page(JobShowPage, SELENIUM_EXEC_PROJECT).forJob(jobUuid)
        def yaml = """
            - 
              defaultTab: nodes
              description: ''
              executionEnabled: true
              id: ${jobUuid}
              loglevel: INFO
              name: testJob
              nodeFilterEditable: false
              plugins:
                ExecutionLifecycle: {}
              scheduleEnabled: true
              sequence:
                commands:
                - script: |2-
                    RED='\\033[0;31m'
                    NC='\\033[0m'
                    printf "Hello \${RED}World\${NC} rundeck"
              keepgoing: false
              strategy: node-first
              uuid: ${jobUuid}
        """
        def pathToJob = JobUtils.generateFileToImport(yaml, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${SELENIUM_EXEC_PROJECT}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        jobShowPage.go()
        jobShowPage.runJobBtn.click()
        executionShowPage.validateStatus 'SUCCEEDED'
        executionShowPage.getLink 'Log Output' click()
        executionShowPage.execLogSettings.click()
        executionShowPage.settingsInputOptions.findAll { !it.isSelected() }.each { it.click() }
        executionShowPage.refresh()
        then:
        executionShowPage.gutterLineNumber.isDisplayed()
        !executionShowPage.getExecLogGutterEntryAttribute(0).isEmpty()
        !executionShowPage.getExecLogGutterEntryAttribute(1).isEmpty()
        executionShowPage.logNodeSetting.isDisplayed()
        executionShowPage.stat.isDisplayed()
        executionShowPage.logContentText.isDisplayed()
        when:
        executionShowPage.execLogSettings.click()
        executionShowPage.settingsInputOptions.findAll {
            it.isSelected() && it.getAttribute("id") != "logview_gutter"
        }.each { it.click() }
        executionShowPage.closePopupSettingsButton.click()
        then:
        executionShowPage.execLogEntryGutters.size() == 0
        executionShowPage.gutterLineNumbers.size() == 0
        executionShowPage.logNodeSettings.size() == 0
        executionShowPage.stats.size() == 0
        executionShowPage.logContentTextOverflows.size() == 1
    }

    /**
     * Verifies the ability to change log view settings while a job is running.
     *
     * Steps:
     * - Executes an adhoc command and navigates to the log output.
     * - Changes log view settings while the command is running and verifies their effects.
     */
    def "change options while running"() {
        setup:
        def commandPage = go CommandPage, SELENIUM_EXEC_PROJECT
        def sideBarPage = page SideBarPage
        def activityPage = page ActivityPage
        def executionShowPage = page ExecutionShowPage
        def jobUuid = "2de51941-605c-435b-b128-4dfa8142f145"
        JobShowPage jobShowPage = page(JobShowPage, SELENIUM_EXEC_PROJECT).forJob(jobUuid)
        def yaml = """
            - 
              defaultTab: nodes
              description: ''
              executionEnabled: true
              id: ${jobUuid}
              loglevel: INFO
              name: testJob
              nodeFilterEditable: false
              plugins:
                ExecutionLifecycle: {}
              scheduleEnabled: true
              sequence:
                commands:
                - script: |2-
                    RED='\\033[0;31m'
                    NC='\\033[0m'
                    printf "Hello \${RED}World\${NC} rundeck"
              keepgoing: false
              strategy: node-first
              uuid: ${jobUuid}
        """
        def pathToJob = JobUtils.generateFileToImport(yaml, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${SELENIUM_EXEC_PROJECT}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        commandPage.runCommandAndWaitToBe("echo 'Hello world'", "SUCCEEDED")
        sideBarPage.goTo NavLinkTypes.ACTIVITY
        activityPage.timeAbs.click()
        executionShowPage.getLink 'Log Output' click()
        then:
        executionShowPage.execLogSettings.click()
        executionShowPage.settingsInputOptions.findAll { !it.isSelected() }.each { it.click() }
        executionShowPage.closePopupSettingsButton.click()
        when:
        jobShowPage.go()
        jobShowPage.runJobBtn.click()
        executionShowPage.validateStatus 'SUCCEEDED'
        executionShowPage.getLink 'Log Output' click()
        executionShowPage.execLogSettings.click()
        executionShowPage.settingsInputOptions.findAll { it.isSelected() }.each { it.click() }
        executionShowPage.settingsOption.click()
        then:
        executionShowPage.execLogGutters.size() == 0
        executionShowPage.gutterLineNumbers.size() == 0
        executionShowPage.logNodeSettings.size() == 0
    }

    def cleanup() {
        deleteProject(SELENIUM_EXEC_PROJECT)
    }

}