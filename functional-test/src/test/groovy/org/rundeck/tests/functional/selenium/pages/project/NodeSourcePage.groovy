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
class NodeSourcePage extends BasePage {

    By newNodeSource = By.xpath("//button[contains(.,'Source')]")

    String loadPath = ""

    NodeSourcePage(final SeleniumContext context) {
        super(context)
    }

    NodeSourcePage(final SeleniumContext context, String project) {
        super(context)
        this.loadPath = "/project/${project}/nodes/sources"
    }

    WebElement getNewNodeSourceButton() {
        el newNodeSource
    }

}
