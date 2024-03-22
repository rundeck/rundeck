package org.rundeck.tests.functional.selenium.login

import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoginPage

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.profile.UserProfilePage

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

    def "successful login with logout"() {
        when:
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        then:
        currentUrl.contains("/menu/home")
        pageSource =~ /Projects/
        cleanup:
        def topMenuPage = page TopMenuPage
        topMenuPage.logOut()
    }

    def "login failed wrong user"() {
        when:
        def loginPage = go LoginPage
        loginPage.login(TEST_USER + ":nope,wrong-user", TEST_PASS)
        then:
        currentUrl.contains("/user/error")
        loginPage.error.text == "Invalid username and password."
    }

    def "login as admin"() {
        when:
        def loginPage = go LoginPage
        def topMenuPage = page TopMenuPage
        def userProfilePage = page UserProfilePage
        then:
        loginPage.login(TEST_USER, TEST_PASS)
        topMenuPage.navigateToUserProfile()
        userProfilePage.validatePage()
        userProfilePage.editLink.click()
        userProfilePage.userLogin.getText() == TEST_USER
        cleanup:
        topMenuPage.logOut()
    }

    def "login as user"() {
        given:
        def user = "user"
        when:
        def loginPage = go LoginPage
        def topMenuPage = page TopMenuPage
        def userProfilePage = page UserProfilePage
        then:
        loginPage.login(user, "user123")
        topMenuPage.navigateToUserProfile()
        userProfilePage.validatePage()
        userProfilePage.editLink.click()
        userProfilePage.userLogin.getText() == user
        cleanup:
        topMenuPage.logOut()
    }

}