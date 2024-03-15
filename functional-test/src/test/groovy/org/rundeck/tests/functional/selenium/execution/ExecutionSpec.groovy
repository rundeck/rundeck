package org.rundeck.tests.functional.selenium.execution

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.ativity.ActivityPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.DashboardPage
import org.rundeck.util.gui.pages.project.ProjectEditPage
import org.rundeck.util.gui.pages.project.SideBarPage
import spock.lang.Shared

@SeleniumCoreTest
class ExecutionSpec extends SeleniumBase {

    @Shared String SELENIUM_EXEC_PROJECT = "SeleniumExecProject"

    def setupSpec() {
        setupProject(SELENIUM_EXEC_PROJECT)
    }

    def setup() {
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
            def dashboardPage = page DashboardPage
        then:
            jobCreatePage.fillBasicJob 'executionCleanUpJob'
            jobCreatePage.createJobButton.click()
            jobShowPage.validatePage()
        when:
            jobShowPage.runJobBtn.click()
        then:
            executionShowPage.waitForElementAttributeToChange executionShowPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED'
        when:
            (0..19).each {
                executionShowPage.runAgainButton.click()
                jobShowPage.runJobBtn.click()
                executionShowPage.waitForElementAttributeToChange executionShowPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED'
            }
        then:
            sideBarPage.goTo NavLinkTypes.ACTIVITY
            activityPage.activityRows.size() == 20
            sideBarPage.goTo NavLinkTypes.PROJECT_CONFIG
        when:
            projectEditPage.enableCleanExecutionHistory()
            projectEditPage.configureCleanExecutionHistory(0, 2, "*/10 * * * * ? *")
            projectEditPage.save()
        then:
            dashboardPage.waitForElementVisible dashboardPage.projectSummary
            sideBarPage.goTo NavLinkTypes.ACTIVITY
        when:
            activityPage.implicitlyWait 15
        then:
            activityPage.activityRows.size() == 2
    }

    def cleanup() {
        deleteProject(SELENIUM_EXEC_PROJECT)
        def topMenuPage = page TopMenuPage
        topMenuPage.logOut()
    }

}
