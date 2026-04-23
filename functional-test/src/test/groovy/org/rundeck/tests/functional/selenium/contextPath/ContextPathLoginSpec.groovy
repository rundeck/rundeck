package org.rundeck.tests.functional.selenium.contextPath

import org.openqa.selenium.By
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoginPage

/**
 * RUN-4332: Test that assets load correctly when server.servlet.context-path is configured
 *
 * This test verifies that:
 * 1. Login page loads with context-path prefix
 * 2. Assets (CSS/JS) load correctly with context-path
 * 3. User can login successfully
 * 4. Home page renders correctly after login
 */
@SeleniumCoreTest
class ContextPathLoginSpec extends SeleniumBase {

    /**
     * Override to use a custom docker-compose with context-path configured
     */
    @Override
    String getCustomDockerComposeLocation() {
        return "docker/compose/oss/docker-compose-context-path.yml"
    }

    /**
     * Override Rundeck URL to include context-path
     */
    @Override
    String getCustomRundeckUrl() {
        return "http://localhost:4440/rundeck"
    }

    def "login with context-path configured"() {
        setup:
            String contextPath = "/rundeck"

        when: "user visits login page"
            def loginPage = go LoginPage

        then: "login page URL includes context-path"
            currentUrl.contains("${contextPath}/user/login")

        and: "page has loaded successfully (no 404 or asset loading errors)"
            pageSource.contains("Login") || pageSource.contains("login")
            !pageSource.contains("404")
            !pageSource.contains("Not Found")

        and: "assets do NOT contain [:] placeholder (RUN-4332 fix verification)"
            !pageSource.contains("[:]")

        when: "user logs in"
            loginPage.login(TEST_USER, TEST_PASS)

        then: "home page loads with context-path"
            currentUrl.contains("${contextPath}/menu/home")
            pageSource =~ /Projects/

        and: "no JavaScript console errors related to asset loading"
            def logs = driver.manage().logs().get("browser").all
            def assetErrors = logs.findAll {
                // Only capture actual errors, not INFO logs
                (it.level.toString() == "SEVERE" || it.level.toString() == "WARNING") &&
                (it.message.contains("Failed to load resource") ||
                 it.message.contains("404") ||
                 it.message.contains("ERR_ABORTED"))
            }
            assetErrors.isEmpty()

        cleanup:
            def topMenuPage = page TopMenuPage
            topMenuPage.logOut()
    }

    def "assets load correctly with context-path"() {
        setup:
            String contextPath = "/rundeck"

        when: "user visits login page"
            def loginPage = go LoginPage
            loginPage.waitForElementVisible(loginPage.loginBtnBy)

        then: "login button is visible (CSS loaded)"
            loginPage.loginBtn.isDisplayed()

        and: "page has rundeck branding (assets loaded)"
            pageSource.contains("Rundeck") || pageSource.contains("Runbook")

        and: "all asset URLs include context-path prefix"
            def assetLinks = driver.findElements(By.cssSelector("link[href*='/assets/'], script[src*='/assets/']"))
            if (!assetLinks.isEmpty()) {
                assetLinks.every { element ->
                    def url = element.getAttribute("href") ?: element.getAttribute("src")
                    // Asset URLs should either be absolute or include context-path
                    url.startsWith("http") || url.startsWith(contextPath)
                }
            }
    }
}
