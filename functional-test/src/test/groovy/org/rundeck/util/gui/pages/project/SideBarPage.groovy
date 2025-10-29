package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage
import org.rundeck.util.gui.common.navigation.NavLinkTypes

/**
 * Side Bar page
 */
@CompileStatic
class SideBarPage extends BasePage {

    String loadPath = ""

    By projectSettings = By.id("nav-project-settings")
    By projectSettingsExportArchive = By.id("nav-project-settings-export-archive")
    By navContainer = By.cssSelector('.navbar__item-container.active')
    By isOverflow = By.id("overflow")

    SideBarPage(final SeleniumContext context) {
        super(context)
    }

    void goTo(NavLinkTypes navLink) {
        def navIdBy = By.id(navLink.id)
        if (navLink.projectConfig) {
            byAndWaitClickable(projectSettings).click()
            waitForNavVisible()
        } else if (overflowFields.size() == 1) {
            // Check if element is visible using a try-catch to handle staleness
            try {
                if (!(el navIdBy).isDisplayed()) {
                    byAndWaitClickable(isOverflow).click()
                    waitForAttributeContains overflowField, 'class', 'active'
                }
            } catch (Exception e) {
                // Element might be stale, click overflow anyway
                byAndWaitClickable(isOverflow).click()
                waitForAttributeContains overflowField, 'class', 'active'
            }
        }
        byAndWaitClickable(navIdBy).click()
    }

    WebElement waitForNavVisible() {
        waitForElementVisible navContainer
    }

    WebElement getProjectSettingsField() {
        el projectSettings
    }

    WebElement getProjectSettingsExportArchiveField() {
        el projectSettingsExportArchive
    }

    WebElement getOverflowField() {
        el isOverflow
    }

    List<WebElement> getOverflowFields() {
        els isOverflow
    }
}
