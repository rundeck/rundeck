package rundeck.services


import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.data.model.v1.*
import org.rundeck.app.data.providers.GormTokenDataProvider
import org.rundeck.app.data.providers.v1.TokenDataProvider
import org.rundeck.spi.data.DataManager
import rundeck.AuthToken
import rundeck.User
import rundeck.services.data.AuthTokenDataService
import spock.lang.Specification

class RundeckAuthTokenManagerServiceSpec extends Specification
        implements ServiceUnitTest<RundeckAuthTokenManagerService>, DataTest {


    def setup() {
        mockDomain(AuthToken)
        mockDomain(User)
    }

    def "get token calls tokenLookup"(){
        given:
            def token='abc123'
        service.tokenDataProvider = Mock(TokenDataProvider){
            1 * tokenLookup('abc123')>>Mock(AuthenticationToken)
        }
        when:
            def result = service.getToken(token)

        then:
            result instanceof AuthenticationToken
    }

    def "get token with type calls tokenLookupWithType"(){
        given:
            def token='abc123'
        service.tokenDataProvider = Mock(TokenDataProvider) {
            1 * tokenLookupWithType('abc123',type)>>Mock(AuthenticationToken)
        }
        when:
            def result = service.getTokenWithType(token, type)

        then:
            result instanceof AuthenticationToken
        where:
            type <<[
                AuthenticationToken.AuthTokenType.WEBHOOK,
                AuthenticationToken.AuthTokenType.USER,
                AuthenticationToken.AuthTokenType.RUNNER,
            ]
    }

    def "importWebhookToken"() {
        given:
            service.apiService = Mock(ApiService)
            service.tokenDataProvider = Mock(TokenDataProvider)
            def auth = Mock(UserAndRolesAuthContext)
            def token = '123'
            def user = 'auser'
            def roles = new HashSet(['a', 'b', 'c'])
        when:
            service.importWebhookToken(auth, token, user, roles)
        then:
            1 * service.apiService.createUserToken(auth, 0, token, user, roles, false, AuthenticationToken.AuthTokenType.WEBHOOK)
    }

    def "importWebhookToken existing"() {
        given:
            service.apiService = Mock(ApiService)
            def provider = new GormTokenDataProvider()
            mockDataService(AuthTokenDataService)
            provider.authTokenDataService = applicationContext.getBean(AuthTokenDataService)
            service.tokenDataProvider = provider

            User user1 = new User(login: 'auser')
            user1.save()
            AuthToken existing = new AuthToken(
                token: '123',
                authRoles: 'a,b',
                user: user1,
                type: AuthenticationToken.AuthTokenType.WEBHOOK,
                tokenMode: AuthTokenMode.LEGACY
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
            0 * service.apiService.createUserToken(auth, 0, token, user, roles, false, AuthenticationToken.AuthTokenType.WEBHOOK)

    }

    def "importWebhookToken existing user token"() {
        given:
            service.apiService = Mock(ApiService)
            service.tokenDataProvider = new GormTokenDataProvider()
            User user1 = new User(login: 'auser')
            user1.save()
            AuthToken existing = new AuthToken(
                    token: '123',
                    authRoles: 'a,b',
                    user: user1,
                    type: AuthenticationToken.AuthTokenType.USER
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
            0 * service.apiService.createUserToken(auth, 0, token, user, roles, false, AuthenticationToken.AuthTokenType.WEBHOOK)

    }

    def "delete token"() {
        given:
        mockDataService(AuthTokenDataService)
        def provider  = new GormTokenDataProvider()
        provider.authTokenDataService = applicationContext.getBean(AuthTokenDataService)

        service.tokenDataProvider = provider
        User user1 = new User(login: 'auser')
        user1.save()
        AuthToken createdToken = new AuthToken(
                user: user1,
                type: tokenType,
                token: 'abc',
                authRoles: 'g,f',
                uuid: UUID.randomUUID().toString(),
                creator: 'elf',
        )
        createdToken.save(flush: true);

        when:
        boolean isDeleted = service.deleteByTokenWithType(tokenValue, tokenType)
        then:
        isDeleted == true

        where:
        tokenValue              | expectedValue     | tokenType
        'abc'                   | true              | AuthenticationToken.AuthTokenType.WEBHOOK
        'abc'                   | true              | AuthenticationToken.AuthTokenType.USER
        'abc'                   | true              | AuthenticationToken.AuthTokenType.RUNNER
    }

    def cleanup() {
    }

}
