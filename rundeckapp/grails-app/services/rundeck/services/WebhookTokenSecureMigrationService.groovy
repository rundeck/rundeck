package rundeck.services

import grails.events.annotation.Subscriber
import org.rundeck.app.data.model.v1.authtoken.AuthTokenMode
import org.rundeck.app.data.model.v1.authtoken.AuthTokenType
import rundeck.AuthToken
import rundeck.data.util.AuthenticationTokenUtils

/**
 * One-time startup fixup (PS-1686): re-hashes any WEBHOOK auth_token rows still in LEGACY
 * or null token_mode to SECURED, using the same SHA-256 helper every other token type
 * already uses. Each row is re-hashed in its own transaction/session (via
 * {@code AuthToken.withNewTransaction}) so a single bad row can't abort the batch or poison
 * the Hibernate session for the rest, and the migration is naturally idempotent — converted
 * rows no longer match the LEGACY/null filter on the next boot. The webhook's public secret
 * lives in the separate {@code webhook.auth_token} column and is never touched here, so
 * existing webhook URLs keep working unchanged.
 *
 * Uses GORM rather than raw SQL: Hibernate's dialect layer already handles cross-database
 * differences for every query here, so there is no vendor-specific SQL to reason about
 * (unlike the earlier raw-SQL version of this fixup).
 *
 * Triggered via the {@code rundeck.bootstrap} event (fired once by {@code BootStrap.groovy}
 * near the end of server startup) rather than being invoked directly from BootStrap, per
 * PS-1686 review feedback.
 */
class WebhookTokenSecureMigrationService {

    def frameworkService
    def configurationService

    /**
     * Subscribes to the {@code rundeck.bootstrap} event and, if this server is allowed to
     * apply server updates (see {@link #canApplyServerUpdates}), migrates any WEBHOOK
     * auth_token rows still in LEGACY or null token_mode to SECURED.
     */
    @Subscriber('rundeck.bootstrap')
    void migrateWebhookTokensToSecured() {
        if (!canApplyServerUpdates()) {
            return
        }
        try {
            List<AuthToken> rows = AuthToken.createCriteria().list {
                eq('type', AuthTokenType.WEBHOOK)
                or {
                    isNull('tokenMode')
                    eq('tokenMode', AuthTokenMode.LEGACY)
                }
            } as List<AuthToken>

            int converted = 0
            rows.each { AuthToken row ->
                try {
                    AuthToken.withNewTransaction {
                        AuthToken fresh = AuthToken.get(row.id)
                        if (fresh && (fresh.tokenMode == null || fresh.tokenMode == AuthTokenMode.LEGACY)) {
                            fresh.token = AuthenticationTokenUtils.encodeTokenValue(fresh.token, AuthTokenMode.SECURED)
                            fresh.tokenMode = AuthTokenMode.SECURED
                            fresh.save(flush: true, failOnError: true)
                            converted++
                        }
                    }
                } catch (Exception rowEx) {
                    log.error("Unable to migrate webhook auth_token id ${row.id} to SECURED mode: ", rowEx)
                }
            }
            if (converted) {
                log.info("Migrated ${converted} webhook auth_token row(s) from LEGACY to SECURED mode")
            }
        } catch (Exception ex) {
            log.warn("Unable to migrate webhook auth tokens to SECURED mode. Please investigate the auth_token table manually.")
            log.error("Webhook token migration error: ", ex)
        }
    }

    /**
     * Reconstructs the same "should this server apply one-time startup migrations" gate
     * that {@code BootStrap.groovy} computes locally, but using only injectable Grails
     * services: outside cluster mode every server may apply updates; in cluster mode, only
     * the configured {@code primaryServerId} (if any) may.
     */
    private boolean canApplyServerUpdates() {
        if (!frameworkService.isClusterModeEnabled()) {
            return true
        }
        String primaryServerId = configurationService.getString("primaryServerId")
        return primaryServerId ? primaryServerId == frameworkService.getServerUUID() : true
    }
}
