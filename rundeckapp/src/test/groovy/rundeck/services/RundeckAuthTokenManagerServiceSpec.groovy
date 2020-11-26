package rundeck.services

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenMode
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import rundeck.AuthToken
import rundeck.User
import spock.lang.Specification

class RundeckAuthTokenManagerServiceSpec extends Specification
        implements ServiceUnitTest<RundeckAuthTokenManagerService>, DataTest {


    def setup() {
        mockDomain(AuthToken)
        mockDomain(User)
    }

    def "importWebhookToken"() {
        given:
            service.apiService = Mock(ApiService)
            def auth = Mock(UserAndRolesAuthContext)
            def token = '123'
            def user = 'auser'
            def roles = new HashSet(['a', 'b', 'c'])
        when:
            service.importWebhookToken(auth, token, user, roles)
        then:
            1 * service.apiService.createUserToken(auth, 0, token, user, roles, false, AuthTokenType.WEBHOOK)
    }

    def "importWebhookToken existing"() {
        given:
            service.apiService = Mock(ApiService)
            User user1 = new User(login: 'auser')
            user1.save()
            AuthToken existing = new AuthToken(
                    token: '123',
                    authRoles: 'a,b',
                    user: user1,
                    type: AuthTokenType.WEBHOOK,
                    mode: AuthTokenMode.LEGACY
            )
            existing.save(flush: true)
            def auth = Mock(UserAndRolesAuthContext)
            def token = '123'
            def user = 'auser'
            def roles = new HashSet(['a', 'b', 'c'])
        when:
            service.importWebhookToken(auth, token, user, roles)
        then:
            AuthToken updated = AuthToken.get(existing.id)
            updated.authRoles == 'a,b,c'
            1 * service.apiService.checkTokenAuthorization(auth, 'auser', roles) >>
            new ApiService.TokenRolesAuthCheck(authorized: true, roles: roles, user: user)
            0 * service.apiService.createUserToken(auth, 0, token, user, roles, false, AuthTokenType.WEBHOOK)

    }

    def "importWebhookToken existing user token"() {
        given:
            service.apiService = Mock(ApiService)
            User user1 = new User(login: 'auser')
            user1.save()
            AuthToken existing = new AuthToken(
                    token: '123',
                    authRoles: 'a,b',
                    user: user1,
                    type: AuthTokenType.USER
            )
            existing.save(flush: true)
            def auth = Mock(UserAndRolesAuthContext)
            def token = '123'
            def user = 'auser'
            def roles = new HashSet(['a', 'b', 'c'])
        when:
            service.importWebhookToken(auth, token, user, roles)
        then:
            Exception e = thrown()
            e.message == 'Cannot import webhook token'
            0 * service.apiService.checkTokenAuthorization(auth, 'auser', roles)
            0 * service.apiService.createUserToken(auth, 0, token, user, roles, false, AuthTokenType.WEBHOOK)

    }

    def cleanup() {
    }

}
