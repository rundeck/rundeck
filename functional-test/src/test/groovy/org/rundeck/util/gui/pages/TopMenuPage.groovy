package org.rundeck.util.gui.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
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
        byAndWaitClickable appUserButtonBy click()
    }

    void logOut() {
        openAppUserMenu()
        // Wait for dropdown menu to be visible before clicking items inside it
        waitForElementVisible appUserMenuDropdownBy
        byAndWait logOutMenuBy click()
    }

    void clickHomeButton(){
        clickElementSafely(divHomeIconTag)
    }

    void navigateToUserProfile() {
        openAppUserMenu()
        // Wait for dropdown menu to be visible before clicking items inside it
        waitForElementVisible appUserMenuDropdownBy
        byAndWaitClickable(By.linkText("Profile")) click()
    }
}
