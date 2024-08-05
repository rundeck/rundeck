package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.execution.HtmlRenderedOutputPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class JobTabsSpec extends SeleniumBase {

    static final String PROJECT_NAME = 'job_nodes_tab'

    def setupSpec() {
        startEnvironment()
    }

    void setup() {
        def loginPage = go(LoginPage)
        loginPage.login(TEST_USER, TEST_PASS)
        setupProject(PROJECT_NAME)
    }

    def cleanup() {
        deleteProject(PROJECT_NAME)
    }

    void "job nodes tab"() {
        when:
        final String option = 'nodes'
        def jobShowPage = page(JobShowPage)
        def executionShowPage = page(ExecutionShowPage)
        then:
        def jobCreatePage = go(JobCreatePage, PROJECT_NAME)
        jobCreatePage.jobNameInput.sendKeys("test-output-tab")
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.addSimpleCommandStep('echo "hello world"', 0)
        jobCreatePage.createJobButton.click()
        jobShowPage.jobActionDropdownButton.click()
        jobShowPage.waitForElementToBeClickable(jobShowPage.editJobLink)
        jobShowPage.editJobLink.click()
        jobCreatePage.tab(JobTab.OTHER).click()
        jobCreatePage.defaultTabNodes.click()
        jobCreatePage.updateBtn.click()
        jobShowPage.runJobBtn.click()
        def nodeViewContainer = executionShowPage.nodeFlowState
        expect:
        currentUrl.endsWith(option)
        executionShowPage.viewButtonOutput.getAttribute("style") == ""
        nodeViewContainer.isDisplayed()
        executionShowPage.waitForElementAttributeToChange(executionShowPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED')
    }

    void "job log output tab"() {
        when:
        def jobShowPage = page(JobShowPage)
        def executionShowPage = page(ExecutionShowPage)
        then:
        def jobCreatePage = go(JobCreatePage, PROJECT_NAME)
        jobCreatePage.jobNameInput.sendKeys("test-output-tab")
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.addSimpleCommandStep('echo "hello world"', 0)
        jobCreatePage.createJobButton.click()
        jobShowPage.jobActionDropdownButton.click()
        jobShowPage.waitForElementToBeClickable(jobShowPage.editJobLink)
        jobShowPage.editJobLink.click()
        jobCreatePage.tab(JobTab.OTHER).click()
        jobCreatePage.defaultTabOutput.click()
        jobCreatePage.updateBtn.click()
        jobShowPage.runJobBtn.click()
        def nodeViewContainer = executionShowPage.nodeFlowState
        expect:
        currentUrl.endsWith("output")
        executionShowPage.viewButtonOutput.getAttribute("style") == "display: none;"
        !nodeViewContainer.isDisplayed()
        executionShowPage.waitForElementAttributeToChange(executionShowPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED')
    }

    void "job log html tab"() {
        when:
        def commandArg = "hello world"
        def command = "echo"
        def fullCommand = "${command} ${commandArg}"
        def jobShowPage = page(JobShowPage)
        def executionShowPage = page(ExecutionShowPage)
        def htmlOutputPage = page HtmlRenderedOutputPage
        then:
        def jobCreatePage = go(JobCreatePage, PROJECT_NAME)
        jobCreatePage.jobNameInput.sendKeys("test-html-output")
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.addSimpleCommandStep(fullCommand, 0)
        jobCreatePage.createJobButton.click()
        jobShowPage.jobActionDropdownButton.click()
        jobShowPage.waitForElementToBeClickable(jobShowPage.editJobLink)
        jobShowPage.editJobLink.click()
        jobCreatePage.tab(JobTab.OTHER).click()
        jobCreatePage.defaultTabHtml.click()
        jobCreatePage.updateBtn.click()
        jobShowPage.runJobBtn.click()
        executionShowPage.waitForElementVisible(htmlOutputPage.logLevelNormalBy)
        expect:
        htmlOutputPage.logLevelNormalLogLine.text == commandArg
        currentUrl.endsWith("convertContent=on&loglevels=on&ansicolor=on&reload=true")
    }
}