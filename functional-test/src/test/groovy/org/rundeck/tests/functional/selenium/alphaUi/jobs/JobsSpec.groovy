package org.rundeck.tests.functional.selenium.alphaUi.jobs

import org.openqa.selenium.By
import org.rundeck.util.annotations.AlphaUiSeleniumCoreTest
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import spock.lang.Shared

@AlphaUiSeleniumCoreTest
class JobsSpec extends SeleniumBase {
    @Shared
    String projectName
    JobCreatePage jobCreatePage

    def setup() {
        setupEnvironment()
        jobCreatePage = setupJobCreatePage()
    }

    def cleanupSpec() {
        try {
            deleteProject(projectName)
        } catch (Exception e) {
            println "Warning: Final cleanup failed: ${e.message}"
        }
    }

    private void setupEnvironment() {
        projectName = UUID.randomUUID().toString()
        setupProject(projectName)
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    private JobCreatePage setupJobCreatePage() {
        def page = page JobCreatePage, projectName
        page.nextUi = true
        page.go()
        return page
    }

    def "job workflow alphaUi flag enabled"() {
        when:
        jobCreatePage.tab JobTab.WORKFLOW click()
        then:
        jobCreatePage.waitForTextToBePresentBySelector(By.xpath("//section[@id='workflowContent']//div[contains(@class, 'control-label')]"), "Workflow",60)
        expect:
        jobCreatePage.workflowAlphaUiContainer.isDisplayed()
    }

}