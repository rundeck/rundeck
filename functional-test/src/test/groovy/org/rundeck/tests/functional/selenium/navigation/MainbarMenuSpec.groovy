package org.rundeck.tests.functional.selenium.navigation

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.pages.MainbarMenuPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.SideBarPage

/**
 * Verifies that the top mainbar's system config menu (gear icon) and user menu
 * (user icon) are rendered by Vue on the pages most likely to expose UiSocket
 * mounting regressions: home, project home, and project configuration.
 */
@SeleniumCoreTest
class MainbarMenuSpec extends SeleniumBase {

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def "sys config and user menus are visible on the home page"() {
        setup:
            def mainbarPage = page MainbarMenuPage
        when:
            go HomePage
        then:
            mainbarPage.isSysConfigMenuDisplayed()
            mainbarPage.isUserMenuDisplayed()
    }

    def "sys config and user menus are visible on a project home page"() {
        setup:
            def homePage = page HomePage
            def mainbarPage = page MainbarMenuPage
        when:
            homePage.goProjectHome SELENIUM_BASIC_PROJECT
        then:
            mainbarPage.isSysConfigMenuDisplayed()
            mainbarPage.isUserMenuDisplayed()
    }

    def "sys config and user menus are visible on the project configuration page"() {
        setup:
            def homePage = page HomePage
            def sideBarPage = page SideBarPage
            def mainbarPage = page MainbarMenuPage
        when:
            homePage.goProjectHome SELENIUM_BASIC_PROJECT
            sideBarPage.goTo NavLinkTypes.PROJECT_CONFIG
        then:
            mainbarPage.isSysConfigMenuDisplayed()
            mainbarPage.isUserMenuDisplayed()
    }

    def "clicking the sys config toggle opens its dropdown"() {
        setup:
            def mainbarPage = page MainbarMenuPage
        when:
            go HomePage
            mainbarPage.clickSysConfigMenu()
        then:
            mainbarPage.isSysConfigDropdownOpen()
    }

    def "clicking the user menu toggle opens its dropdown"() {
        setup:
            def mainbarPage = page MainbarMenuPage
        when:
            go HomePage
            mainbarPage.clickUserMenu()
        then:
            mainbarPage.isUserMenuDropdownOpen()
    }
}
