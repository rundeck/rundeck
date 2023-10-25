package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.HomePage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectCreatePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class NodesSpec extends SeleniumBase {

    public static final String TEST_USER = System.getenv("RUNDECK_TEST_USER") ?: "admin"
    public static final String TEST_PASS = System.getenv("RUNDECK_TEST_PASS") ?: "admin"

    def "go to edit nodes"() {
        when:
            def loginPage = page LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.createProject()
        then:
            def projectCreatePage = page ProjectCreatePage
            projectCreatePage.createProject(toCamelCase specificationContext.currentFeature.name)
            'Add a new Node Source' == projectCreatePage.newNodeSourceButton.getText()
        cleanup:
            projectCreatePage.deleteProject()
            projectCreatePage.waitForModal 1
            projectCreatePage.validatePage()
    }

}
