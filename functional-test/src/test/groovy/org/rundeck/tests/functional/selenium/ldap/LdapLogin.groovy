package org.rundeck.tests.functional.selenium.ldap

import org.rundeck.util.annotations.LdapTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoggedOutPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.usersummary.UserSummaryPage

@LdapTest
class LdapLogin extends SeleniumBase {


    def "ldap login"() {
        when:
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


        def result= userSummaryPage.getUserCountNumberToBe("2",3)
        then:
        result == "2"
    }
}
