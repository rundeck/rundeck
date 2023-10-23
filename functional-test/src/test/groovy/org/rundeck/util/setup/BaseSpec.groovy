package org.rundeck.util.setup

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
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
class BaseSpec extends BaseContainer {

    private static final int maxRetries = 5
    public static JavascriptExecutor js

    WebDriver driver = new ChromeDriver()
    def user = 'admin' //Default user
    def pass = 'admin123' //Default pass

    def setup() {
        def client = getClient()
        driver.get(client.baseUrl)
    }

    def cleanup() {
        takeScreenshot(toCamelCase(specificationContext.currentFeature.name))
        driver.quit()
    }

    void doLogin() {
        driver.findElement(By.id('login')).sendKeys(user as CharSequence)
        driver.findElement(By.id('password')).sendKeys(pass as CharSequence)
        driver.findElement(By.id('btn-login')).click()
    }

    void createProject(String projectName, Map<String, String> attributes) {
        (0..<maxRetries).any {
            try {
                if (driver.findElements(By.linkText("Create New Project")).size() == 0)
                    driver.findElement(By.linkText("New Project")).click()
                else
                    driver.findElement(By.linkText("Create New Project")).click()
                return true
            } catch (StaleElementReferenceException ignored) {
            }
        }
        driver.findElement(By.id("newproject")).click()
        driver.findElement(By.id("newproject")).sendKeys(projectName)

        if (attributes != null && !attributes.isEmpty()) {
            attributes.each { key, value ->
                driver.findElement(By.id(key)).sendKeys(value)
            }
        }

        driver.findElement(By.id("create")).click()
    }

    void outsideProjectGoTo(NavBarTypes navBar) {
        driver.findElement(By.id('appAdmin')).click()
        driver.findElement(By.linkText(navBar.linkText)).click();
        driver.currentUrl.contains(navBar.url)
    }

    void intoProjectGoTo(NavLinkTypes navLink) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(15))
        (0..<maxRetries).any {
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
                return true
            } catch (StaleElementReferenceException ignored) {
            } catch (InterruptedException e) {
                e.printStackTrace()
            }
        }
    }

    void deleteKeyStorage(String storagePath, String name) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(5))
        outsideProjectGoTo(NavBarTypes.KEYSTORAGE)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"page_storage\"]//*[@class=\"action\"]//*[contains(.,'$storagePath')]")))
        driver.findElement(By.xpath("//*[@id=\"page_storage\"]//*[@class=\"action\"]//*[contains(.,'$storagePath')]")).click()
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"page_storage\"]//*[@class=\"action\"]//*[contains(.,'$name')]")))
        driver.findElement(By.xpath("//*[@id=\"page_storage\"]//*[@class=\"action\"]//*[contains(.,'$name')]")).click()
        driver.findElement(By.xpath("//*[@class=\"btn-group\"]//button[contains(.,'Action')]")).click()
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Delete Selected Item")))
        driver.findElement(By.linkText("Delete Selected Item")).click()
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@class=\"modal-content\"]//button[contains(.,'Delete')]")))
        driver.findElement(By.xpath("//*[@class=\"modal-content\"]//button[contains(.,'Delete')]")).click()
        driver.findElement(By.id("nav-rd-home")).click()
    }

    void overwriteKeyStorage(String newKeyValue, String storagePath, String name) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(5))
        outsideProjectGoTo(NavBarTypes.KEYSTORAGE)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"page_storage\"]//*[@class=\"action\"]//*[contains(.,'$storagePath')]")))
        driver.findElement(By.xpath("//*[@id=\"page_storage\"]//*[@class=\"action\"]//*[contains(.,'$storagePath')]")).click()
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"page_storage\"]//*[@class=\"action\"]//*[contains(.,'$name')]")))
        driver.findElement(By.xpath("//*[@id=\"page_storage\"]//*[@class=\"action\"]//*[contains(.,'$name')]")).click()
        driver.findElement(By.linkText("Overwrite Key")).click()
        waitForModal(1)
        driver.findElement(By.id("uploadpasswordfield")).click()
        driver.findElement(By.id("uploadpasswordfield")).sendKeys(newKeyValue)
        driver.findElement(By.xpath("//input[@type='submit']")).click()
        checkKeyExists(name, storagePath)
        driver.findElement(By.id("nav-rd-home")).click()
    }

    void addKeyStorage(StorageKeyType keyType, String keyValue, String storagePath, String name) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(15))
        outsideProjectGoTo(NavBarTypes.KEYSTORAGE)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText('Add or Upload a Key')))
        driver.findElement(By.linkText('Add or Upload a Key')).click()
        waitForModal(1)
        def select = new Select(driver.findElement(By.name("uploadKeyType")))
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("uploadKeyType")))
        select.selectByValue(keyType.type)
        driver.findElement(By.id(keyType.fieldId)).sendKeys(keyValue)
        driver.findElement(By.id("uploadResourcePath2")).sendKeys(storagePath)
        driver.findElement(By.id("uploadResourceName2")).sendKeys(name)
        driver.findElement(By.xpath("//input[@type='submit']")).click()
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

    void deleteProject(boolean skipActivityValidation) {
        driver.findElement(By.id("nav-project-settings")).click()
        new WebDriverWait(driver, Duration.ofSeconds(2)).until {
            ExpectedConditions.visibilityOf(driver.findElement(By.cssSelector(".navbar__item-container.active")))
        }

        if (!skipActivityValidation) {
            js.executeScript("location.href = '#nav-project-settings-edit-project';")
            driver.findElement(By.id("nav-project-settings-edit-project")).click()
            driver.findElement(By.linkText("Execution Mode")).click()

            if (!driver.findElement(By.name("extraConfig.scheduledExecutionService.disableSchedule")).isSelected()) {
                driver.findElement(By.name("extraConfig.scheduledExecutionService.disableSchedule")).click()
            }

            if (!driver.findElement(By.name("extraConfig.scheduledExecutionService.disableExecution")).isSelected()) {
                driver.findElement(By.name("extraConfig.scheduledExecutionService.disableExecution")).click()
            }

            driver.findElement(By.id("save")).click()
            driver.findElement(By.id("nav-activity-link")).click()
            driver.findElement(By.id("auto-refresh")).click()

            new WebDriverWait(driver, Duration.ofSeconds(10)).until {
                ExpectedConditions.numberOfElementsToBe(By.cssSelector("progress-bar.progress-bar-striped.active.progress-bar-info"), 0)
            }

            js.executeScript("location.href = '#nav-project-settings';")
            driver.findElement(By.id("nav-project-settings")).click()

            new WebDriverWait(driver, Duration.ofSeconds(2)).until {
                ExpectedConditions.visibilityOf(driver.findElement(By.cssSelector(".navbar__item-container.active")))
            }
        }

        driver.findElement(By.id("nav-project-settings-delete-project")).click()
        driver.findElement(By.partialLinkText("Delete this Project")).click()

        // wait for deleteProjectModal css to be block
        new WebDriverWait(driver, Duration.ofSeconds(2)).until {
            ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='deleteProjectModal'][contains(@style, 'display: block')]"))
        }

        driver.findElement(By.xpath("//button[text()='Delete Project Now']")).click()
        waitForModal(0)
        driver.getCurrentUrl().contains("/menu/home")
    }

    String toCamelCase(String str) {
        def result = str.split(" ").collect { it.capitalize() }.join("")
        return result[0].toLowerCase() + result[1..-1]
    }

    void waitForModal(int expected) {
        new WebDriverWait(driver, Duration.ofSeconds(15)).until {
            ExpectedConditions.numberOfElementsToBe(By.cssSelector(".modal.fade.in"), expected)
        }
    }

    void takeScreenshot(String fileName) {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)
        File testResourcesDir = new File(System.getProperty("user.home") + "/test-results/images")
        if (!testResourcesDir.exists()) {
            testResourcesDir.mkdirs()
        }
        File destination = new File(testResourcesDir, "${driver.class.simpleName}-$fileName" + ".png")
        screenshot.renameTo(destination)
    }

}
