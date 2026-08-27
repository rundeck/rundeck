package rundeck.services

import grails.events.annotation.Subscriber
import groovy.sql.Sql
import org.rundeck.app.data.model.v1.authtoken.AuthTokenMode
import rundeck.data.util.AuthenticationTokenUtils

/**
 * One-time startup fixup (PS-1686): re-hashes any WEBHOOK auth_token rows still in LEGACY
 * or null token_mode to SECURED, using the same SHA-256 helper every other token type
 * already uses. Runs one row per statement so a single bad row can't abort the batch, and
 * is naturally idempotent — converted rows no longer match the LEGACY/null filter on the
 * next boot. The webhook's public secret lives in the separate `webhook.auth_token` column
 * and is never touched here, so existing webhook URLs keep working unchanged.
 *
 * The SELECT/UPDATE statements below use only ANSI-standard predicates (IS NULL, =, AND/OR)
 * and JDBC positional (?) placeholders — no vendor-specific functions, sequences, or DDL.
 * Rundeck's supported databases (MySQL/MariaDB, PostgreSQL, H2, Oracle, MSSQL) all handle
 * this construct identically via groovy.sql.Sql/PreparedStatement. The Oracle-specific
 * quirks that do exist elsewhere in this codebase (see the `dbms: "oracle"` Liquibase
 * changesets, e.g. ExecReportJcExecIdToExecutionId.groovy's bigint->NUMBER(19,0) cast, or
 * the sequence-existence checks in HibernateIndex.groovy) are all DDL/type-system
 * differences that don't apply to this simple DML SELECT/UPDATE.
 *
 * Triggered via the {@code rundeck.bootstrap} event (fired once by {@code BootStrap.groovy}
 * near the end of server startup) rather than being invoked directly from BootStrap, per
 * PS-1686 review feedback.
 */
class WebhookTokenSecureMigrationService {

    def dataSource
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
        Sql sql = new Sql(dataSource)
        try {
            List<groovy.sql.GroovyRowResult> rows = sql.rows(
                    "SELECT id, token FROM auth_token WHERE type = 'WEBHOOK' AND (token_mode IS NULL OR token_mode = 'LEGACY')"
            )
            int converted = 0
            rows.each { row ->
                try {
                    String hashed = AuthenticationTokenUtils.encodeTokenValue(row.token as String, AuthTokenMode.SECURED)
                    converted += sql.executeUpdate(
                            "UPDATE auth_token SET token = ?, token_mode = 'SECURED' WHERE id = ? AND token = ?",
                            [hashed, row.id, row.token]
                    )
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
