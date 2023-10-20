package org.rundeck.tests.functional.selenium


import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class BasicLoginSpec extends SeleniumBase {
    //override env var if necessary to run with different credentials
    public static final String TEST_USER = System.getenv("RUNDECK_TEST_USER") ?: "admin"
    public static final String TEST_PASS = System.getenv("RUNDECK_TEST_PASS") ?: "admin123"

    def "successful login"() {

        when:
            get(client.baseUrl)
            def page = page LoginPage
            page.login(TEST_USER, TEST_PASS)

        then:
            currentUrl.contains("/menu/home")
    }

    def "login failed wrong pass"() {

        when:
            get(client.baseUrl)
            def page = page LoginPage
            page.login(TEST_USER, TEST_PASS + ":nope,wrong-password")
        then:
            currentUrl.contains("/user/error")
    }

    def "login failed empty pass"() {

        when:
            get(client.baseUrl)
            def page = page LoginPage
            page.login(TEST_USER, "")
        then:
            currentUrl.contains("/user/error")
    }

}