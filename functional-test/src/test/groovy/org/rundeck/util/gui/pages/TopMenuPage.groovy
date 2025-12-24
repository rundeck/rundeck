package org.rundeck.util.gui.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

/**
 * Top Menu page
 */
@CompileStatic
class TopMenuPage extends BasePage {

    String loadPath = ""

    By settingsButtonBy = By.id("appAdmin")
    By systemConfigurationMenuBy = By.linkText("System Configuration")
    By appUserButtonBy = By.id("appUser")
    By appUserMenuDropdownBy = By.cssSelector("#appUser .dropdown-menu")
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
        // Check if dropdown is already open
        try {
            def dropdown = el(appUserMenuDropdownBy)
            if (dropdown.isDisplayed()) {
                // Wait for menu items to be present
                waitForElementVisible(logOutMenuBy)
                return
            }
        } catch (Exception e) {
            // Dropdown not found or not visible, need to click
        }
        
        // Click user button
        byAndWaitClickable(appUserButtonBy).click()
        
        // Wait for menu items to be visible (more reliable than waiting for container)
        waitForElementVisible(logOutMenuBy)
    }

    void logOut() {
        openAppUserMenu()
        // Get element first, then use waitIgnoring to handle stale elements
        WebElement logoutLink = waitForElementVisible(logOutMenuBy)
        waitIgnoringForElementToBeClickable(logoutLink).click()
        // Wait for logout to complete
        waitForElementVisible(By.partialLinkText("Log In Again"))
        waitForUrlToContain("/user/loggedout")
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
