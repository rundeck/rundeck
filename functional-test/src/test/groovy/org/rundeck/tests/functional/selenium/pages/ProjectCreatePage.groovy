package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

/**
 * Project create page
 */
@CompileStatic
class ProjectCreatePage extends BasePage {
    static final String PAGE_PATH = "/resources/createProject"

    By projectNameInputBy = By.cssSelector("#createform form input[name=\"newproject\"]")
    By labelInputBy = By.cssSelector("#createform form input[name=\"label\"]")
    By descriptionInputBy = By.cssSelector("#createform form input[name=\"description\"]")

    ProjectCreatePage(final SeleniumContext context) {
        super(context)
    }

    @Override
    String getLoadPath() {
        PAGE_PATH
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

}
