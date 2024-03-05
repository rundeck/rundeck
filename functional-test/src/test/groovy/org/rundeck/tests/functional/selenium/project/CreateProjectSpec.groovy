package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectCreatePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class CreateProjectSpec extends SeleniumBase {

    def "create project has basic fields"() {
        when:
            def loginPage = go LoginPage
            def homePage = page HomePage
            def projectCreatePage = page ProjectCreatePage
        then:
            loginPage.login(TEST_USER, TEST_PASS)
            homePage.createProjectButton()
        expect:
            currentUrl.contains("/resources/createProject")
            projectCreatePage.projectNameInput
            projectCreatePage.labelInput
            projectCreatePage.descriptionInput
    }
}