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
 * Verifies via POST to j_security_check then GET /api/user/roles with the session cookie.
 */
@LdapTest
class LdapRolesApiSpec extends BaseContainer {

    static final String LDAP_USER = "jdoe"
    static final String LDAP_PASS = "jdoe"
    static final List<String> EXPECTED_ROLES = ["admin", "user", "architect", "build", "deploy"]

    def "LDAP user roles are populated in Subject after session login"() {
        given: "a session-based HTTP client that stores cookies"
        def cookieJar = new CustomCookieJar()
        def sessionClient = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build()

        when: "the LDAP user logs in via j_security_check"
        try (def warmupResponse = sessionClient.newCall(new Request.Builder()
                .url(client.baseUrl)
                .get()
                .build()).execute()) {
            warmupResponse.body().string()
        }
        try (def loginResponse = sessionClient.newCall(new Request.Builder()
                .url("${client.baseUrl}/j_security_check")
                .post(new FormBody.Builder()
                        .add("j_username", LDAP_USER)
                        .add("j_password", LDAP_PASS)
                        .build())
                .build()).execute()) {
            loginResponse.body().string()
        }

        and: "the roles endpoint is called with the session cookie"
        def roles
        try (def rolesResponse = sessionClient.newCall(new Request.Builder()
                .url("${client.baseUrl}/api/${client.apiVersion}/user/roles")
                .get()
                .build()).execute()) {
            assert rolesResponse.code() == 200: "Roles endpoint returned ${rolesResponse.code()} — login may have failed"
            roles = (MAPPER.readValue(rolesResponse.body().string(), Map).roles ?: []) as List
        }

        then: "the LDAP user has the expected group roles — not an empty list"
        !roles.isEmpty()
        roles.containsAll(EXPECTED_ROLES)
    }
}
