package rundeck.interceptors

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import grails.core.GrailsApplication
import grails.testing.web.interceptor.InterceptorUnitTest
import rundeck.UtilityTagLib
import rundeck.codecs.HTMLAttributeCodec
import rundeck.codecs.HTMLContentCodec
import rundeck.codecs.URIComponentCodec
import rundeck.services.ConfigurationService
import rundeck.services.UserService
import spock.lang.Specification

import javax.security.auth.Subject
import java.security.Principal

class SetUserInterceptorSpec extends Specification implements InterceptorUnitTest<SetUserInterceptor> {

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
}
