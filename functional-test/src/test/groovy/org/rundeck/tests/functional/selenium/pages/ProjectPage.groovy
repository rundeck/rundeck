package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

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
