package rundeckapp

import groovy.sql.Sql
import org.h2.jdbcx.JdbcDataSource
import rundeck.data.util.AuthenticationTokenUtils
import org.rundeck.app.data.model.v1.authtoken.AuthTokenMode
import spock.lang.Specification

class BootStrapTest  extends Specification {
    def "Test convertToTokenMap"() {
        given:
        Properties userMap = new Properties().tap {
            load(new StringReader("""\
                |user1=${token("0")}
                |user2=${token("1")},role1
                |user3=${token("2")},role1,role2
                |user4=${token("3")};${token("4")},role1
                """.stripMargin()))
        }
        when:
        Properties tokenMap = BootStrap.convertToTokenMap(userMap)

        then:
        tokenMap.getProperty(token("0")) == "user1,api_token_group"
        tokenMap.getProperty(token("1")) == "user2,role1"
        tokenMap.getProperty(token("2")) == "user3,role1,role2"
        tokenMap.getProperty(token("3")) == "user4,role1"
        tokenMap.getProperty(token("4")) == "user4,role1"
    }

    def "migrateWebhookTokensToSecured hashes LEGACY and null-mode webhook rows, leaves everything else alone"() {
        given:
        def dataSource = new JdbcDataSource()
        dataSource.setURL("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1")
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

        def bootStrap = new BootStrap()
        bootStrap.dataSource = dataSource

        when:
        bootStrap.migrateWebhookTokensToSecured()
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
        def dataSource = new JdbcDataSource()
        dataSource.setURL("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1")
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
        def bootStrap = new BootStrap()
        bootStrap.dataSource = dataSource

        when:
        bootStrap.migrateWebhookTokensToSecured()
        def afterFirstRun = sql.rows("SELECT token, token_mode FROM auth_token")[0]
        bootStrap.migrateWebhookTokensToSecured()
        def afterSecondRun = sql.rows("SELECT token, token_mode FROM auth_token")[0]

        then:
        afterFirstRun.token == afterSecondRun.token
        afterFirstRun.token_mode == afterSecondRun.token_mode == 'SECURED'

        cleanup:
        sql.close()
    }

    private String token(String suffix) {
        return "0123456701234567012345670000000${suffix}"
    }
}
