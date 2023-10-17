package org.rundeck.tests.functional.selenium

import org.openqa.selenium.By
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.setup.BaseTest

@SeleniumCoreTest
class BasicLoginSpec extends BaseTest {

    def "successful login"() {
        when:
        doLogin('admin', 'admin')
        then:
        driver.currentUrl.contains("/menu/home")
    }

    def "unsuccessful login with bad password"() {
        when:
        doLogin('admin', 'admin123')
        def errorMessage = driver.findElement(By.cssSelector('.alert.alert-danger')).getText()
        then:
        driver.currentUrl.contains("/user/error")
        errorMessage == 'Invalid username and password.'
    }

}