package org.rundeck.tests.functional.selenium.settings

import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.settings.SettingsBarPage

/**
 * Tests for Settings Bar and NextUI mode functionality including:
 * - Settings bar visibility
 * - Settings modal behavior
 * - Theme selection
 * - NextUI cookie-based preference
 */
@SeleniumCoreTest
class SettingsBarSpec extends SeleniumBase {

    static final String PROJECT_NAME = "SettingsBarTestProject"

    def setupSpec() {
        setupProject(PROJECT_NAME)
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
    }

    def "Settings buttons are visible in utility bar"() {
        when:
        def homePage = go HomePage
        def settingsBarPage = page SettingsBarPage

        then:
        settingsBarPage.settingsBarButtons.size() > 0
        settingsBarPage.settingsCogButton.displayed
    }

    def "Clicking settings button opens modal with Theme tab"() {
        when:
        def homePage = go HomePage
        def settingsBarPage = page SettingsBarPage
        settingsBarPage.clickSettingsButton()
        settingsBarPage.waitForModalVisible()

        then:
        settingsBarPage.settingsModal.displayed
        settingsBarPage.isThemeTabActive()

        cleanup:
        settingsBarPage.closeModal()
    }

    def "Theme dropdown changes theme preference"() {
        when:
        def homePage = go HomePage
        def settingsBarPage = page SettingsBarPage
        settingsBarPage.clickSettingsButton()
        settingsBarPage.waitForModalVisible()
        settingsBarPage.setTheme("dark")

        then:
        settingsBarPage.getCurrentTheme() == "dark"

        cleanup:
        settingsBarPage.setTheme("system")
        settingsBarPage.closeModal()
    }

    def "Switching between Theme and UI Early Access tabs"() {
        when:
        def homePage = go HomePage
        def settingsBarPage = page SettingsBarPage
        settingsBarPage.clickSettingsButton()
        settingsBarPage.waitForModalVisible()

        then:
        settingsBarPage.isThemeTabActive()

        when:
        settingsBarPage.clickUiEarlyAccessTab()

        then:
        settingsBarPage.isUiEarlyAccessTabActive()

        when:
        settingsBarPage.clickThemeTab()

        then:
        settingsBarPage.isThemeTabActive()

        cleanup:
        settingsBarPage.closeModal()
    }

    def "NextUI cookie true enables nextUi mode on capable pages"() {
        when:
        driver.manage().addCookie(new Cookie("nextUi", "true", "/"))
        def jobListPage = go JobListPage, PROJECT_NAME
        waitForPageLoadComplete()

        then:
        def body = driver.findElement(By.tagName("body"))
        def bodyClass = body.getAttribute("class")
        bodyClass.contains("ui-type-next")

        cleanup:
        driver.manage().deleteCookieNamed("nextUi")
    }

    def "NextUI cookie false disables nextUi mode"() {
        when:
        driver.manage().addCookie(new Cookie("nextUi", "false", "/"))
        def jobListPage = go JobListPage, PROJECT_NAME
        waitForPageLoadComplete()

        then:
        def body = driver.findElement(By.tagName("body"))
        def bodyClass = body.getAttribute("class")
        !bodyClass.contains("ui-type-next")

        cleanup:
        driver.manage().deleteCookieNamed("nextUi")
    }

    def "Closing settings modal with close button"() {
        when:
        def homePage = go HomePage
        def settingsBarPage = page SettingsBarPage
        settingsBarPage.clickSettingsButton()
        settingsBarPage.waitForModalVisible()

        then:
        settingsBarPage.isModalOpen()

        when:
        settingsBarPage.closeModal()

        then:
        !settingsBarPage.isModalOpen()
    }
}
