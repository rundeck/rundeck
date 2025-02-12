package org.rundeck.tests.functional.selenium.alphaUi.jobs

import org.openqa.selenium.By
import org.rundeck.util.annotations.AlphaUiSeleniumCoreTest
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage

@AlphaUiSeleniumCoreTest
class JobsSpec extends SeleniumBase {
    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    /**
     * Checks that new workflow tab is active
     */
    def "job workflow alphaUi"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        then:
        jobCreatePage.tab JobTab.WORKFLOW click()
        jobCreatePage.waitForTextToBePresentBySelector(By.xpath("//section[@id='workflowContent']//div[contains(@class, 'control-label')]"), "Workflow",60)
        expect:
        jobCreatePage.workflowAlphaUiContainer.isDisplayed()
    }

    def "Duplicate option create form next ui"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        def optName = 'test'
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys optName
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.executeScript "window.location.hash = '#workflowKeepGoingFail'"
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 0

        jobCreatePage.duplicateButton( optName, 0) click()


        then: "create form is shown"
        jobCreatePage.optionNameNew() displayed
        jobCreatePage.optionNameNew().getAttribute('value') == optName + '_copy'
        jobCreatePage.saveOptionButton.displayed
        when:
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 1

        then:
        jobCreatePage.optionNameSaved 0 getText() equals optName
        jobCreatePage.optionNameSaved 1 getText() equals optName + '_copy'

        where:
        nextUi = true
    }

    def "Create option form next ui"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        def optName = 'test'
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
        jobCreatePage.optionButton.click()

        then: "create form is shown"
        jobCreatePage.optionNameNew() displayed
        jobCreatePage.saveOptionButton.displayed
        where:
        nextUi = true
    }

    def "create valid job basic options"() {
        when:
        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
        jobCreatePage.nextUi=true
        jobCreatePage.go()
        def jobShowPage = page JobShowPage
        def optionName = 'seleniumOption1'
        then:
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys optionName
        jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi 0
        jobCreatePage.createJobButton.click()
        then:
        jobCreatePage.waitForUrlToContain('/job/show')
        jobShowPage.jobLinkTitleLabel.getText().contains('create valid job basic options')
        jobShowPage.optionInputText(optionName) != null
    }
}
