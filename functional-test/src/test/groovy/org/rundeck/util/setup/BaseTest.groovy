package org.rundeck.util.setup

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.openqa.selenium.By
import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.BaseContainer

import java.time.Duration

/**
 * Base class for tests, util methods for all tests
 */
@CompileStatic
@Slf4j
class BaseTest extends BaseContainer {

    WebDriver driver = new ChromeDriver()

    def setup() {
        def client = getClient()
        driver.get(client.baseUrl)
    }

    def cleanup() {
        driver.quit()
    }

    void doLogin(String user, String password) {
        driver.findElement(By.id('login')).sendKeys(user)
        driver.findElement(By.id('password')).sendKeys(password)
        driver.findElement(By.id('btn-login')).click()
    }

    void outsideProjectGoTo(NavBarTypes navBar) {
        driver.findElement(By.id('appAdmin')).click()
        driver.findElement(By.linkText(navBar.linkText)).click();
        driver.currentUrl.contains(navBar.url)
    }

    void intoProjectGoTo(NavLinkTypes navLink) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(10))
        (0..<5).each {
            try {
                if (navLink.projectConfig) {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector('.nav-drawer')))
                    if (!driver.findElement(By.cssSelector(".nav-drawer")).isDisplayed()) {
                        driver.findElement(By.id("nav-project-settings")).click()
                        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.cssSelector('.navbar__item-container.active'))))
                    }
                } else if (!driver.findElement(By.id(navLink.id)).isDisplayed() && driver.findElements(By.id('overflow')).size() == 1) {
                    driver.findElement(By.id("overflow")).click()
                    wait.until(ExpectedConditions.attributeContains(driver.findElement(By.id('overflow')), 'class', 'active'))
                }
                Thread.sleep(3000)
                driver.findElement(By.id(navLink.id)).click()
                wait.until(ExpectedConditions.urlContains(navLink.url))
                //driver.currentUrl.contains(navLink.url)
            } catch (StaleElementReferenceException ignored) {
            } catch (InterruptedException e) {
                e.printStackTrace()
            }
        }
    }

    void deleteKeyStorage(String storagePath, String name) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(5))
        outsideProjectGoTo(NavBarTypes.KEYSTORAGE)
        driver.findElement(By.xpath("//action[contains(.,'$storagePath')]")).click()
        driver.findElement(By.xpath("//action[contains(.,'$name')]")).click()
        driver.findElement(By.xpath("//button[contains(.,'Delete')]")).click()
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.cssSelector(".modal.in"))))
        driver.findElement(By.xpath("//button[contains(.,'storagedelete')]")).click()
        driver.findElement(By.id("nav-rd-home")).click()
    }

    void overwriteKeyStorage(String newKeyValue, String storagePath, String name) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(5))
        outsideProjectGoTo(NavBarTypes.KEYSTORAGE)
        driver.findElement(By.xpath("//action[contains(.,'$storagePath')]")).click()
        driver.findElement(By.xpath("//action[contains(.,'$name')]")).click()
        driver.findElement(By.xpath("//button[contains(.,'Overwrite Key')]")).click()
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.cssSelector(".modal.in"))))
        driver.findElement(By.id("uploadpasswordfield")).click()
        driver.findElement(By.id("uploadpasswordfield")).sendKeys(newKeyValue)
        driver.findElement(By.xpath("//button[contains(.,'Save')]")).click()
        checkKeyExists(name, storagePath)
        driver.findElement(By.xpath("//*[contains(.,'a few seconds ago')]")).getText()
        driver.findElement(By.id("nav-rd-home")).click()
    }

    void addKeyStorage(StorageKeyType keyType, String keyValue, String storagePath, String name) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(5))
        outsideProjectGoTo(NavBarTypes.KEYSTORAGE)
        driver.findElement(By.xpath("//button[contains(.,'Add or Upload a Key')]")).click()

        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.cssSelector(".modal.in"))))

        def select = new Select(driver.findElement(By.name("uploadKeyType")))
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.name("uploadKeyType"))))
        select.selectByValue(keyType.type)
        driver.findElement(By.id(keyType.fieldId)).sendKeys(keyValue)
        driver.findElement(By.id("uploadResourcePath2")).sendKeys(storagePath)
        driver.findElement(By.id("uploadResourceName2")).sendKeys(name)
        driver.findElement(By.xpath("//button[contains(.,'Save')]")).click()
        checkKeyExists(name, storagePath)
        driver.findElement(By.id("nav-rd-home")).click()
    }

    private void checkKeyExists(String name, String storagePath) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(5))
        try {
            wait.ignoring(StaleElementReferenceException.class).until(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(.,'keys/${storagePath}/${name}')]")))
        } catch (Exception e) {
            String keyDirectoryLocator = "//span[text()='$storagePath']"
            wait.ignoring(StaleElementReferenceException.class).until(
                    ExpectedConditions.elementToBeClickable(By.xpath(keyDirectoryLocator))
            )
            driver.findElement(By.xpath(keyDirectoryLocator)).click()
            String keyLocator = "//span[text()='$name']"
            wait.ignoring(StaleElementReferenceException.class).until(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(keyLocator))
            )
        }
    }

}
