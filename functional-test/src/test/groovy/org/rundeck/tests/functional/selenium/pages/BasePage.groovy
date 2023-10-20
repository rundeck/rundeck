package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

/**
 * Base type for page object model
 */
@CompileStatic
abstract class BasePage {
    final SeleniumContext context

    /**
     * Create a new page
     * @param context
     */
    BasePage(final SeleniumContext context) {
        this(context, null)
    }
    /**
     * Create a new page, will load the loadPath if present and validate with {@link #validatePage()}
     * @param context
     */
    BasePage(final SeleniumContext context, String loadPath) {
        this.context = context
        if (loadPath) {
            driver.get(context.client.baseUrl + loadPath)
            validatePage()
        }
    }

    /**
     * Validate the page is loaded correctly, e.g. thrown an exception if not valid.
     */
    void validatePage() {

    }


    WebDriver getDriver() {
        context.driver
    }

    WebElement el(By by) {
        context.driver.findElement(by)
    }

    List<WebElement> els(By by) {
        context.driver.findElements(by)
    }
}
