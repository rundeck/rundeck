package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

@CompileStatic
class ProjectCreatePage extends BasePage {

    static final String PAGE_PATH = "/resources/createProject"
    //Use xpath since the css locator changes if a project already exist
    By createNewProjectButton = By.xpath('//a[@href="/resources/createProject"]')
    By newProjectNameField = By.name("newproject")
    By createProjectButton = By.name('create')

    String loadPath = PAGE_PATH

    ProjectCreatePage(final SeleniumContext context) {
        super(context)
    }

    WebElement getCreateNewProjectButton(){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.numberOfElementsToBe(createNewProjectButton, 1))
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.elementToBeClickable(createNewProjectButton))
        el createNewProjectButton
    }

    WebElement getProjectNameField(){
        el newProjectNameField
    }

    WebElement getCreateProjectButton(){
        el createProjectButton
    }

}
