package org.rundeck.tests.functional.selenium.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

/**
 * Project page
 */
@CompileStatic
class NodeSourcePage extends BasePage {

    String loadPath = ""

    By newNodeSource = By.xpath("//button[contains(.,'Source')]")

    NodeSourcePage(final SeleniumContext context) {
        super(context)
    }

    WebElement getNewNodeSourceButton() {
        el newNodeSource
    }

    void validatePage() {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.urlContains(loadPath))
    }

}
