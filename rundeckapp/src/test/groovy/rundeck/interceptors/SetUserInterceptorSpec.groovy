package rundeck.interceptors

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import grails.testing.web.interceptor.InterceptorUnitTest
import rundeck.services.UserService
import spock.lang.Specification

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
}
