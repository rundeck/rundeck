package org.rundeck.tests.functional.selenium.visual

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
class CreateSpec extends SeleniumBase {

    def "Create Project has basic fields"() {
        when:
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        def homePage = page HomePage
        homePage.createProjectButton()
        def projectCreatePage = page ProjectCreatePage

        then:
        currentUrl.contains("/resources/createProject")
        projectCreatePage.getProjectNameInput()
        projectCreatePage.getLabelInput()
        projectCreatePage.getDescriptionInput()
    }

    def "Create Job has basic fields"() {
        when:
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        def homePage = go HomePage
        homePage.createProjectButton()

        def projectCreatePage = page ProjectCreatePage
        projectCreatePage.createProject(toCamelCase specificationContext.currentFeature.name)

        def sideBarPage = page SideBarPage
        sideBarPage.goTo NavLinkTypes.JOBS
        sleep 5000
        def jobListPage = page JobListPage
        jobListPage.createJobButton.click()
        def jobCreatePage = page JobCreatePage

        then:
        currentUrl.contains("/job/create")
        jobCreatePage.getJobNameInput()
        jobCreatePage.getGroupPathInput()
        jobCreatePage.getDescriptionTextarea()
    }
}