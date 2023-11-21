package org.rundeck.tests.functional.selenium.tests.execution

import org.rundeck.tests.functional.selenium.pages.execution.CommandPage
import org.rundeck.tests.functional.selenium.pages.execution.ExecutionShowPage
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class CommandSpec extends SeleniumBase {

    def setupSpec() {
        setupProject("SeleniumBasic", "/projects-import/SeleniumBasic.zip")
    }

    def "abort button in commands page"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def commandPage = go CommandPage, "SeleniumBasic"
        then:
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.commandTextField.sendKeys "echo running test && sleep 45"
            commandPage.runButton.click()
            commandPage.waitForElementVisible commandPage.runningExecutionStateButton
            commandPage.runningExecutionStateButton.getAttribute("data-execstate") == 'RUNNING'
        expect:
            commandPage.abortButton.click()
            sleep 3000
            commandPage.runningExecutionStateButton.getAttribute("data-execstate") == 'ABORTED'
    }

    def "abort button in show page"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def commandPage = go CommandPage, "SeleniumBasic"
        then:
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.commandTextField.sendKeys "echo running test && sleep 45"
            commandPage.runButton.click()
            sleep 1000
            commandPage.runningButtonLink().click()
        expect:
            def executionShowPage = page ExecutionShowPage
            executionShowPage.executionStateDisplayLabel.getAttribute('data-execstate') == 'RUNNING'

            executionShowPage.abortButton.click()
            sleep 3000
            executionShowPage.executionStateDisplayLabel.getAttribute('data-execstate') == 'ABORTED'
    }

    def "default page load shows nodes view"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def commandPage = go CommandPage, "SeleniumBasic"
        then:
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.commandTextField.sendKeys "echo running test '" + this.class.name + "'"
            commandPage.runButton.click()
            sleep 1000
            def href = commandPage.runningButtonLink().getAttribute("href")
            commandPage.driver.get href
        expect:
            def executionShowPage = page ExecutionShowPage
            executionShowPage.validatePage()
            executionShowPage.executionStateDisplayLabel.getAttribute('data-execstate') != null
            executionShowPage.viewContentNodes.isDisplayed()
            !executionShowPage.viewButtonNodes.isDisplayed()
            executionShowPage.viewButtonOutput.isDisplayed()
    }

    def "fragment #output page load shows output view"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def commandPage = go CommandPage, "SeleniumBasic"
        then:
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.commandTextField.sendKeys "echo running test '" + this.class.name + "'"
            commandPage.runButton.click()
            sleep 1000
            def href = commandPage.runningButtonLink().getAttribute("href")
            commandPage.driver.get href + "#output"
        expect:
            def executionShowPage = page ExecutionShowPage
            executionShowPage.validatePage()
            executionShowPage.executionStateDisplayLabel.getAttribute('data-execstate') != null
            executionShowPage.viewContentOutput.isDisplayed()
            executionShowPage.viewButtonNodes.isDisplayed()
            !executionShowPage.viewButtonOutput.isDisplayed()
    }

    def "output view toggle to nodes view with button"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def commandPage = go CommandPage, "SeleniumBasic"
        then:
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.commandTextField.sendKeys "echo running test '" + this.class.name + "'"
            commandPage.runButton.click()
            sleep 1000
            def href = commandPage.runningButtonLink().getAttribute("href")
            commandPage.driver.get href + "#output"
        expect:
            def executionShowPage = page ExecutionShowPage
            executionShowPage.validatePage()
            executionShowPage.viewButtonNodes.isDisplayed()
            executionShowPage.viewButtonNodes.click()

            executionShowPage.viewContentNodes.isDisplayed()
            !executionShowPage.viewButtonNodes.isDisplayed()
            executionShowPage.viewButtonOutput.isDisplayed()
    }

    def "nodes view toggle to output view with button"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def commandPage = go CommandPage, "SeleniumBasic"
        then:
            commandPage.nodeFilterTextField.sendKeys".*"
            commandPage.filterNodeButton.click()
            commandPage.commandTextField.sendKeys "echo running test '" + this.class.name + "'"
            commandPage.runButton.click()
            sleep 1000
            def href = commandPage.runningButtonLink().getAttribute("href")
            commandPage.driver.get href
        expect:
            def executionShowPage = page ExecutionShowPage
            executionShowPage.validatePage()
            executionShowPage.viewButtonOutput.isDisplayed()
            executionShowPage.viewButtonOutput.click()

            executionShowPage.viewContentOutput.isDisplayed()
            !executionShowPage.viewButtonOutput.isDisplayed()
            executionShowPage.viewButtonNodes.isDisplayed()
    }

}
