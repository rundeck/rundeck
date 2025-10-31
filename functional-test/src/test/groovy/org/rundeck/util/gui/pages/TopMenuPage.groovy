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
        byAndWait logOutMenuBy click()
    }

    void clickHomeButton(){
        byAndWaitClickable divHomeIconTag click()
    }

    void navigateToUserProfile() {
        openAppUserMenu()
        byAndWaitClickable(By.linkText("Profile")) click()
    }
}
