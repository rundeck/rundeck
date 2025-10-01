package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.NodeSourcePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class NodesSpec extends SeleniumBase {

    /**
     * Use an isolated project copied from SELENIUM_BASIC_PROJECT, so other suites can't
     * modify the same project during CI.
     */
    static final String NODES_TEST_PROJECT =
            (System.getenv('NODES_TEST_PROJECT') ?: "${SELENIUM_BASIC_PROJECT}-Nodes-${System.currentTimeMillis()}")

    def setupSpec() {
        // Copy/seed from the standard archive used elsewhere
        setupProjectArchiveDirectoryResource(NODES_TEST_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def "go to edit nodes"() {
        setup:
            def loginPage = go LoginPage
            def nodeSourcePage = page NodeSourcePage
            def menuPage = page HomePage
        when:
            loginPage.login(TEST_USER, TEST_PASS)
            menuPage.validatePage()
            menuPage.goProjectHome(NODES_TEST_PROJECT)
            nodeSourcePage.loadPath = "/project/${NODES_TEST_PROJECT}/nodes/sources"
            nodeSourcePage.go("/project/${NODES_TEST_PROJECT}/nodes/sources")

        then:
            nodeSourcePage.waitForElementVisible nodeSourcePage.newNodeSourceButton
            nodeSourcePage.newNodeSourceButton != null
            nodeSourcePage.newNodeSourceButton.getText().contains("Source")
    }

    def "create node source and press save"() {
        setup:
            def loginPage      = go LoginPage
            def nodeSourcePage = page NodeSourcePage
            def menuPage       = page HomePage

        when: "navigate to Nodes → Sources"
            loginPage.login(TEST_USER, TEST_PASS)
            menuPage.validatePage()
            menuPage.goProjectHome(NODES_TEST_PROJECT)
            nodeSourcePage.forProject(NODES_TEST_PROJECT)
            nodeSourcePage.go()

        then: "open picker"
            nodeSourcePage.waitForElementVisible nodeSourcePage.newNodeSourceButton
            nodeSourcePage.clickAddNewNodeSource()

        when: "choose Local provider"
            nodeSourcePage.chooseProviderByName("Local")

        and: "click page-level Save"
            nodeSourcePage.clickSaveNodeSources()

        then: "either toast shows OR the unsaved banner disappears"
            nodeSourcePage.waitForSavedState()
            nodeSourcePage.waitForPageReady()
    }
}
