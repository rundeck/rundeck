package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.setup.NavLinkTypes

import java.time.Duration

/**
 * Project create page
 */
@CompileStatic
class ProjectCreatePage extends BasePage {
    static final String PAGE_PATH = "/resources/createProject"

    By projectNameInputBy = By.cssSelector("#createform form input[name=\"newproject\"]")
    By labelInputBy = By.cssSelector("#createform form input[name=\"label\"]")
    By descriptionInputBy = By.cssSelector("#createform form input[name=\"description\"]")
    By createBy = By.id("create")
    By isNavigator = By.cssSelector(".nav-drawer")
    By projectSettings = By.id("nav-project-settings")
    By navContainer = By.cssSelector('.navbar__item-container.active')
    By isOverflow = By.id("overflow")
    By newNodeSource = By.xpath("//button[contains(.,'Add a new Node Source')]")
    By deleteProjectNav = By.id("nav-project-settings-delete-project")
    By deleteProjectButton = By.partialLinkText("Delete this Project")
    By deleteProjectModal = By.xpath("//*[@id='deleteProjectModal'][contains(@style, 'display: block')]")
    By deleteProjectConfirm = By.xpath("//button[text()='Delete Project Now']")

    ProjectCreatePage(final SeleniumContext context) {
        super(context)
    }

    @Override
    String getLoadPath() {
        PAGE_PATH
    }

    void createProject(String name) {
        projectNameInput.click()
        projectNameInput.sendKeys(name)
        createField.click()
    }

    void deleteProject() {
        projectSettingsField.click()
        waitForElementVisible navContainer
        deleteProjectNavField.click()
        deleteProjectButtonField.click()
        waitForPresenceOfElementLocated deleteProjectModal
        deleteProjectConfirmField.click()
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

    WebElement getProjectNameInput() {
        el projectNameInputBy
    }

    WebElement getLabelInput() {
        el labelInputBy
    }

    WebElement getDescriptionInput() {
        el descriptionInputBy
    }

    WebElement getCreateField() {
        el createBy
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

    WebElement getNewNodeSourceButton() {
        el newNodeSource
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

}
