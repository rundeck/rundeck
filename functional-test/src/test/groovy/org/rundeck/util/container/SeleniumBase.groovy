package org.rundeck.util.container

import groovy.transform.CompileStatic
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.rundeck.util.extensions.TestResultExtension
import org.rundeck.tests.functional.selenium.pages.BasePage

/**
 * Utility Base for selenium test specs
 */
@CompileStatic
class SeleniumBase extends BaseContainer implements WebDriver, SeleniumContext {

    public static final String TEST_USER = System.getenv("RUNDECK_TEST_USER") ?: "admin"
    public static final String TEST_PASS = System.getenv("RUNDECK_TEST_PASS") ?: "admin123"

    /**
     * Create a driver
     */
    private WebDriver _driver

    @Delegate
    WebDriver getDriver() {
        if (null == _driver) {
            _driver = new ChromeDriver()
        }
        return _driver
    }


    def cleanup() {
        specificationContext.currentSpec.listeners
                .findAll { it instanceof TestResultExtension.ErrorListener }
                .each {
                    def errorInfo = (it as TestResultExtension.ErrorListener).errorInfo
                    if(errorInfo){
                        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)
                        File testResourcesDir = new File("build/test-results/images")
                        if (!testResourcesDir.exists()) {
                            testResourcesDir.mkdirs()
                        }
                        File destination = new File(testResourcesDir, "${specificationContext.currentSpec.filename}-${specificationContext.currentIteration.name}" + ".png")
                        screenshot.renameTo(destination)
                    }
                }
        driver?.quit()
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
     * Load the page and return the page object
     * @param clazz Page object type, must have a constructor that takes a WebDriver
     * @return
     */
    <T extends BasePage> T go(Class<T> clazz) {
        T page = page(clazz)
        page.go()
        return page
    }

    String toCamelCase(String str) {
        str?.split(" ")?.collect { it.capitalize() }?.join("")?.uncapitalize() ?: ""
    }

}
