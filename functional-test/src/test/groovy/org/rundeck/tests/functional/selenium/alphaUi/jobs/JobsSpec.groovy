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

    def "job workflow nextUi flag enabled"() {
        when:
        jobCreatePage.tab JobTab.WORKFLOW click()
        then:
        // Grails 7: NextUI workflow uses Vue components, check for workflow section
        jobCreatePage.waitForElementVisible(By.cssSelector("section#workflowContent"))
        expect:
        // Verify NextUI workflow UI is loaded by checking for add step button
        jobCreatePage.waitForElementVisible(By.cssSelector("[data-test='add-step']"))
    }

}