package org.rundeck.tests.functional.selenium.ldap

import groovy.json.JsonSlurper
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.annotations.LdapTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoginPage

import java.time.Duration

/**
 * Regression test for RUN-4599: LDAP users must have roles in their JAAS Subject after login.
 *
 * Before the fix in AbstractLoginModule.commit(), LDAP users could authenticate successfully
 * but had an empty Subject (no RundeckRole principals), causing all project access to be rejected.
 * This test verifies roles are present via the /api/user/roles endpoint in the same browser session.
 */
@LdapTest
class LdapRolesSpec extends SeleniumBase {

    static final String LDAP_USER = "jdoe"
    static final String LDAP_PASS = "jdoe"
    // Roles assigned to jdoe in functional-test/src/test/resources/docker/ldap/ldif/50-bootstrap.ldif
    static final List<String> EXPECTED_ROLES = ["admin", "user", "architect", "build", "deploy"]

    def "LDAP user has authorization roles in Subject after login"() {
        given:
        def loginPage = go LoginPage
        def topMenuPage = page TopMenuPage

        when: "LDAP user logs in"
        loginPage.login(LDAP_USER, LDAP_PASS)
        topMenuPage.waitForUrlToNotContain("/user/login")

        and: "the roles endpoint is called in the same browser session"
        driver.get("${client.baseUrl}/api/${client.apiVersion}/user/roles")
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until { it.findElement(By.tagName("body")).text?.contains('"roles"') }
        def rolesResponse = new JsonSlurper().parseText(
            driver.findElement(By.tagName("body")).text
        ) as Map

        then: "the LDAP user has the expected group roles — not an empty list"
        def actualRoles = (rolesResponse.roles ?: []) as Collection<String>
        !actualRoles.isEmpty()
        actualRoles.containsAll(EXPECTED_ROLES)

        cleanup:
        topMenuPage.logOut()
    }
}
