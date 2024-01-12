package org.rundeck.tests.functional.selenium.pages.home

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

/**
 * Home page
 */
@CompileStatic
class HomePage extends BasePage {

    By createNewProject = By.linkText("Create New Project")
    By newProject = By.linkText("New Project")
    By bodyNextUIBy = By.cssSelector('body.ui-type-next')
    By projectActionsButton = By.cssSelector('.btn-group.dropdown')

    static final String PAGE_PATH_PROJECT = '/project/$PROJECT/home'

    String loadPath = "/menu/home"

    HomePage(final SeleniumContext context) {
        super(context)
    }

    void goProjectHome(String projectName) {
        loadPath = PAGE_PATH_PROJECT
        driver.get("${context.client.baseUrl}${loadPath.replaceAll('\\$PROJECT', projectName)}")
        waitForUrlToContain(projectName)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on home page: " + driver.currentUrl)
        }
    }

    void createProjectButton() {
        try {
            waitForElementToBeClickable createNewProjectField
            createNewProjectField.click()
        } catch (Exception ignored) {
            waitForElementToBeClickable newProjectField
            newProjectField.click()
        }
    }

    WebElement getCreateNewProjectField() {
        el createNewProject
    }

    WebElement getNewProjectField() {
        el newProject
    }

    WebElement getBodyNextUI(){
        el bodyNextUIBy
    }

    WebElement getProjectActionsButton(){
        waitPresent(projectActionsButton, 5)
    }

    private WebElement waitPresent(By selector, Integer seconds) {
        new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(
                ExpectedConditions.presenceOfElementLocated(selector)
        )
    }

    void setLoadPath(String loadPath) {
        this.loadPath = loadPath
    }

    void loadPathToNextUI() {
        loadPath = "/menu/home?nextUi=true"
    }
}
