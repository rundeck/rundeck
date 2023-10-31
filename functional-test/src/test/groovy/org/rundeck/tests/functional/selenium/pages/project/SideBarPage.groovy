package org.rundeck.tests.functional.selenium.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.setup.NavLinkTypes

/**
 * Side Bar page
 */
@CompileStatic
class SideBarPage extends BasePage {

    String loadPath = ""

    By isNavigator = By.cssSelector(".nav-drawer")
    By projectSettings = By.id("nav-project-settings")
    By navContainer = By.cssSelector('.navbar__item-container.active')
    By isOverflow = By.id("overflow")
    By deleteProjectNav = By.id("nav-project-settings-delete-project")
    By deleteProjectButton = By.partialLinkText("Delete this Project")
    By deleteProjectModal = By.xpath("//*[@id='deleteProjectModal'][contains(@style, 'display: block')]")
    By deleteProjectConfirm = By.xpath("//button[text()='Delete Project Now']")

    SideBarPage(final SeleniumContext context) {
        super(context)
    }

    void goTo(NavLinkTypes navLink) {
        (0..5).any {
            try {
                waitForPresenceOfElementLocated isNavigator
                if (navLink.projectConfig) {
                    if (!(el isNavigator).isDisplayed())
                        projectSettingsField.click()
                    waitForElementVisible navContainer
                } else if (!(el By.id(navLink.id)).isDisplayed() && overflowFields.size() == 1) {
                    overflowField.click()
                    waitForAttributeContains overflowField, 'class', 'active'
                }
                sleep(3000)
                def navId = el By.id(navLink.id)
                navId.click()
                return true
            } catch (StaleElementReferenceException ignored) {
            } catch (InterruptedException e) {
                e.printStackTrace()
            }
        }
    }

    void deleteProject() {
        projectSettingsField.click()
        waitForElementVisible navContainer
        deleteProjectNavField.click()
        deleteProjectButtonField.click()
        waitForPresenceOfElementLocated deleteProjectModal
        deleteProjectConfirmField.click()
    }

    WebElement getDeleteProjectNavField() {
        el deleteProjectNav
    }

    WebElement getDeleteProjectConfirmField() {
        el deleteProjectConfirm
    }

    WebElement getDeleteProjectButtonField() {
        el deleteProjectButton
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
