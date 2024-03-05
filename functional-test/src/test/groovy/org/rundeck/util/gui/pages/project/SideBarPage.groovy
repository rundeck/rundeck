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
    By navContainer = By.cssSelector('.navbar__item-container.active')
    By isOverflow = By.id("overflow")

    SideBarPage(final SeleniumContext context) {
        super(context)
    }

    void goTo(NavLinkTypes navLink) {
        def navIdBy = By.id(navLink.id)
        if (navLink.projectConfig) {
            projectSettingsField.click()
            waitForElementVisible navContainer
        } else if (!(el navIdBy).isDisplayed() && overflowFields.size() == 1) {
            overflowField.click()
            waitForAttributeContains overflowField, 'class', 'active'
        }
        el navIdBy click()
    }

    WebElement getProjectSettingsField() {
        el projectSettings
    }

    WebElement getOverflowField() {
        el isOverflow
    }

    List<WebElement> getOverflowFields() {
        els isOverflow
    }
}
