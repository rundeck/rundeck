package org.rundeck.tests.functional.selenium.visual

import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectCreatePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class CreateProjectSpec extends SeleniumBase {

    def "has basic fields"() {
        when:
            def loginPage = page LoginPage
            def page = page ProjectCreatePage
            loginPage.login()

        then:
            currentUrl.contains("/resources/createProject")
            page.getProjectNameInput()
            page.getLabelInput()
            page.getDescriptionInput()
    }
}