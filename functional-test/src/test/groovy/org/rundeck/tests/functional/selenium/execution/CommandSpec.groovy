package org.rundeck.tests.functional.selenium.execution

import org.rundeck.util.gui.pages.execution.CommandPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class CommandSpec extends SeleniumBase {

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def "abort button in commands page"() {
        when:
            def commandPage = go CommandPage, SELENIUM_BASIC_PROJECT
        then:
            commandPage.nodeFilterTextField.click()
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.waitForElementToBeClickable commandPage.commandTextField
            commandPage.commandTextField.click()
            commandPage.waitForElementAttributeToChange commandPage.commandTextField, 'disabled', null
            commandPage.commandTextField.sendKeys "echo running test && sleep 45"
            commandPage.runButton.click()
            commandPage.waitForElementAttributeToChange commandPage.runningExecutionStateButton, 'data-execstate', 'RUNNING'
        expect:
            commandPage.abortButton.click()
            commandPage.waitForElementAttributeToChange commandPage.runningExecutionStateButton, 'data-execstate', 'ABORTED'
    }

    def "abort button in show page"() {
        when:
            def commandPage = go CommandPage, SELENIUM_BASIC_PROJECT
            def executionShowPage = page ExecutionShowPage
        then:
            commandPage.nodeFilterTextField.click()
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.waitForElementToBeClickable commandPage.commandTextField
            commandPage.commandTextField.click()
            commandPage.waitForElementAttributeToChange commandPage.commandTextField, 'disabled', null
            commandPage.commandTextField.sendKeys "echo running test && sleep 45"
            commandPage.runButton.click()
            commandPage.runningButtonLink().click()
        expect:
            executionShowPage.waitForElementAttributeToChange executionShowPage.executionStateDisplayLabel, 'data-execstate', 'RUNNING'
            executionShowPage.abortButton.click()
            executionShowPage.waitForElementAttributeToChange executionShowPage.executionStateDisplayLabel, 'data-execstate', 'ABORTED'
    }

    def "default page load shows nodes view"() {
        when:
            def commandPage = go CommandPage, SELENIUM_BASIC_PROJECT
            def executionShowPage = page ExecutionShowPage
        then:
            commandPage.nodeFilterTextField.click()
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.waitForElementToBeClickable commandPage.commandTextField
            commandPage.commandTextField.click()
            commandPage.waitForElementAttributeToChange commandPage.commandTextField, 'disabled', null
            commandPage.commandTextField.sendKeys "echo running test '" + this.class.name.toString() + "'"
            commandPage.runButton.click()
            def href = commandPage.runningButtonLink().getAttribute("href")
            commandPage.driver.get href
        expect:
            executionShowPage.validatePage()
            executionShowPage.waitForElementAttributeToChange executionShowPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED'
            executionShowPage.viewContentNodes.isDisplayed()
            !executionShowPage.viewButtonNodes.isDisplayed()
            executionShowPage.viewButtonOutput.isDisplayed()
    }

    def "fragment #output page load shows output view"() {
        when:
            def commandPage = go CommandPage, SELENIUM_BASIC_PROJECT
            def executionShowPage = page ExecutionShowPage
        then:
            commandPage.nodeFilterTextField.click()
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.waitForElementToBeClickable commandPage.commandTextField
            commandPage.commandTextField.click()
            commandPage.waitForElementAttributeToChange commandPage.commandTextField, 'disabled', null
            commandPage.commandTextField.sendKeys "echo running test '" + this.class.name.toString() + "'"
            commandPage.runButton.click()
            def href = commandPage.runningButtonLink().getAttribute("href")
            commandPage.driver.get href + "#output"
        expect:
            executionShowPage.validatePage()
            executionShowPage.waitForElementAttributeToChange executionShowPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED'
            executionShowPage.viewContentOutput.isDisplayed()
            executionShowPage.viewButtonNodes.isDisplayed()
            !executionShowPage.viewButtonOutput.isDisplayed()
    }

    def "output view toggle to nodes view with button"() {
        when:
            def commandPage = go CommandPage, SELENIUM_BASIC_PROJECT
            def executionShowPage = page ExecutionShowPage
        then:
            commandPage.nodeFilterTextField.click()
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.waitForElementToBeClickable commandPage.commandTextField
            commandPage.commandTextField.click()
            commandPage.waitForElementAttributeToChange commandPage.commandTextField, 'disabled', null
            commandPage.commandTextField.sendKeys "echo running test '" + this.class.name.toString() + "'"
            commandPage.runButton.click()
            def href = commandPage.runningButtonLink().getAttribute("href")
            commandPage.driver.get href + "#output"
        expect:
            executionShowPage.validatePage()
            executionShowPage.viewButtonNodes.isDisplayed()
            executionShowPage.viewButtonNodes.click()

            executionShowPage.viewContentNodes.isDisplayed()
            !executionShowPage.viewButtonNodes.isDisplayed()
            executionShowPage.viewButtonOutput.isDisplayed()
    }

    def "nodes view toggle to output view with button"() {
        when:
            def commandPage = go CommandPage, SELENIUM_BASIC_PROJECT
            def executionShowPage = page ExecutionShowPage
        then:
            commandPage.nodeFilterTextField.click()
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.waitForElementToBeClickable commandPage.commandTextField
            commandPage.commandTextField.click()
            commandPage.waitForElementAttributeToChange commandPage.commandTextField, 'disabled', null
            commandPage.commandTextField.sendKeys "echo running test '" + this.class.name.toString() + "'"
            commandPage.runButton.click()
            def href = commandPage.runningButtonLink().getAttribute("href")
            commandPage.driver.get href
        expect:
            executionShowPage.validatePage()
            executionShowPage.viewButtonOutput.isDisplayed()
            executionShowPage.viewButtonOutput.click()

            executionShowPage.viewContentOutput.isDisplayed()
            !executionShowPage.viewButtonOutput.isDisplayed()
            executionShowPage.viewButtonNodes.isDisplayed()
    }

    def "save as job button saves the command as a job"() {
        when:
        def commandPage = go CommandPage, SELENIUM_BASIC_PROJECT
        def jobCreatePage = page JobCreatePage
        def jobShowPage = page  JobShowPage
        then:
        commandPage.nodeFilterTextField.click()
        commandPage.nodeFilterTextField.sendKeys".*"
        commandPage.filterNodeButton.click()
        commandPage.waitForElementToBeClickable commandPage.commandTextField
        commandPage.commandTextField.click()
        commandPage.waitForElementAttributeToChange commandPage.commandTextField, 'disabled', null
        commandPage.commandTextField.sendKeys "echo running test '" + this.class.name.toString() + "'"
        commandPage.runButton.click()
        commandPage.saveAsJobButton.click()
        jobCreatePage.getJobNameInput().sendKeys("From Command")
        jobCreatePage.saveJob()
        expect:
        jobShowPage.validatePage()
    }

}
