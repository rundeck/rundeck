package org.rundeck.tests.functional.selenium.execution

import org.openqa.selenium.By
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.pages.ativity.ActivityPage
import org.rundeck.util.gui.pages.execution.CommandPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectEditPage
import org.rundeck.util.gui.pages.project.SideBarPage
import spock.lang.Shared
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
class ExecutionSpec extends SeleniumBase {

    @Shared String SELENIUM_EXEC_PROJECT

    def setup() {
        SELENIUM_EXEC_PROJECT = specificationContext.currentIteration.name.tokenize().collect { it.capitalize() }.join()
        setupProject(SELENIUM_EXEC_PROJECT)
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def "auto execution clean up"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_EXEC_PROJECT
            def jobShowPage = page JobShowPage
            def executionShowPage = page ExecutionShowPage
            def sideBarPage = page SideBarPage
            def activityPage = page ActivityPage
            def projectEditPage = page ProjectEditPage
        then:
            jobCreatePage.fillBasicJob 'executionCleanUpJob'
            jobCreatePage.createJobButton.click()
            jobShowPage.validatePage()
        when:
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

    def "viewer execution check log view"() {
        setup:
            def jobCreatePage = go JobCreatePage, SELENIUM_EXEC_PROJECT
            def jobShowPage = page JobShowPage
            def executionShowPage = page ExecutionShowPage
        when:
            jobCreatePage.addScriptStep("scriptJob", [
                    "RED='\\033[0;31m'",
                    "\n",
                    "NC='\\033[0m'",
                    "\n",
                    'printf "Hello ${RED}World${NC} rundeck"'
            ])
        then:
            jobShowPage.validatePage()
            jobShowPage.runJobBtn.click()
        when:
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

    def "check running job follow output"() {
        setup:
            def jobCreatePage = go JobCreatePage, SELENIUM_EXEC_PROJECT
            def jobShowPage = page JobShowPage
            def executionShowPage = page ExecutionShowPage
        when:
            def commands = []

            40.times {
                commands << "echo 'Hello world '"
                commands << "\n"
            }

            commands << "sleep 10"
            commands << "\n"
            commands << "echo 'this is my last line'"
            jobCreatePage.addScriptStep("testJob", commands)
        then:
            jobShowPage.validatePage()
            jobShowPage.runJobBtn.click()
        when:
            executionShowPage.validatePage()
            executionShowPage.getLink 'Log Output' click()
        then:
            executionShowPage.waitForTextContainsIsDisplayed(executionShowPage.getTextContains("this is my last line"))
    }

    def "check url with line number"() {
        setup:
            def jobCreatePage = go JobCreatePage, SELENIUM_EXEC_PROJECT
            def jobShowPage = page JobShowPage
            def executionShowPage = page ExecutionShowPage
        when:
            def commands = []

            40.times {
                commands << "echo 'Hello world '"
                commands << "\n"
            }

            commands << "sleep 10"
            commands << "\n"
            commands << "echo 'this is my last line'"
            jobCreatePage.addScriptStep("testJob", commands)
        then:
            jobShowPage.validatePage()
            jobShowPage.runJobBtn.click()
        when:
            executionShowPage.validateStatus 'SUCCEEDED'
            executionShowPage.getLink 'Log Output' click()
            executionShowPage.execLogSettings.click()
            executionShowPage.settingsInputOptions.findAll { !it.isSelected() }.each { it.click() }
        then:
            executionShowPage.refresh()
            executionShowPage.execLogEntryGutters.get(9).click()
            executionShowPage.currentUrl().endsWith("#outputL5")
    }

    def "check url with line number high lighted"() {
        setup:
            def jobCreatePage = go JobCreatePage, SELENIUM_EXEC_PROJECT
            def jobShowPage = page JobShowPage
            def executionShowPage = page ExecutionShowPage
            def sideBarPage = page SideBarPage
        when:
            def commands = []

            40.times {
                commands << "echo 'Hello world '"
                commands << "\n"
            }

            commands << "sleep 10"
            commands << "\n"
            commands << "echo 'this is my last line'"
            jobCreatePage.addScriptStep("testJob", commands)
        then:
            jobShowPage.validatePage()
            jobShowPage.runJobBtn.click()
        when:
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
    }

    def "check after refresh"() {
        setup:
            def jobCreatePage = go JobCreatePage, SELENIUM_EXEC_PROJECT
            def jobShowPage = page JobShowPage
            def executionShowPage = page ExecutionShowPage
        when:
            jobCreatePage.addScriptStep("exampleJob", [
                    "RED='\\033[0;31m'",
                    "\n",
                    "NC='\\033[0m'",
                    "\n",
                    'printf "Hello ${RED}World${NC} rundeck"'
            ])
        then:
            jobShowPage.validatePage()
            jobShowPage.runJobBtn.click()
        when:
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

    def "check every option"() {
        setup:
            def jobCreatePage = go JobCreatePage, SELENIUM_EXEC_PROJECT
            def jobShowPage = page JobShowPage
            def executionShowPage = page ExecutionShowPage
        when:
            jobCreatePage.addScriptStep("exampleJob", [
                    "RED='\\033[0;31m'",
                    "\n",
                    "NC='\\033[0m'",
                    "\n",
                    'printf "Hello ${RED}World${NC} rundeck"'
            ])
        then:
            jobShowPage.validatePage()
            jobShowPage.runJobBtn.click()
        when:
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
            executionShowPage.execLogEntryGutters.isEmpty()
            executionShowPage.gutterLineNumbers.isEmpty()
            executionShowPage.logNodeSettings.isEmpty()
            executionShowPage.stats.isEmpty()
            executionShowPage.logContentTextOverflows.size() == 1
    }

    def "change options while running"() {
        setup:
            def commandPage = go CommandPage, SELENIUM_EXEC_PROJECT
            def jobCreatePage = page JobCreatePage, SELENIUM_EXEC_PROJECT
            def sideBarPage = page SideBarPage
            def activityPage = page ActivityPage
            def executionShowPage = page ExecutionShowPage
            def jobShowPage = page JobShowPage
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
            sideBarPage.goTo NavLinkTypes.JOBS
            jobCreatePage.go()
            jobCreatePage.addScriptStep("exampleJob", [
                    "RED='\\033[0;31m'",
                    "\n",
                    "NC='\\033[0m'",
                    "\n",
                    'printf "Hello ${RED}World${NC} rundeck"'
            ], 5)
        then:
            jobShowPage.validatePage()
            jobShowPage.runJobBtn.click()
        when:
            executionShowPage.validateStatus 'SUCCEEDED'
            executionShowPage.getLink 'Log Output' click()
            executionShowPage.execLogSettings.click()
            executionShowPage.settingsInputOptions.findAll { it.isSelected() }.each { it.click() }
            executionShowPage.settingsOption.click()
        then:
            executionShowPage.execLogGutters.isEmpty()
            executionShowPage.gutterLineNumbers.isEmpty()
            executionShowPage.logNodeSettings.isEmpty()
    }

    def cleanup() {
        deleteProject(SELENIUM_EXEC_PROJECT)
    }

}
