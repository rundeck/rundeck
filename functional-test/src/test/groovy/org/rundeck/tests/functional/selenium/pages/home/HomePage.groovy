package org.rundeck.tests.functional.selenium.pages.home

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * Home page
 */
@CompileStatic
class HomePage extends BasePage {

    By createNewProject = By.linkText("Create New Project")
    By newProject = By.linkText("New Project")

    static final String PAGE_PATH_PROJECT = '/project/$PROJECT/home'

    String loadPath = "/menu/home"

    HomePage(final SeleniumContext context) {
        super(context)
    }

    void goProjectHome(String projectName) {
        loadPath = PAGE_PATH_PROJECT
        driver.get("${context.client.baseUrl}${loadPath.replaceAll('\\$PROJECT', projectName)}")
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on home page: " + driver.currentUrl)
        }
    }

    void createProjectButton() {
        if (createNewProjectFields.size() == 0)
            newProjectField.click()
        else
            createNewProjectField.click()
    }

    List<WebElement> getCreateNewProjectFields() {
        els createNewProject
    }

    WebElement getCreateNewProjectField() {
        el createNewProject
    }

    WebElement getNewProjectField() {
        el newProject
    }

    void setLoadPath(String loadPath) {
        this.loadPath = loadPath
    }
}
