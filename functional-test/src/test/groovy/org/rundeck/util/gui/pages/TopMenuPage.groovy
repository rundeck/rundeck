package org.rundeck.util.gui.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

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
        clickElementSafely(logOutMenuBy)
        // Rely on post-logout navigation (context-path and product builds vary link markup).
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(
            ExpectedConditions.or(
                ExpectedConditions.urlContains("/user/loggedout"),
                ExpectedConditions.urlContains("/user/login")
            )
        )
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
