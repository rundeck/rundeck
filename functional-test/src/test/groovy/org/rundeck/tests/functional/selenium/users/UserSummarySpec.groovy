package org.rundeck.tests.functional.selenium.users

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoggedOutPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.usersummary.UserSummaryPage

@SeleniumCoreTest
class UserSummarySpec extends SeleniumBase {

    def "login a first time with two different users"() {
        when: "Login with two different users consecutively"
        def loginPage = go LoginPage
        def topMenuPage = page TopMenuPage
        def loggedOutPage = page LoggedOutPage
        def userSummaryPage = page UserSummaryPage

        and: "Login with the first user"
        loginPage.login("user", "user123")

        and: "Log out the first user"
        topMenuPage.logOut()
        loggedOutPage.getLoginAgainField().click()

        and: "Login with the second user"
        loginPage.login(TEST_USER, TEST_PASS)

        and: "Navigate to the User Manager Page"
        userSummaryPage.go()

        and: "Get the user count"
        hold(3)
        def userCount = userSummaryPage.userCountField.getText().toInteger()

        then: "At least two users should appear on the user summary page"
        userCount >= 2
    }

    def "search user by name on user summary list"() {
        when: "Login and search for a user by name"
        def loginPage = go LoginPage
        def userSummaryPage = page UserSummaryPage
        def topMenuPage = page TopMenuPage
        def loggedOutPage = page LoggedOutPage

        and: "Login with the first user"
        loginPage.login("user", "user123")

        and: "Log out the first user"
        topMenuPage.logOut()
        loggedOutPage.getLoginAgainField().click()

        and: "Login with the second user"
        loginPage.login(TEST_USER, TEST_PASS)

        and: "Navigate to the User Manager Page"
        userSummaryPage.go()

        and: "Search for the user by name 'admin'"
        userSummaryPage.waitForElementToBeClickable(userSummaryPage.searchButton)
        userSummaryPage.searchButton.click()
        userSummaryPage.waitForElementVisible(userSummaryPage.loginInput)
        userSummaryPage.loginInput.sendKeys("admin")
        userSummaryPage.waitForElementToBeClickable(userSummaryPage.modalSearchButton)
        userSummaryPage.modalSearchButton.click()

        then: "The user count field should show '1'"
        userSummaryPage.waitForTextToBePresentBySelector(userSummaryPage.userCountFieldBy, "1", 10)
    }
}
