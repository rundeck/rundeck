package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.setup.NavLinkTypes

/**
 * Project create page
 */
@CompileStatic
class ProjectCreatePage extends BasePage {

    By projectNameInputBy = By.cssSelector("#createform form input[name=\"newproject\"]")
    By labelInputBy = By.cssSelector("#createform form input[name=\"label\"]")
    By descriptionInputBy = By.cssSelector("#createform form input[name=\"description\"]")
    By createBy = By.id("create")

    String loadPath = "/resources/createProject"

    ProjectCreatePage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on create project page: " + driver.currentUrl)
        }
    }

    void createProject(String name) {
        projectNameInput.click()
        projectNameInput.sendKeys(name)
        createField.click()
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



}
