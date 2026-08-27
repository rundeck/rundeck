package rundeck.services

import grails.testing.services.ServiceUnitTest
import groovy.sql.Sql
import org.rundeck.app.data.model.v1.authtoken.AuthTokenMode
import rundeck.data.util.AuthenticationTokenUtils
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.DriverManager

class WebhookTokenSecureMigrationServiceSpec extends Specification
        implements ServiceUnitTest<WebhookTokenSecureMigrationService> {

    def "migrateWebhookTokensToSecured hashes LEGACY and null-mode webhook rows, leaves everything else alone"() {
        given:
        def dataSource = h2DataSource()
        def sql = new Sql(dataSource)
        sql.execute('''
            CREATE TABLE auth_token (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                token VARCHAR(255) NOT NULL UNIQUE,
                type VARCHAR(255),
                token_mode VARCHAR(255)
            )
        ''')
        sql.execute("INSERT INTO auth_token (token, type, token_mode) VALUES ('legacy-clear-1', 'WEBHOOK', 'LEGACY')")
        sql.execute("INSERT INTO auth_token (token, type, token_mode) VALUES ('legacy-clear-2', 'WEBHOOK', NULL)")
        sql.execute("INSERT INTO auth_token (token, type, token_mode) VALUES ('already-secured', 'WEBHOOK', 'SECURED')")
        sql.execute("INSERT INTO auth_token (token, type, token_mode) VALUES ('user-token-clear', 'USER', 'LEGACY')")

        service.dataSource = dataSource
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
        }

        when:
        service.migrateWebhookTokensToSecured()
        def rows = sql.rows("SELECT token, type, token_mode FROM auth_token ORDER BY id")

        then:
        rows[0].token == AuthenticationTokenUtils.encodeTokenValue('legacy-clear-1', AuthTokenMode.SECURED)
        rows[0].token_mode == 'SECURED'
        rows[1].token == AuthenticationTokenUtils.encodeTokenValue('legacy-clear-2', AuthTokenMode.SECURED)
        rows[1].token_mode == 'SECURED'
        // already SECURED: untouched
        rows[2].token == 'already-secured'
        rows[2].token_mode == 'SECURED'
        // not a WEBHOOK token: untouched even though it's LEGACY
        rows[3].token == 'user-token-clear'
        rows[3].token_mode == 'LEGACY'

        cleanup:
        sql.close()
    }

    def "migrateWebhookTokensToSecured is a no-op the second time it runs"() {
        given:
        def dataSource = h2DataSource()
        def sql = new Sql(dataSource)
        sql.execute('''
            CREATE TABLE auth_token (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                token VARCHAR(255) NOT NULL UNIQUE,
                type VARCHAR(255),
                token_mode VARCHAR(255)
            )
        ''')
        sql.execute("INSERT INTO auth_token (token, type, token_mode) VALUES ('legacy-clear-1', 'WEBHOOK', 'LEGACY')")

        service.dataSource = dataSource
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
        }

        when:
        service.migrateWebhookTokensToSecured()
        def afterFirstRun = sql.rows("SELECT token, token_mode FROM auth_token")[0]
        service.migrateWebhookTokensToSecured()
        def afterSecondRun = sql.rows("SELECT token, token_mode FROM auth_token")[0]

        then:
        afterFirstRun.token == afterSecondRun.token
        afterFirstRun.token_mode == 'SECURED'
        afterSecondRun.token_mode == 'SECURED'

        cleanup:
        sql.close()
    }

    def "migrateWebhookTokensToSecured does nothing when this is not the primary server in a cluster"() {
        given:
        def dataSource = h2DataSource()
        def sql = new Sql(dataSource)
        sql.execute('''
            CREATE TABLE auth_token (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                token VARCHAR(255) NOT NULL UNIQUE,
                type VARCHAR(255),
                token_mode VARCHAR(255)
            )
        ''')
        sql.execute("INSERT INTO auth_token (token, type, token_mode) VALUES ('legacy-clear-1', 'WEBHOOK', 'LEGACY')")

        service.dataSource = dataSource
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
            getServerUUID() >> "not-the-primary"
        }
        service.configurationService = Mock(ConfigurationService) {
            getString("primaryServerId") >> "some-other-uuid"
        }

        when:
        service.migrateWebhookTokensToSecured()
        def row = sql.rows("SELECT token, token_mode FROM auth_token")[0]

        then:
        row.token == 'legacy-clear-1'
        row.token_mode == 'LEGACY'

        cleanup:
        sql.close()
    }

    def "migrateWebhookTokensToSecured runs when this server is the configured primary in a cluster"() {
        given:
        def dataSource = h2DataSource()
        def sql = new Sql(dataSource)
        sql.execute('''
            CREATE TABLE auth_token (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                token VARCHAR(255) NOT NULL UNIQUE,
                type VARCHAR(255),
                token_mode VARCHAR(255)
            )
        ''')
        sql.execute("INSERT INTO auth_token (token, type, token_mode) VALUES ('legacy-clear-1', 'WEBHOOK', 'LEGACY')")

        service.dataSource = dataSource
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
            getServerUUID() >> "this-server-uuid"
        }
        service.configurationService = Mock(ConfigurationService) {
            getString("primaryServerId") >> "this-server-uuid"
        }

        when:
        service.migrateWebhookTokensToSecured()
        def row = sql.rows("SELECT token, token_mode FROM auth_token")[0]

        then:
        row.token == AuthenticationTokenUtils.encodeTokenValue('legacy-clear-1', AuthTokenMode.SECURED)
        row.token_mode == 'SECURED'

        cleanup:
        sql.close()
    }

    def "migrateWebhookTokensToSecured runs in cluster mode when no primaryServerId is configured"() {
        given:
        def dataSource = h2DataSource()
        def sql = new Sql(dataSource)
        sql.execute('''
            CREATE TABLE auth_token (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                token VARCHAR(255) NOT NULL UNIQUE,
                type VARCHAR(255),
                token_mode VARCHAR(255)
            )
        ''')
        sql.execute("INSERT INTO auth_token (token, type, token_mode) VALUES ('legacy-clear-1', 'WEBHOOK', 'LEGACY')")

        service.dataSource = dataSource
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
            getServerUUID() >> "this-server-uuid"
        }
        service.configurationService = Mock(ConfigurationService) {
            getString("primaryServerId") >> null
        }

        when:
        service.migrateWebhookTokensToSecured()
        def row = sql.rows("SELECT token, token_mode FROM auth_token")[0]

        then:
        row.token == AuthenticationTokenUtils.encodeTokenValue('legacy-clear-1', AuthTokenMode.SECURED)
        row.token_mode == 'SECURED'

        cleanup:
        sql.close()
    }

    /**
     * Builds a plain {@link DataSource} backed by a fresh H2 in-memory database, reachable via
     * {@link DriverManager}. This avoids a compile-time dependency on {@code org.h2.jdbcx.JdbcDataSource}
     * (H2 is only declared {@code runtimeOnly} in this module, so it is present on the test runtime
     * classpath but not the test compile classpath); the H2 driver self-registers with
     * {@link DriverManager} via the JDBC 4 service-loader mechanism once its jar is on the runtime
     * classpath, so no explicit driver class reference is needed here either.
     */
    private static DataSource h2DataSource() {
        String jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        return [getConnection: { -> DriverManager.getConnection(jdbcUrl) }] as DataSource
    }
}
