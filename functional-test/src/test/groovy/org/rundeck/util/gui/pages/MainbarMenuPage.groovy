package org.rundeck.util.gui.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

/**
 * Component page object for the top mainbar's system config and user menus.
 * Both menus are rendered via Vue UiSocket, so all checks use explicit waits
 * to account for async mounting.
 *
 * Has no load path — attach to any page after navigation.
 */
@CompileStatic
class MainbarMenuPage extends BasePage {

    String loadPath = ""

    /** Rendered Vue component for the system config (gear) menu. */
    By sysConfigMenuBy = By.id("appSysConfigMenu")

    /** Rendered Vue component for the user menu. */
    By userMenuBy = By.id("appUserMenu")

    /** Toggle button inside the sys config component. */
    By sysConfigToggleBy = By.cssSelector("#appSysConfigMenu [data-testid='mainbar-menu-toggle']")

    /** Toggle button inside the user menu component. */
    By userMenuToggleBy = By.cssSelector("#appUserMenu [data-testid='mainbar-menu-toggle']")

    /**
     * The uiv Dropdown component adds class "open" to its root element when the
     * dropdown is expanded. Since the id attribute falls through to that root div,
     * "#appSysConfigMenu.open" is a reliable open-state indicator.
     */
    By sysConfigOpenBy = By.cssSelector("#appSysConfigMenu.open")

    /** @see #sysConfigOpenBy */
    By userMenuOpenBy = By.cssSelector("#appUserMenu.open")

    MainbarMenuPage(SeleniumContext context) {
        super(context)
    }

    /**
     * Waits up to 30 s for the sys config menu component to be visible.
     * The component renders asynchronously via Vue UiSocket after DOMContentLoaded.
     */
    WebElement waitForSysConfigMenuVisible() {
        waitForElementVisible(sysConfigMenuBy)
    }

    /**
     * Waits up to 30 s for the user menu component to be visible.
     * The component renders asynchronously via Vue UiSocket after DOMContentLoaded.
     */
    WebElement waitForUserMenuVisible() {
        waitForElementVisible(userMenuBy)
    }

    boolean isSysConfigMenuDisplayed() {
        waitForSysConfigMenuVisible()
        el(sysConfigMenuBy).isDisplayed()
    }

    boolean isUserMenuDisplayed() {
        waitForUserMenuVisible()
        el(userMenuBy).isDisplayed()
    }

    void clickSysConfigMenu() {
        byAndWaitClickable(sysConfigToggleBy).click()
    }

    void clickUserMenu() {
        byAndWaitClickable(userMenuToggleBy).click()
    }

    boolean isSysConfigDropdownOpen() {
        els(sysConfigOpenBy).size() > 0
    }

    boolean isUserMenuDropdownOpen() {
        els(userMenuOpenBy).size() > 0
    }
}
