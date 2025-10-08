package org.rundeck.util.gui.pages.home

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.gui.pages.BasePage
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
    By readmeMessageBy = By.className("markdown-body")

    static final String PAGE_PATH_PROJECT = '/project/$PROJECT/home'

    String loadPath = "/menu/home"

    HomePage(final SeleniumContext context) {
        super(context)
    }

    void goProjectHome(String projectName) {
        driver.get("${context.client.baseUrl}${PAGE_PATH_PROJECT.replaceAll('\\$PROJECT', projectName)}")
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

    def getReadmeMessage(){
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(
                ExpectedConditions.numberOfElementsToBe(readmeMessageBy, 1)
        )
        (el readmeMessageBy).getText()
    }
}
