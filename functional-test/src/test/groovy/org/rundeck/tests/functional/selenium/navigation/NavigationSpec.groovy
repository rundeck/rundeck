package org.rundeck.tests.functional.selenium.navigation

import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.appadmin.SystemConfigurationPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.SideBarPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
class NavigationSpec extends SeleniumBase {

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup(){
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def "visits jobs"() {
        setup:
            def homePage = page HomePage
            def sideBarPage = page SideBarPage
        when:
            homePage.goProjectHome SELENIUM_BASIC_PROJECT
            sideBarPage.goTo NavLinkTypes.JOBS
        then:
            sideBarPage.waitForUrlToContain('/jobs')
    }

    def "visits nodes"() {
        setup:
            def homePage = page HomePage
            def sideBarPage = page SideBarPage
        when:
            homePage.goProjectHome SELENIUM_BASIC_PROJECT
            sideBarPage.goTo NavLinkTypes.NODES
        then:
            sideBarPage.waitForUrlToContain('/nodes')
    }

    def "visits commands"() {
        setup:
            def homePage = page HomePage
            def sideBarPage = page SideBarPage
        when:
            homePage.goProjectHome SELENIUM_BASIC_PROJECT
            sideBarPage.goTo NavLinkTypes.COMMANDS
        then:
            sideBarPage.waitForUrlToContain('/command')
    }

    def "visits activity"() {
        setup:
            def homePage = page HomePage
            def sideBarPage = page SideBarPage
        when:
            homePage.goProjectHome SELENIUM_BASIC_PROJECT
            sideBarPage.goTo NavLinkTypes.ACTIVITY
        then:
            sideBarPage.waitForUrlToContain('/activity')
    }

    def "visits System Configuration"() {
        setup:
            def topMenuPage = page TopMenuPage
            def systemConfigurationPage = page SystemConfigurationPage
        when:
            topMenuPage.navigateToSystemConfiguration()
        then:
            systemConfigurationPage.validatePage()
    }
}
