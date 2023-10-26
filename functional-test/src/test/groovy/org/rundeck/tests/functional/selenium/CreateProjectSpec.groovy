package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.HomePage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectCreatePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class CreateProjectSpec extends SeleniumBase {

    def "has basic fields"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.createProjectButton()
        then:
            currentUrl.contains("/resources/createProject")
            def projectCreatePage = page ProjectCreatePage
        expect:
            projectCreatePage.projectNameInput != null
            projectCreatePage.labelInput != null
            projectCreatePage.descriptionInput != null
    }
}