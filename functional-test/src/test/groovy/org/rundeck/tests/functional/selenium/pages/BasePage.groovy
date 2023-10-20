package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

/**
 * Base type for page object model
 */
@CompileStatic
abstract class BasePage {
    final WebDriver driver

    BasePage(final WebDriver driver) {
        this.driver = driver
    }

    WebElement el(By by) {
        driver.findElement(by)
    }

    List<WebElement> els(By by) {
        driver.findElements(by)
    }
}
