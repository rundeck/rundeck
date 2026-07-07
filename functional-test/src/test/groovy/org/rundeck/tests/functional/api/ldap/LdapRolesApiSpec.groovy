package org.rundeck.tests.functional.api.ldap

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.rundeck.util.annotations.LdapTest
import org.rundeck.util.api.common.CustomCookieJar
import org.rundeck.util.container.BaseContainer

/**
 * Regression test for RUN-4599: LDAP users must have roles in their JAAS Subject after login.
 *
 * Before the fix in AbstractLoginModule.commit(), LDAP users could authenticate successfully
 * but had an empty Subject (no RundeckRole principals), causing all project access to be rejected.
 * This test verifies the full stack without a browser: session login via j_security_check,
 * then GET /api/user/roles to confirm roles are populated.
 */
@LdapTest
class LdapRolesApiSpec extends BaseContainer {

    static final String LDAP_USER = "jdoe"
    static final String LDAP_PASS = "jdoe"
    // Roles assigned to jdoe in functional-test/src/test/resources/docker/ldap/ldif/50-bootstrap.ldif
    static final List<String> EXPECTED_ROLES = ["admin", "user", "architect", "build", "deploy"]

    def "LDAP user roles are populated in Subject after session login"() {
        given: "a session-based HTTP client that stores cookies"
        def cookieJar = new CustomCookieJar()
        def sessionClient = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build()

        when: "the LDAP user logs in via j_security_check"
        def loginRequest = new Request.Builder()
                .url("${client.baseUrl}/j_security_check")
                .post(new FormBody.Builder()
                        .add("j_username", LDAP_USER)
                        .add("j_password", LDAP_PASS)
                        .build())
                .build()
        try (def loginResponse = sessionClient.newCall(loginRequest).execute()) {
            !loginResponse.body().string().contains("j_security_check")
        }

        and: "the user roles endpoint is called with the session cookie"
        def rolesRequest = new Request.Builder()
                .url("${client.baseUrl}/api/${client.apiVersion}/user/roles")
                .get()
                .build()

        then: "the response contains the LDAP group roles — not an empty list"
        try (def rolesResponse = sessionClient.newCall(rolesRequest).execute()) {
            rolesResponse.code() == 200
            def body = client.jsonValue(rolesResponse.body(), Map)
            !body.roles.isEmpty()
            body.roles.containsAll(EXPECTED_ROLES)
        }
    }
}
