package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

@CompileStatic
class ProjectCreatePage extends BasePage {

    static final String PAGE_PATH = "/resources/createProject"
    //Use xpath since the css locator changes if a project already exist
    By createNewProjectButton = By.xpath('//a[@href="/resources/createProject"]')
    By newProjectNameField = By.name("newproject")
    By createProjectButton = By.name('create')

    String loadPath = PAGE_PATH

    ProjectCreatePage(SeleniumContext context) {
        super(context)
    }

    WebElement getCreateNewProjectButton(){
        el createNewProjectButton
    }

    WebElement getProjectNameField(){
        el newProjectNameField
    }

    WebElement getCreateProjectButton(){
        el createProjectButton
    }

}
