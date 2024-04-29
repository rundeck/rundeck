package org.rundeck.tests.functional.selenium.ldap

import org.openqa.selenium.By
import org.rundeck.util.annotations.LdapTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoggedOutPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.usersummary.UserSummaryPage

@LdapTest
class LdapLogin extends SeleniumBase {


    def "login on ldap with ldapSync=true user property doesn't duplicate users"() {
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
        userSummaryPage.waitForTextToBePresentBySelector(By.className("text-info"),"2",5)
        then:
        userSummaryPage.getUserCountField().getText() == "2"
    }
}
