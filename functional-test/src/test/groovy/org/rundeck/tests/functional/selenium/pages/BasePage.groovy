package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
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

    /**
     * Create a new page
     * @param context
     */
    BasePage(final SeleniumContext context) {
        this.context = context
        this.context.driver.manage().window().setSize(new Dimension(1200, 1050))
        this.context.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
    }

    abstract String getLoadPath()
    /**
     * Go to the page and validate
     */
    void go() {
        if (loadPath && !loadPath.empty) {
            implicitlyWait 2000
            driver.get(context.client.baseUrl + loadPath)
            validatePage()
        }
    }

    void go(String loadPath) {
        if (loadPath && !loadPath.empty) {
            implicitlyWait 2000
            driver.get(context.client.baseUrl + loadPath)
            validatePage()
        }
    }
    /**
     * Validate the page is loaded
     */
    void validatePage() {

    }

    void refresh() {
        driver.navigate().refresh()
    }

    WebElement waitForElementVisible(WebElement locator) {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.visibilityOf(locator))
    }

    WebElement waitForElementVisible(By locator) {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.visibilityOfElementLocated(locator))
    }

    void waitForNumberOfElementsToBeOne(By locator) {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.numberOfElementsToBe(locator, 1))
    }

    void waitForElementToBeClickable(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30))
        wait.until {
            WebDriver d ->
                def elementLocator = d.findElement(locator)
                ExpectedConditions.elementToBeClickable(elementLocator)
        }
    }

    void waitForElementToBeClickable(WebElement locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30))
        wait.until { ExpectedConditions.elementToBeClickable(locator) }
    }

    void waitForTextToBePresentInElement(WebElement locator, String text) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30))
        wait.until { ExpectedConditions.textToBePresentInElement(locator, text) }
    }

    boolean waitForElementAttributeToChange(WebElement locator, String attribute, String valueCompare) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30))
        wait.until {
            WebDriver d ->
                def elementLocator = locator.getAttribute(attribute)
                elementLocator == valueCompare
        }
    }

    void implicitlyWait(int milliSeconds) {
        context.driver.manage().timeouts().implicitlyWait(Duration.ofMillis(milliSeconds))
    }

    void waitForNumberOfElementsToBe(By locator, Integer number) {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.numberOfElementsToBe(locator, number))
    }

    WebElement waitIgnoringForElementVisible(WebElement locator) {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.visibilityOf(locator))
    }

    WebElement waitIgnoringForElementToBeClickable(WebElement locator) {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.elementToBeClickable(locator))
    }

    WebElement waitForPresenceOfElementLocated(By locator) {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.presenceOfElementLocated(locator))
    }

    void waitForUrlToContain(String text) {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.urlContains(text))
    }

    boolean waitForAttributeContains(WebElement locator, String attribute, String value) {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.attributeContains(locator, attribute, value))
    }

    def waitForModal(int expected) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30)).until {
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

    WebElement byAndWaitClickable(By locator) {
        waitForElementToBeClickable locator
        el locator
    }

    WebDriver getDriver() {
        context.driver
    }

    void executeScript(String script) {
        ((JavascriptExecutor) context.driver).executeScript(script)
    }

    void executeScript(String script, WebElement element) {
        ((JavascriptExecutor) context.driver).executeScript(script, element)
    }

    WebElement el(By by) {
        context.driver.findElement(by)
    }

    List<WebElement> els(By by) {
        context.driver.findElements(by)
    }

    WebElement getLink(String text){
        el By.partialLinkText(text)
    }
}
