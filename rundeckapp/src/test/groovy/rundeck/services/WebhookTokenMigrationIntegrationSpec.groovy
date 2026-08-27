package rundeck.services

import groovy.sql.Sql
import org.rundeck.app.data.model.v1.authtoken.AuthTokenMode
import rundeck.data.util.AuthenticationTokenUtils
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.DriverManager

/**
 * End-to-end regression coverage for PS-1686: a webhook created before this fix — auth_token
 * row in LEGACY mode, webhook.auth_token holding that same plaintext value — must keep
 * authenticating with the exact same raw token value both BEFORE and AFTER
 * {@link WebhookTokenSecureMigrationService#migrateWebhookTokensToSecured} runs.
 *
 * This ties together the two independent storage paths involved in webhook auth (the
 * auth_token row consulted by {@code GormTokenDataProvider.tokenLookupWithType}, and the
 * separate webhook.auth_token column consulted by {@code Webhook.findByAuthToken}) plus the
 * migration itself. {@code GormTokenDataProviderSpec}'s "token lookup test" already proves
 * the hybrid SECURED-hash-or-LEGACY-raw lookup correctly resolves a SECURED row by re-hashing
 * the caller's raw input; what's verified here is the piece that closes the loop: the
 * migration produces exactly that SECURED shape (raw input hashes to the persisted value)
 * while leaving the webhook's separate public-secret column untouched.
 */
class WebhookTokenMigrationIntegrationSpec extends Specification {

    Sql sql
    WebhookTokenSecureMigrationService migrationService

    def setup() {
        String jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        DataSource dataSource = [getConnection: { -> DriverManager.getConnection(jdbcUrl) }] as DataSource
        sql = new Sql(dataSource)
        sql.execute('''
            CREATE TABLE auth_token (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                token VARCHAR(255) NOT NULL UNIQUE,
                type VARCHAR(255),
                token_mode VARCHAR(255)
            )
        ''')
        sql.execute('''
            CREATE TABLE webhook (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                uuid VARCHAR(255),
                name VARCHAR(255) NOT NULL,
                project VARCHAR(255) NOT NULL,
                auth_token VARCHAR(255) NOT NULL,
                event_plugin VARCHAR(255) NOT NULL
            )
        ''')

        migrationService = new WebhookTokenSecureMigrationService()
        migrationService.dataSource = dataSource
        migrationService.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
        }
    }

    def cleanup() {
        sql.close()
    }

    def "an old LEGACY webhook token keeps resolving via both storage paths after the SECURED migration runs"() {
        given: "a webhook created before PS-1686: auth_token is LEGACY, and webhook.auth_token holds the same plaintext value"
        String clearToken = 'old-plaintext-webhook-secret'
        sql.execute("INSERT INTO auth_token (token, type, token_mode) VALUES (${clearToken}, 'WEBHOOK', 'LEGACY')")
        sql.execute("INSERT INTO webhook (uuid, name, project, auth_token, event_plugin) VALUES ('old-webhook', 'legacy-webhook', 'Test', ${clearToken}, 'log-webhook-event')")

        when: "the webhook is triggered before the migration runs"
        def webhookRowBefore = sql.firstRow("SELECT auth_token FROM webhook WHERE auth_token = ${clearToken}")
        def authTokenRowBefore = sql.firstRow("SELECT token, token_mode FROM auth_token WHERE type = 'WEBHOOK' AND token = ${clearToken}")

        then: "both independent lookups resolve using the raw clear-text value — the webhook works"
        webhookRowBefore?.auth_token == clearToken
        authTokenRowBefore?.token == clearToken
        authTokenRowBefore?.token_mode == 'LEGACY'

        when: "the startup migration re-hashes existing LEGACY webhook tokens"
        migrationService.migrateWebhookTokensToSecured()

        and: "the webhook is triggered again after the migration, with the SAME raw token"
        def webhookRowAfter = sql.firstRow("SELECT auth_token FROM webhook WHERE auth_token = ${clearToken}")
        String expectedHash = AuthenticationTokenUtils.encodeTokenValue(clearToken, AuthTokenMode.SECURED)
        def authTokenRowAfter = sql.firstRow("SELECT token, token_mode FROM auth_token WHERE type = 'WEBHOOK' AND token = ${expectedHash}")

        then: "the webhook definition still resolves by the identical raw value — its URL is unaffected"
        webhookRowAfter?.auth_token == clearToken

        and: "the auth_token row is now SECURED, and its hash is exactly what re-hashing the original raw value produces"
        authTokenRowAfter?.token == expectedHash
        authTokenRowAfter?.token_mode == 'SECURED'

        and: "no row is left behind under the old plaintext value — it was converted in place, not duplicated"
        sql.firstRow("SELECT COUNT(*) AS c FROM auth_token WHERE type = 'WEBHOOK' AND token = ${clearToken}").c == 0
    }
}
