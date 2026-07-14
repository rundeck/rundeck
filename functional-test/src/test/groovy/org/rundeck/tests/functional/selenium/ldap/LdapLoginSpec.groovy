package org.rundeck.tests.functional.selenium.ldap

import org.rundeck.util.annotations.LdapTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoggedOutPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.profile.UserProfilePage
import org.rundeck.util.gui.pages.usersummary.UserSummaryPage

@LdapTest
class LdapLoginSpec extends SeleniumBase {

    static final List<String> EXPECTED_ROLES = ["admin", "user", "architect", "build", "deploy"]

    def "login on ldap with ldapSync=true user property doesn't duplicate users"() {
        when: "login a first time with two different users"
        def loginPage = go LoginPage
        def topMenuPage = page TopMenuPage
        def loggedOutPage = page LoggedOutPage
        def userProfilePage = page UserProfilePage
        def userSummaryPage = page UserSummaryPage
        //first login
        loginPage.login("user", "user")
        topMenuPage.logOut()
        loggedOutPage.waitForUrlToContain("/user/loggedout")
        loggedOutPage.getLoginAgainField().click()
        loginPage.waitForUrlToContain("/user/login")
        loginPage.waitForElementVisible(loginPage.loginFieldBy)
        //second login
        loginPage.login("jdoe", "jdoe")

        and: "the LDAP user profile shows expected authorization roles"
        userProfilePage.go()
        def groups = userProfilePage.authGroupsText.split(/\s*,\s*/).findAll { it }.toSet()

        then: "LDAP user has expected authorization roles on profile"
        !groups.isEmpty()
        groups.containsAll(EXPECTED_ROLES)

        when: "opening user summary"
        userSummaryPage.go()

        then: "only two users should appear on the user summary page"
        userSummaryPage.waitForTextToBePresentBySelector(userSummaryPage.userCountFieldBy, "2", 5)
    }
}
