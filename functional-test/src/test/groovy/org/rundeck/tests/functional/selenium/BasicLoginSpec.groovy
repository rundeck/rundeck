package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.LoginPage

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class BasicLoginSpec extends SeleniumBase {

    def "successful login"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            currentUrl.contains("/menu/home")
            pageSource =~ /Projects/
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