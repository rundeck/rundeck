package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.NodeSourcePage
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

    /**
     * Minimal flow: create a node source (Local), then press page-level Save.
     * Consider it a success if either a success toast appears OR the sources API responds OK.
     */
    def "create node source and press save"() {
        setup:
            def loginPage      = go LoginPage
            def nodeSourcePage = page NodeSourcePage
            def menuPage       = page HomePage

        when: "navigate to Nodes → Sources"
            loginPage.login(TEST_USER, TEST_PASS)
            menuPage.validatePage()
            menuPage.goProjectHome(SELENIUM_BASIC_PROJECT)
            nodeSourcePage.forProject(SELENIUM_BASIC_PROJECT)
            nodeSourcePage.go()

        then: "open picker"
            nodeSourcePage.waitForElementVisible nodeSourcePage.newNodeSourceButton
            nodeSourcePage.clickAddNewNodeSource()

        when: "choose Local provider"
            nodeSourcePage.chooseProviderPreferLocal()

        and: "click page-level Save"
            nodeSourcePage.clickSaveNodeSources()

        then: "toast or API confirms"
            nodeSourcePage.waitForSaveToastOrRefresh()
            nodeSourcePage.waitForPageReady()
    }
}
