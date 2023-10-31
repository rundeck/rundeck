package org.rundeck.tests.functional.selenium.tests.project

import org.rundeck.tests.functional.selenium.pages.home.HomePage
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.tests.functional.selenium.pages.project.ProjectCreatePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

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
}