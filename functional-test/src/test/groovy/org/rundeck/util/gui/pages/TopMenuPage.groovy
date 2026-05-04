package org.rundeck.util.gui.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

import java.net.URI

/**
 * Top Menu page
 */
@CompileStatic
class TopMenuPage extends BasePage {

    String loadPath = ""

    By settingsButtonBy = By.id("appAdmin")
    By systemConfigurationMenuBy = By.linkText("System Configuration")
    By appUserDropdownBy = By.id("userLabel")
    By logOutMenuBy = By.linkText("Logout")
    By divHomeIconTag = By.cssSelector("#nav-rd-home i")

    TopMenuPage(final SeleniumContext context) {
        super(context)
    }

    void navigateToSystemConfiguration() {
        openSettingsMenu()
        def systemConfigEntry = byAndWait systemConfigurationMenuBy
        systemConfigEntry.click()
    }

    void openSettingsMenu() {
        byAndWait settingsButtonBy click()
    }

    void openAppUserMenu() {
        clickElementSafely(appUserDropdownBy)
        waitForElementVisible(logOutMenuBy)
    }

    void logOut() {
        openAppUserMenu()
        String path = ''
        try {
            path = URI.create(driver.currentUrl).path ?: ''
        } catch (Exception ignored) {
            path = driver.currentUrl
        }
        clickElementSafely(logOutMenuBy)
        // Prefer BasePage#waitForUrlToNotContain: leave an authenticated path segment (login / loggedout
        // URLs omit these). Fallback for uncommon paths uses explicit logout landing wait.
        List<String> markers = ['/project/', '/menu/home', '/job/', '/execution/', '/resources/',
                                '/user/profile', '/activity', '/command/run', '/nodes']
        String marker = markers.find { path.contains(it) }
        if (marker != null) {
            waitForUrlToNotContain(marker)
        } else {
            waitForLogoutLandingUrl()
        }
    }

    void clickHomeButton(){
        clickElementSafely(divHomeIconTag)
    }

    void navigateToUserProfile() {
        openAppUserMenu()
        By profileLinkBy = By.linkText("Profile")
        // Get element first, then use waitIgnoring to handle stale elements
        WebElement profileLink = waitForElementVisible(profileLinkBy)
        waitIgnoringForElementToBeClickable(profileLink).click()
        // Wait for navigation to complete
        waitForUrlToContain("/user/profile")
    }
}
