package org.rundeck.tests.functional.selenium.tests.project

import org.rundeck.tests.functional.selenium.pages.home.HomePage
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.tests.functional.selenium.pages.project.NodeSourcePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class NodesSpec extends SeleniumBase {

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def "go to edit nodes"() {
        setup:
            def loginPage = go LoginPage
            def nodeSourcePage = page NodeSourcePage
            def menuPage = page HomePage
        when:
            loginPage.login(TEST_USER, TEST_PASS)
            menuPage.validatePage()
            menuPage.goProjectHome(SELENIUM_BASIC_PROJECT)
            nodeSourcePage.loadPath = "/project/${SELENIUM_BASIC_PROJECT}/nodes/sources"
            nodeSourcePage.go("/project/${SELENIUM_BASIC_PROJECT}/nodes/sources")

        then:
            nodeSourcePage.waitForElementVisible nodeSourcePage.newNodeSourceButton
            nodeSourcePage.newNodeSourceButton != null
            nodeSourcePage.newNodeSourceButton.getText().contains("Source")
    }

}
