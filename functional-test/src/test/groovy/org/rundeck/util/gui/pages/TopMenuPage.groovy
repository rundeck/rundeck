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
    By logOutMenuBy = By.linkText("Logout")
    By divHome = By.id("nav-rd-home")

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
        byAndWait logOutMenuBy click()
    }

    void clickHomeButton(){
        waitForElementToBeClickable divHome
        try {
            (el divHome).findElement(By.tagName("i")).click()
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            // Retry once if stale
            (el divHome).findElement(By.tagName("i")).click()
        }
    }

    void navigateToUserProfile() {
        openAppUserMenu()
        byAndWaitClickable(By.linkText("Profile")) click()
    }
}
