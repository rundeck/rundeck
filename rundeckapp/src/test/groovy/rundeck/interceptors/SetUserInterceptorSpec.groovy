package rundeck.interceptors

import com.dtolabs.rundeck.app.config.RundeckConfig
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import grails.core.GrailsApplication
import grails.testing.gorm.DataTest
import grails.testing.web.interceptor.InterceptorUnitTest
import rundeck.AuthToken
import rundeck.User
import rundeck.UtilityTagLib
import rundeck.codecs.HTMLAttributeCodec
import rundeck.codecs.HTMLContentCodec
import rundeck.codecs.URIComponentCodec
import rundeck.services.ConfigurationService
import rundeck.services.UserService
import spock.lang.Specification

import javax.security.auth.Subject
import javax.servlet.ServletContext
import java.security.Principal

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
        mockCodec(URIComponentCodec)
        mockCodec(HTMLContentCodec)
        mockCodec(HTMLAttributeCodec)
        mockTagLib(UtilityTagLib)

        def userServiceMock = Mock(UserService) {
            getUserGroupSourcePluginRoles(username) >> { groups }
        }
        grailsApplication.config.rundeck.security.requiredRole = requiredRole
        messageSource.addMessage("user.not.allowed",Locale.default,"User Not Allowed")

        when:
        interceptor.userService = userServiceMock

        interceptor.request.userPrincipal = new Username(username)
        interceptor.request.remoteUser = username
        boolean allowed = interceptor.before()

        then:
        allowed == expected

        where:
        requiredRole | username | groups           | expected
        "enter"      | "auser"  | ["grp1"]         | false
        "enter"      | "auser"  | ["grp1","enter"] | true
        null         | "auser"  | ["grp1"]         | true


    }

    def "lookupToken"() {

        setup:
        User u1 = new User(login: "admin")
        User u2 = new User(login: "whk")
        AuthToken userTk1 = new AuthToken(token: "123",user:u1,authRoles:"admin",type: null)
        AuthToken userTk2 = new AuthToken(token: "456",user:u1,authRoles:"admin",type: AuthTokenType.USER)
        AuthToken whkTk = new AuthToken(token: "789",user:u2,authRoles:"admin",type:AuthTokenType.WEBHOOK)
        u1.save()
        u2.save()
        userTk1.save()
        userTk2.save()
        whkTk.save()
        def svCtx = Mock(ServletContext)

        when:
        String result = interceptor.lookupToken(tk,svCtx,webhookToken)

        then:
        result == expected

        where:
        tk|webhookToken|expected
        "123"|true|null
        "123"|false|"admin"
        "456"|true|null
        "456"|false|"admin"
        "789"|true|"whk"
        "789"|false|null
    }

}
