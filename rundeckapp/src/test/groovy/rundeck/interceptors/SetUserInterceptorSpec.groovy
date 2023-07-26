package rundeck.interceptors

import com.dtolabs.rundeck.app.config.RundeckConfig
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import org.rundeck.app.data.model.v1.AuthTokenMode
import grails.testing.gorm.DataTest
import grails.testing.web.interceptor.InterceptorUnitTest
import org.rundeck.app.access.InterceptorHelper
import org.rundeck.app.data.model.v1.AuthenticationToken
import org.rundeck.app.data.providers.GormTokenDataProvider
import rundeck.AuthToken
import rundeck.ConfigTagLib
import rundeck.User
import rundeck.UtilityTagLib
import rundeck.codecs.HTMLAttributeCodec
import rundeck.codecs.HTMLContentCodec
import rundeck.codecs.URIComponentCodec
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import rundeck.services.UserService
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.ServletContext

class SetUserInterceptorSpec extends Specification implements InterceptorUnitTest<SetUserInterceptor>, DataTest {

    void setupSpec() {
        mockDomain User
        mockDomain AuthToken
    }
    def setup() {
    }

    def cleanup() {

    }

    void "Test setUser interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"setUser")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }

    void "CreateAuth subject with roles supplied from user group plugin"() {
        given:
        def userServiceMock = Mock(UserService) {
            getUserGroupSourcePluginRoles(username) >> { roles }
        }

        defineBeans {
            userService(userServiceMock)
        }
        interceptor.interceptorHelper = Mock(InterceptorHelper) {
            matchesAllowedAsset(_,_) >> false
        }
        interceptor.configurationService = Mock(ConfigurationService) {
            getString(_,_) >> ""
            getString(_) >> ""
        }
        when:
        interceptor.userService = userServiceMock
        request.remoteUser = username
        request.userPrincipal = new Username(username)
        boolean allowed = interceptor.before()

        then:
        allowed
        request.subject.principals.find { it instanceof Username }?.name == username
        request.subject.principals.findAll { it instanceof Group }.collect { it.name } == roles

        where:
        username   | roles
        "testUser" | []
        "testUser" | ["one","two","three"]
    }

    def "Require role for login if set in properties"() {
        setup:
        defineBeans {
            rundeckConfig(RundeckConfig)
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }

        }
        GroovyMock(ConfigurationService, global: true)

        mockCodec(URIComponentCodec)
        mockCodec(HTMLContentCodec)
        mockCodec(HTMLAttributeCodec)
        mockTagLib(UtilityTagLib)
        mockTagLib(ConfigTagLib)

        def userServiceMock = Mock(UserService) {
            getUserGroupSourcePluginRoles(username) >> { groups }
        }
        interceptor.interceptorHelper = Mock(InterceptorHelper) {
            matchesAllowedAsset(_,_) >> false
        }

        interceptor.configurationService = Mock(ConfigurationService) {
            getString("security.requiredRole","")>>requiredRole
            getString(_,_)>>""
            getString(_)>>""
        }

        when:
        interceptor.userService = userServiceMock

        interceptor.request.userPrincipal = new Username(username)
        interceptor.request.remoteUser = username
        boolean allowed = interceptor.before()

        then:
        allowed == expected
        flash.loginErrorCode==code

        where:
        requiredRole | username | groups            | expected | code
        "enter"      | "auser"  | ["grp1"]          | false    | 'user.not.allowed'
        "enter"      | "auser"  | ["grp1", "enter"] | true     | null
        ""           | "auser"  | ["grp1"]          | true     | null


    }

    def "Require a list of roles for login if set in properties"() {
        setup:
        defineBeans {
            rundeckConfig(RundeckConfig)
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }

        }
        GroovyMock(ConfigurationService, global: true)

        mockCodec(URIComponentCodec)
        mockCodec(HTMLContentCodec)
        mockCodec(HTMLAttributeCodec)
        mockTagLib(UtilityTagLib)
        mockTagLib(ConfigTagLib)

        def userServiceMock = Mock(UserService) {
            getUserGroupSourcePluginRoles("User") >> { groups }
        }
        interceptor.interceptorHelper = Mock(InterceptorHelper) {
            matchesAllowedAsset(_,_) >> false
        }

        interceptor.configurationService = Mock(ConfigurationService) {
             getString("security.requiredRole","")>>soleRequiredRole
             getString("security.requiredRoles","")>>commaSeparatedUserRoles
        }

        when:
        interceptor.userService = userServiceMock

        interceptor.request.userPrincipal = new Username("User")
        interceptor.request.remoteUser = "User"
        boolean allowed = interceptor.before()

        then:
        allowed == userAllowed
        flash.loginErrorCode==code

        where:
        soleRequiredRole | commaSeparatedUserRoles | groups           | userAllowed | code
        ''               | ""                      |["admin", "user"] | true        | null
        'admin'          | ""                      |["admin", "user"] | true        | null
        'user'           | ""                      |["admin"]         | false       | 'user.not.allowed'
        ''               | "admin,user"            |["admin"]         | true        | null
        ''               | "admin,user"            |["admin", "user"] | true        | null
        'allowed'        | "admin,user"            |["allowed"]       | true        | null
        'allowed'        | "admin,user"            |["anyOfThem"]     | false       | 'user.not.allowed'

    }

    @Unroll
    def "lookupToken"() {

        setup:
        User u1 = new User(login: "admin")
        User u2 = new User(login: "whk")
        AuthToken userTk1 = new AuthToken(token: "123",user:u1,authRoles:"admin",type: null)
        AuthToken userTk2 = new AuthToken(token: "456", user:u1, authRoles:"admin", type: AuthenticationToken.AuthTokenType.USER, tokenMode: AuthTokenMode.LEGACY)
        AuthToken userTk3 = new AuthToken(token: "ABC", user:u1, authRoles:"admin", type: AuthenticationToken.AuthTokenType.USER, tokenMode: AuthTokenMode.SECURED)
        AuthToken userTk4 = new AuthToken(token: "DEF", user:u1, authRoles:"admin", type: AuthenticationToken.AuthTokenType.USER, tokenMode: null)
        AuthToken runnerTk1 = new AuthToken(token: "RN1", user:u1, authRoles:"admin", type: AuthenticationToken.AuthTokenType.RUNNER, tokenMode: AuthTokenMode.SECURED)
        AuthToken whkTk = new AuthToken(token: "789", user:u2, authRoles:"admin", type:AuthenticationToken.AuthTokenType.WEBHOOK, tokenMode: AuthTokenMode.LEGACY)
        u1.save()
        u2.save()
        userTk1.save()
        userTk2.save()
        userTk3.save()
        userTk4.save()
        runnerTk1.save()
        whkTk.save()
        def svCtx = Mock(ServletContext)
        request.setAttribute(SetUserInterceptor.RUNNER_RQ_ATTRIB, runnerRq)
        def apiService = new ApiService()
        apiService.tokenDataProvider = new GormTokenDataProvider()

        when:
        interceptor.apiService = apiService
        AuthenticationToken foundToken = interceptor.lookupToken(tk,svCtx,webhookToken)
        String result = foundToken?.getOwnerName()

        then:
        result == expected

        where:
        tk    | webhookToken | runnerRq | expected
        "123" | true         | false    | null
        "123" | false        | false    | "admin"
        "456" | true         | false    | null
        "456" | false        | false    | "admin"
        "ABC" | true         | false    | null
        "ABC" | false        | false    | "admin"
        "DEF" | true         | false    | null
        "DEF" | false        | false    | "admin"
        "789" | true         | false    | "whk"
        "789" | false        | false    | null
        "RN1" | false        | false    | null
        "RN1" | false        | null     | null
        "RN1" | false        | true     | "admin"
    }

    @Unroll
    def "lookupTokenRoles"() {

        given:
        User u1 = new User(login: "admin")

        AuthToken userTk3 = new AuthToken(token: tk, user:u1, authRoles:authRoles, type: AuthenticationToken.AuthTokenType.USER, tokenMode: AuthTokenMode.SECURED)
        u1.save()
        userTk3.save()
        def svCtx = Mock(ServletContext)
        def apiService = new ApiService()
        apiService.tokenDataProvider = new GormTokenDataProvider()

        when:
        interceptor.apiService = apiService
        Set<String> foundRoles = interceptor.lookupTokenRoles(userTk3,svCtx)

        then:
        foundRoles == expected

        where:
        tk    | authRoles       | expected
        "123" | "admin"         | ['admin'].toSet()
        "456" |  null           |  null
        "ABC" | "webhook,role1" | ['webhook','role1'].toSet()
    }

    def "request without remote auth info will be invalid"(){
        given:
            request.api_version=12
            interceptor.interceptorHelper=Mock(InterceptorHelper)
        when:
            def result=interceptor.before()
        then:
            result
            request.invalidApiAuthentication
    }

}
