package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.FindBy
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext

import java.time.Duration
import java.util.concurrent.TimeoutException

/**
 * Base type for page object model
 */
@CompileStatic
abstract class BasePage {
    final SeleniumContext context
    By modalField = By.cssSelector(".modal.fade.in")
    @FindBy(id = "nav-rd-home") WebElement home

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

    WebElement waitForElementVisible(WebElement locator) {
        new WebDriverWait(context.driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.visibilityOf(locator))
    }

    WebElement waitForElementVisible(By locator) {
        new WebDriverWait(context.driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.visibilityOfElementLocated(locator))
    }

    WebElement waitIgnoringForElementVisible(WebElement locator) {
        new WebDriverWait(context.driver, Duration.ofSeconds(15))
                .ignoring(StaleElementReferenceException.class)
                .until (ExpectedConditions.visibilityOf(locator))
    }

    WebElement waitIgnoringForElementToBeClickable(WebElement locator) {
        new WebDriverWait(context.driver, Duration.ofSeconds(15))
                .ignoring(StaleElementReferenceException.class)
                .until (ExpectedConditions.elementToBeClickable(locator))
    }

    def waitForModal(int expected) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15)).until {
                ExpectedConditions.numberOfElementsToBe(modalField, expected)
            }
        } catch (TimeoutException e) {
            throw new RuntimeException("Timed out waiting for the modal to have ${expected} elements.", e)
        }
    }

    WebElement byAndWait(By locator) {
        waitForElementVisible locator
        el locator
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
