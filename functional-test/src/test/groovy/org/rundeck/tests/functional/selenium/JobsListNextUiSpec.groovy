package org.rundeck.tests.functional.selenium

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.JobsListPage
import org.rundeck.tests.functional.selenium.pages.JobsList_nextui_Page
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectHomePage
import org.rundeck.tests.functional.selenium.pages.SideBar
import org.rundeck.tests.functional.selenium.pages.SideBarNavLinks
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import spock.lang.Unroll

@SeleniumCoreTest
class JobsListNextUiSpec extends SeleniumBase {


    public static final String TEST_PROJECT = "SeleniumBasic"

    def setupSpec() {
        setupProject(TEST_PROJECT, "/projects-import/SeleniumBasic.zip")
    }

    @Unroll
    def "view jobs list page"() {
        setup:
            LoginPage loginPage = page LoginPage
            JobsListPage jobListPage = page JobsList_nextui_Page
            jobListPage.project = TEST_PROJECT
        when:
            loginPage.go()
            loginPage.login(TEST_USER, TEST_PASS)

            jobListPage.go()
        then:
            jobListPage.createJobLink.isDisplayed()

            def actionsButton = jobListPage.jobsActionsButton

            actionsButton.isDisplayed()
            actionsButton.getText().contains('Job Actions')
            jobListPage.jobsHeader.isDisplayed()
            jobListPage.activitySectionLink.isDisplayed()
            jobListPage.activityHeader.isDisplayed()
        when:
            actionsButton.click()
        then:
            jobListPage.getLink('Upload Definition').isDisplayed()
            jobListPage.getLink('Bulk Edit').isDisplayed()
    }
}
