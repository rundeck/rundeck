package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.HomePage
import org.rundeck.tests.functional.selenium.pages.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.JobListPage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectCreatePage
import org.rundeck.tests.functional.selenium.pages.SideBarPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.setup.NavLinkTypes

@SeleniumCoreTest
class CreateProjectSpec extends SeleniumBase {

    def setupSpec(){
        setupProject("SeleniumBasic", "/projects-import/SeleniumBasic.zip")
    }

    def "create project has basic fields"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.createProjectButton()
        then:
            currentUrl.contains("/resources/createProject")
            def projectCreatePage = page ProjectCreatePage
        expect:
            projectCreatePage.projectNameInput
            projectCreatePage.labelInput
            projectCreatePage.descriptionInput
    }

    def "create job has basic fields"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.goProjectHome"SeleniumBasic"
        when:
            def sideBarPage = page SideBarPage
            sideBarPage.goTo NavLinkTypes.JOBS
        then:
            sleep 5000
            def jobListPage = page JobListPage
            jobListPage.newJobButton.click()
        expect:
            currentUrl.contains("/job/create")
            def jobCreatePage = page JobCreatePage
            jobCreatePage.jobNameInput
            jobCreatePage.groupPathInput
            jobCreatePage.descriptionTextarea
    }
}