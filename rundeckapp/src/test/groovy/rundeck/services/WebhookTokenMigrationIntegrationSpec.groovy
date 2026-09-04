package rundeck.services

import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.authtoken.AuthTokenMode
import org.rundeck.app.data.model.v1.authtoken.AuthTokenType
import org.rundeck.app.data.providers.GormTokenDataProvider
import rundeck.AuthToken
import rundeck.User
import spock.lang.Specification
import webhooks.Webhook

/**
 * End-to-end regression coverage for PS-1686: a webhook created before this fix — auth_token
 * row in LEGACY mode, webhook.auth_token holding that same plaintext value — must keep
 * authenticating with the exact same raw token value both BEFORE and AFTER
 * {@link WebhookTokenSecureMigrationService#migrateWebhookTokensToSecured} runs.
 *
 * This ties together the two independent storage paths involved in webhook auth (the
 * auth_token row consulted by {@code GormTokenDataProvider.tokenLookupWithType}, and the
 * separate webhook.auth_token column consulted by {@code Webhook.findByAuthToken}) plus the
 * migration itself.
 */
class WebhookTokenMigrationIntegrationSpec extends Specification implements DataTest {

    GormTokenDataProvider tokenProvider = new GormTokenDataProvider()
    WebhookTokenSecureMigrationService migrationService = new WebhookTokenSecureMigrationService()

    void setup() {
        mockDomains(AuthToken, User, Webhook)
        migrationService.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
        }
    }

    def "an old LEGACY webhook token keeps authenticating after the SECURED migration runs"() {
        given: "a webhook created before PS-1686: the auth_token row is LEGACY, and the webhook's public secret is the same plaintext value"
        User owner = new User(login: 'webhookUser').save(flush: true, failOnError: true)
        String clearToken = 'old-plaintext-webhook-secret'
        new AuthToken(
                user: owner,
                uuid: 'old-webhook-token',
                authRoles: 'webhook,test',
                token: clearToken,
                tokenMode: AuthTokenMode.LEGACY,
                type: AuthTokenType.WEBHOOK,
        ).save(flush: true, failOnError: true)
        new Webhook(
                uuid: 'old-webhook',
                name: 'legacy-webhook',
                project: 'Test',
                authToken: clearToken,
                eventPlugin: 'log-webhook-event',
        ).save(flush: true, failOnError: true)

        when: "the webhook is triggered before the migration runs"
        def webhookBefore = Webhook.findByAuthToken(clearToken)
        def authTokenBefore = tokenProvider.tokenLookupWithType(clearToken, AuthTokenType.WEBHOOK)

        then: "both lookups succeed using the raw clear-text value"
        webhookBefore?.uuid == 'old-webhook'
        authTokenBefore?.uuid == 'old-webhook-token'
        authTokenBefore.getAuthRolesSet() == ['webhook', 'test'] as Set

        when: "the startup migration re-hashes existing LEGACY webhook tokens"
        migrationService.migrateWebhookTokensToSecured()

        and: "the webhook is triggered again after the migration, with the SAME raw token"
        def webhookAfter = Webhook.findByAuthToken(clearToken)
        def authTokenAfter = tokenProvider.tokenLookupWithType(clearToken, AuthTokenType.WEBHOOK)

        then: "both lookups still succeed with the identical raw value — the webhook keeps working"
        webhookAfter?.uuid == 'old-webhook'
        authTokenAfter?.uuid == 'old-webhook-token'
        authTokenAfter.getAuthRolesSet() == ['webhook', 'test'] as Set

        and: "the row is now actually hashed at rest, not left in plaintext"
        AuthToken migrated = AuthToken.findByUuid('old-webhook-token')
        migrated.tokenMode == AuthTokenMode.SECURED
        migrated.token != clearToken

        and: "the webhook's public secret column was never touched by the migration"
        Webhook.findByUuid('old-webhook').authToken == clearToken
    }
}
