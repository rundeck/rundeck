package org.rundeck.tests.functional.selenium.ldap


import org.rundeck.util.annotations.LdapTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoggedOutPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.usersummary.UserSummaryPage

@LdapTest
class LdapLoginSpec extends SeleniumBase {


    /**
     * Test case to verify that logging in with ldapSync=true user property doesn't duplicate users.
     */
    def "login on ldap with ldapSync=true user property doesn't duplicate users"() {
        when:"login a first time with two different users"
        def loginPage = go LoginPage
        def topMenuPage = page TopMenuPage
        def loggedOutPage = page LoggedOutPage
        def userSummaryPage = page UserSummaryPage
        //first login
        loginPage.login("user", "user")
        topMenuPage.logOut()
        loggedOutPage.getLoginAgainField().click()
        //second login
        loginPage.login("jdoe", "jdoe")
        userSummaryPage.go()
        then:"only two users should appear on the user summary page"
        userSummaryPage.waitForTextToBePresentBySelector(userSummaryPage.userCountFieldBy,"2",5)
    }
}
