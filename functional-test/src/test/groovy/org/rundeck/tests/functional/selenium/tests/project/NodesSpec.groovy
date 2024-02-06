package org.rundeck.tests.functional.selenium.tests.project

import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.tests.functional.selenium.pages.project.NodeSourcePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class NodesSpec extends SeleniumBase {

    def setupSpec() {
        setupProject(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}.zip")
    }

    def "go to edit nodes"() {
        setup:
            def loginPage = go LoginPage
            def nodeSourcePage = page NodeSourcePage
        when:
            loginPage.login(TEST_USER, TEST_PASS)
            nodeSourcePage.loadPath = "/project/${SELENIUM_BASIC_PROJECT}/nodes/sources"
            nodeSourcePage.go("/project/${SELENIUM_BASIC_PROJECT}/nodes/sources")
        then:
            nodeSourcePage.waitForElementVisible nodeSourcePage.newNodeSourceButton
            nodeSourcePage.newNodeSourceButton != null
            nodeSourcePage.newNodeSourceButton.getText().contains("Source")
    }

}
