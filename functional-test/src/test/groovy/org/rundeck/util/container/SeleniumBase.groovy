package org.rundeck.util.container

import groovy.transform.CompileStatic
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.spock.extensions.TestResultExtension
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration
import java.util.logging.Level

/**
 * Utility Base for selenium test specs
 */
@CompileStatic
class SeleniumBase extends BaseContainer implements WebDriver, SeleniumContext {

    public static final String TEST_USER = System.getenv("RUNDECK_TEST_USER") ?: "admin"
    public static final String TEST_PASS = System.getenv("RUNDECK_TEST_PASS") ?: "admin123"
    public static final String SELENIUM_BASIC_PROJECT = "SeleniumBasic"
    public static final String downloadFolder = System.getProperty("user.dir") + "/src/test/resources" + getSeparator() +"downloads";
    public static final boolean TEST_SELENIUM_HEADLESS_MODE = (System.getenv("TEST_SELENIUM_HEADLESS_MODE") ?: "true").toBoolean()

    /**
     * Create a driver
     */
    private WebDriver _driver

    @Delegate
    WebDriver getDriver() {
        if (null == _driver) {
            def prefs = ["download.default_directory": downloadFolder]
            LoggingPreferences logPrefs = new LoggingPreferences()
            logPrefs.enable(LogType.BROWSER, Level.ALL)

            ChromeOptions options = new ChromeOptions()
            options.setCapability("goog:loggingPrefs", logPrefs);
            options.setImplicitWaitTimeout(Duration.ofSeconds(5))
            options.setExperimentalOption("prefs", prefs)
            options.addArguments("start-maximized")
            options.addArguments("enable-automation")
            options.addArguments("--no-sandbox")
            options.addArguments("--disable-infobars")
            options.addArguments("--disable-dev-shm-usage")
            options.addArguments("--disable-browser-side-navigation")
            options.addArguments("--disable-gpu")
            options.addArguments("--disable-extensions")
            options.addArguments("--disable-popup-blocking")
            options.addArguments("--disable-default-apps")
            options.addArguments("--disable-blink-features=AutomationControlled")
            options.addArguments("--disable-features=Chrome,DownloadPromptForDownload")
            if(TEST_SELENIUM_HEADLESS_MODE) {
                options.addArguments("--headless=new")
            }
            _driver = new ChromeDriver(options)
        }
        return _driver
    }


    def cleanup() {
        if(_driver){
            specificationContext.currentSpec.listeners
                    .findAll { it instanceof TestResultExtension.ErrorListener }
                    .each {
                        def errorInfo = (it as TestResultExtension.ErrorListener).errorInfo
                        if(errorInfo){
                            File screenshot = ((TakesScreenshot) _driver).getScreenshotAs(OutputType.FILE)
                            File testResourcesDir = new File("build/test-results/images")
                            if (!testResourcesDir.exists()) {
                                testResourcesDir.mkdirs()
                            }
                            File destination = new File(testResourcesDir, "${specificationContext.currentSpec.filename}-${specificationContext.currentIteration.name}" + ".png")
                            screenshot.renameTo(destination)
                        }
                    }
            _driver?.quit()
        }
    }

    /**
     * Get a page object for the type, does not automatically load the page
     * @param clazz Page object type, must have a constructor that takes a WebDriver
     * @return
     */
    <T extends BasePage> T page(Class<T> clazz) {
        return clazz.getDeclaredConstructor(SeleniumContext).newInstance(this)
    }

    /**
     * Get a page object for the type, does not automatically load the page and send additional args
     * @param clazz Page object type, must have a constructor that takes a WebDriver
     * @return
     */
    <T extends BasePage> T page(Class<T> clazz, Object args) {
        return clazz.getDeclaredConstructor(SeleniumContext, args.getClass()).newInstance(this, args)
    }


    /**
     * Load the page and return the page object
     * @param clazz Page object type, must have a constructor that takes a WebDriver
     * @return
     */
    <T extends BasePage> T go(Class<T> clazz) {
        T page = page(clazz)
        page.go()
        return page
    }

    /**
     * Load the page and return the page object
     * @param clazz Page object type, must have a constructor that takes a WebDriver
     * @return
     */
    <T extends BasePage> T go(Class<T> clazz, Object args) {
        T page = page(clazz, args)
        page.go()
        return page
    }

    /**
     * Retrieves the file separator based on the operating system.
     * On Windows, it returns "\\"; on other systems, it returns "/".
     *
     * @return The file separator.
     */
    static String getSeparator() {
        return System.getProperty("os.name").toLowerCase().contains("windows") ? "\\" : "/"
    }

    /**
     * Wait for document readyState to be 'complete'
     * @param waitingTime
     */
    void waitForPageLoadComplete(Duration waitingTime = WaitingTime.MODERATE) {
        new WebDriverWait(driver, waitingTime).until {
            (((JavascriptExecutor) it).executeScript("return document.readyState") == "complete")
        }
    }
}
