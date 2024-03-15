package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobEditPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectCreatePage


@SeleniumCoreTest
class JobTabsSpec extends SeleniumBase {

    void setup() {
        def loginPage = go(LoginPage)
        loginPage.login(TEST_USER, TEST_PASS)
    }

    void "job nodes tab"() {
        when:
            String projectName = "job_nodes_tab"
            def homePage = page(HomePage)
            def projectCreatePage = page(ProjectCreatePage)
            def jobShowPage = page(JobShowPage)
            def jobEditPage = page(JobEditPage)
            def executionShowPage = page(ExecutionShowPage)
        then:
            homePage.createProjectButton()
            projectCreatePage.createProject(projectName)
            def jobCreatePage = go(JobCreatePage, projectName)
            jobCreatePage.fillBasicJob('jobNodesTab')
            jobCreatePage.addSimpleCommandStepButton.click()
            jobCreatePage.addSimpleCommandStep('echo "hello world"', 1)
            jobCreatePage.createJobButton.click()
            jobShowPage.jobActionDropdownButton.click()
            jobShowPage.waitForElementToBeClickable(jobShowPage.editJobLink)
            jobShowPage.editJobLink.click()
            jobCreatePage.tab(JobTab.OTHER).click()
            jobCreatePage.defaultTabNodes.click()
            jobEditPage.editButton.click()
            jobShowPage.runJobBtn.click()
        expect:
            executionShowPage.validatePage()
            executionShowPage.waitForElementAttributeToChange(executionShowPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED')
        cleanup:
            deleteProject(projectName)
    }
}
