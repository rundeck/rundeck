package org.rundeck.tests.functional.selenium


import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectListPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class BasicLoginSpec extends SeleniumBase {
    //override env var if necessary to run with different credentials
    public static final String TEST_USER = System.getenv("RUNDECK_TEST_USER") ?: "admin"
    public static final String TEST_PASS = System.getenv("RUNDECK_TEST_PASS") ?: "admin123"

    def "successful login"() {

        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            currentUrl.contains("/menu/home")
    }

    def "login failed wrong pass"() {

        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS + ":nope,wrong-password")
        then:
            currentUrl.contains("/user/error")
            loginPage.error.text == "Invalid username and password."
    }

    def "login failed empty pass"() {

        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, "")
        then:
            currentUrl.contains("/user/error")
            loginPage.error.text == "Invalid username and password."
    }

}