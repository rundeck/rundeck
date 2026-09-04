package rundeck.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.data.model.v1.authtoken.AuthTokenMode
import org.rundeck.app.data.model.v1.authtoken.AuthTokenType
import rundeck.AuthToken
import rundeck.User
import rundeck.data.util.AuthenticationTokenUtils
import spock.lang.Specification

class WebhookTokenSecureMigrationServiceSpec extends Specification
        implements ServiceUnitTest<WebhookTokenSecureMigrationService>, DataTest {

    void setup() {
        mockDomains(AuthToken, User)
    }

    private AuthToken saveToken(String token, AuthTokenType type, AuthTokenMode tokenMode) {
        User owner = new User(login: "user-${token}").save(flush: true, failOnError: true)
        new AuthToken(
                user: owner,
                uuid: "uuid-${token}",
                authRoles: 'webhook,test',
                token: token,
                type: type,
                tokenMode: tokenMode,
        ).save(flush: true, failOnError: true)
    }

    def "migrateWebhookTokensToSecured hashes LEGACY and null-mode webhook rows, leaves everything else alone"() {
        given:
        saveToken('legacy-clear-1', AuthTokenType.WEBHOOK, AuthTokenMode.LEGACY)
        saveToken('legacy-clear-2', AuthTokenType.WEBHOOK, null)
        saveToken('already-secured', AuthTokenType.WEBHOOK, AuthTokenMode.SECURED)
        saveToken('user-token-clear', AuthTokenType.USER, AuthTokenMode.LEGACY)

        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
        }

        when:
        service.migrateWebhookTokensToSecured()

        then:
        AuthToken.findByUuid('uuid-legacy-clear-1').with {
            it.token == AuthenticationTokenUtils.encodeTokenValue('legacy-clear-1', AuthTokenMode.SECURED)
            it.tokenMode == AuthTokenMode.SECURED
        }
        AuthToken.findByUuid('uuid-legacy-clear-2').with {
            it.token == AuthenticationTokenUtils.encodeTokenValue('legacy-clear-2', AuthTokenMode.SECURED)
            it.tokenMode == AuthTokenMode.SECURED
        }
        // already SECURED: untouched
        AuthToken.findByUuid('uuid-already-secured').with {
            it.token == 'already-secured'
            it.tokenMode == AuthTokenMode.SECURED
        }
        // not a WEBHOOK token: untouched even though it's LEGACY
        AuthToken.findByUuid('uuid-user-token-clear').with {
            it.token == 'user-token-clear'
            it.tokenMode == AuthTokenMode.LEGACY
        }
    }

    def "migrateWebhookTokensToSecured is a no-op the second time it runs"() {
        given:
        saveToken('legacy-clear-1', AuthTokenType.WEBHOOK, AuthTokenMode.LEGACY)
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
        }

        when:
        service.migrateWebhookTokensToSecured()
        AuthToken afterFirstRun = AuthToken.findByUuid('uuid-legacy-clear-1')
        String tokenAfterFirstRun = afterFirstRun.token
        AuthTokenMode modeAfterFirstRun = afterFirstRun.tokenMode
        service.migrateWebhookTokensToSecured()
        AuthToken afterSecondRun = AuthToken.findByUuid('uuid-legacy-clear-1')

        then:
        tokenAfterFirstRun == afterSecondRun.token
        modeAfterFirstRun == AuthTokenMode.SECURED
        afterSecondRun.tokenMode == AuthTokenMode.SECURED
    }

    def "migrateWebhookTokensToSecured does nothing when this is not the primary server in a cluster"() {
        given:
        saveToken('legacy-clear-1', AuthTokenType.WEBHOOK, AuthTokenMode.LEGACY)
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
            getServerUUID() >> "not-the-primary"
        }
        service.configurationService = Mock(ConfigurationService) {
            getString("primaryServerId") >> "some-other-uuid"
        }

        when:
        service.migrateWebhookTokensToSecured()
        AuthToken row = AuthToken.findByUuid('uuid-legacy-clear-1')

        then:
        row.token == 'legacy-clear-1'
        row.tokenMode == AuthTokenMode.LEGACY
    }

    def "migrateWebhookTokensToSecured runs when this server is the configured primary in a cluster"() {
        given:
        saveToken('legacy-clear-1', AuthTokenType.WEBHOOK, AuthTokenMode.LEGACY)
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
            getServerUUID() >> "this-server-uuid"
        }
        service.configurationService = Mock(ConfigurationService) {
            getString("primaryServerId") >> "this-server-uuid"
        }

        when:
        service.migrateWebhookTokensToSecured()
        AuthToken row = AuthToken.findByUuid('uuid-legacy-clear-1')

        then:
        row.token == AuthenticationTokenUtils.encodeTokenValue('legacy-clear-1', AuthTokenMode.SECURED)
        row.tokenMode == AuthTokenMode.SECURED
    }

    def "migrateWebhookTokensToSecured runs in cluster mode when no primaryServerId is configured"() {
        given:
        saveToken('legacy-clear-1', AuthTokenType.WEBHOOK, AuthTokenMode.LEGACY)
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
            getServerUUID() >> "this-server-uuid"
        }
        service.configurationService = Mock(ConfigurationService) {
            getString("primaryServerId") >> null
        }

        when:
        service.migrateWebhookTokensToSecured()
        AuthToken row = AuthToken.findByUuid('uuid-legacy-clear-1')

        then:
        row.token == AuthenticationTokenUtils.encodeTokenValue('legacy-clear-1', AuthTokenMode.SECURED)
        row.tokenMode == AuthTokenMode.SECURED
    }
}
