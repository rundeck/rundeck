package org.rundeck.tests.functional.selenium.alphaUi.jobs

import org.openqa.selenium.By
import org.rundeck.util.annotations.AlphaUiSeleniumCoreTest
import org.rundeck.util.gui.pages.jobs.JobCreatePage
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
//    def "job workflow alphaUi"() {
//        when:
//        def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
//        jobCreatePage.nextUi=true
//        jobCreatePage.go()
//        then:
//        jobCreatePage.tab JobTab.WORKFLOW click()
//        jobCreatePage.waitForTextToBePresentBySelector(By.xpath("//section[@id='workflowContent2']//div[contains(@class, 'control-label')]"), "Workflow",60)
//        expect:
//        jobCreatePage.workflowAlphaUiContainer.isDisplayed()
//    }
}
