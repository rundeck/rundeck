package org.rundeck.tests.functional.selenium

import org.openqa.selenium.By
import org.rundeck.tests.functional.selenium.pages.ProjectListPage
import org.rundeck.tests.functional.selenium.pages.ProjectListNextUiPage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import spock.lang.Unroll

@SeleniumCoreTest
class ProjectListNextUiSpec extends SeleniumBase {
    public static final String TEST_PROJECT = "SeleniumBasic"

    def setupSpec() {
        setupProject(TEST_PROJECT, "/projects-import/SeleniumBasic.zip")
    }

    @Unroll
    def "view project list page"() {
        setup:
            ProjectListPage listPage = page ProjectListPage
            LoginPage loginPage = page LoginPage
            ProjectListNextUiPage projectListNextUiPage = page ProjectListNextUiPage
        when:
            loginPage.go()
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            listPage.validatePage()

        when:
            projectListNextUiPage.go()
        then:
            //validate ui type next
            verifyAll {
                driver.currentUrl.contains('nextUi=true')
            }

            projectListNextUiPage.getFirstRunCreateBtn().isDisplayed()
    }
}
