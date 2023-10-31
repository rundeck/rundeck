package org.rundeck.tests.functional.selenium.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * Project page
 */
@CompileStatic
class ProjectPage extends BasePage {

    By newNodeSource = By.xpath("//button[contains(.,'Add a new Node Source')]")

    String loadPath = ""

    ProjectPage(final SeleniumContext context) {
        super(context)
    }

    WebElement getNewNodeSourceButton() {
        el newNodeSource
    }

}
