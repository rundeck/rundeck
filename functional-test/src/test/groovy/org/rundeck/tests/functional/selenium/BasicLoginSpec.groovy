package org.rundeck.tests.functional.selenium

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.BaseContainer

@SeleniumCoreTest
class BasicLoginSpec extends BaseContainer {

    def "successful login"() {
        given:
        def client = getClient()
        WebDriver driver = new ChromeDriver()
        driver.get(client.baseUrl)
        when:
        driver.findElement(By.id("login")).sendKeys("admin")
        driver.findElement(By.id("password")).sendKeys("admin123")
        driver.findElement(By.id("btn-login")).click()
        then:
        driver.currentUrl.contains("/menu/home")
        cleanup:
        driver.quit()
    }

}