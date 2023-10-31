package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.HomePage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectPage
import org.rundeck.tests.functional.selenium.pages.SideBarPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.setup.NavLinkTypes

@SeleniumCoreTest
class NodesSpec extends SeleniumBase {

    def setupSpec() {
        setupProject("SeleniumBasic", "/projects-import/SeleniumBasic.zip")
    }

    def "go to edit nodes"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.goProjectHome"SeleniumBasic"
        when:
            def sideBarPage = page SideBarPage
            sideBarPage.goTo NavLinkTypes.EDIT_NODES
            def projectPage = page ProjectPage
        then:
            'Add a new Node Source' == projectPage.newNodeSourceButton.getText()
    }

}
