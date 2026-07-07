package org.rundeck.tests.functional.selenium.ldap


import groovy.json.JsonSlurper
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.annotations.LdapTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoggedOutPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.usersummary.UserSummaryPage

import java.time.Duration

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
        // Wait for logout to complete and logged out page to load
        loggedOutPage.waitForUrlToContain("/user/loggedout")
        loggedOutPage.getLoginAgainField().click()
        // Wait for login page to load after clicking "Login Again"
        loginPage.waitForUrlToContain("/user/login")
        //second login
        loginPage.login("jdoe", "jdoe")
        // Wait for login to complete before navigating to user summary
        topMenuPage.waitForUrlToNotContain("/user/login")

        when: "checking LDAP user roles from the authenticated browser session"
        driver.get("${client.baseUrl}/api/30/user/roles")
        def roleResponseWait = new WebDriverWait(driver, Duration.ofSeconds(8))
        roleResponseWait.until { it.findElement(By.tagName("body")).text?.contains('"roles"') }
        def rolesResponse = new JsonSlurper().parseText(driver.findElement(By.tagName("body")).text) as Map

        then: "LDAP user has expected authorization roles"
        def expectedRoles = ["admin", "user", "architect", "build", "deploy"]
        def actualRoles = (rolesResponse.roles ?: []) as Collection<String>
        actualRoles.containsAll(expectedRoles)

        when: "opening user summary"
        userSummaryPage.go()

        then:"only two users should appear on the user summary page"
        userSummaryPage.waitForTextToBePresentBySelector(userSummaryPage.userCountFieldBy,"2",5)
    }
}
