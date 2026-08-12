package org.rundeck.tests.functional.api.jdbc

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.rundeck.util.annotations.JdbcTest
import org.rundeck.util.api.common.CustomCookieJar
import org.rundeck.util.container.BaseContainer

/**
 * Regression test for GH-10423: JDBC JAAS users must have roles in their authorization
 * Subject after login.
 *
 * org.eclipse.jetty.security.jaas.spi.JDBCLoginModule adds role principals to the Subject
 * as org.eclipse.jetty.security.jaas.JAASRole, not org.rundeck.jaas.RundeckRole. Before the
 * fix in RundeckJaasAuthorityGranter.grant(), only RundeckRole principals were recognized,
 * so JDBC users authenticated successfully but ended up with zero granted roles.
 * Verifies via POST to j_security_check then GET /api/user/roles with the session cookie.
 */
@JdbcTest
class JdbcRolesApiSpec extends BaseContainer {

    static final String JDBC_USER = "jdbctest"
    static final String JDBC_PASS = "jdbctest"
    static final List<String> EXPECTED_ROLES = ["admin", "user", "architect", "build", "deploy"]

    def "JDBC user roles are populated in Subject after session login"() {
        given: "a session-based HTTP client that stores cookies"
        def cookieJar = new CustomCookieJar()
        def sessionClient = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build()

        when: "the JDBC user logs in via j_security_check"
        try (def warmupResponse = sessionClient.newCall(new Request.Builder()
                .url(client.baseUrl)
                .get()
                .build()).execute()) {
            warmupResponse.body().string()
        }
        try (def loginResponse = sessionClient.newCall(new Request.Builder()
                .url("${client.baseUrl}/j_security_check")
                .post(new FormBody.Builder()
                        .add("j_username", JDBC_USER)
                        .add("j_password", JDBC_PASS)
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

        then: "the JDBC user has the expected group roles — not an empty list"
        !roles.isEmpty()
        roles.containsAll(EXPECTED_ROLES)
    }
}
