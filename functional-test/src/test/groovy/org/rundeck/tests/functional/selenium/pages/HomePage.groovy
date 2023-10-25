package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.FindBy
import org.rundeck.util.container.SeleniumContext

@CompileStatic
class HomePage extends BasePage {

    By appAdmin = By.id("appAdmin")
    By keyStorage = By.linkText("Key Storage")
    By navHome = By.id("nav-rd-home")
    By createNewProject = By.linkText("Create New Project")
    By newProject = By.linkText("New Project")

    static final String PAGE_PATH = "/menu/home"

    HomePage(final SeleniumContext context) {
        super(context, "/")
    }

    void validatePage() {
        if (!driver.currentUrl.contains(PAGE_PATH)) {
            throw new IllegalStateException("Not on home page: " + driver.currentUrl)
        }
    }

    void createProject() {
        if (createNewProjectFields.size() == 0)
            newProjectField.click()
        else
            createNewProjectField.click()
    }

    void goToKeyStorage() {
        waitForElementVisible appAdminField
        appAdminField.click()
        keyStorageField.click()
    }

    WebElement getAppAdminField() {
        el appAdmin
    }

    WebElement getKeyStorageField() {
        el keyStorage
    }

    WebElement getNavHome() {
        waitForElementVisible navHome
        el navHome
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

}
