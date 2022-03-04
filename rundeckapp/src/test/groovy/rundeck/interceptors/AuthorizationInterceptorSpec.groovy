package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import org.rundeck.app.access.InterceptorHelper
import spock.lang.Specification

class AuthorizationInterceptorSpec extends Specification implements InterceptorUnitTest<AuthorizationInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test authorization interceptor matching"() {
        when: "A request matches the interceptor"
            withRequest(controller: "authorization")

        then: "The interceptor does match"
            interceptor.doesMatch()
    }

    def "apiVersionStatusNotReady causes 503 response"() {
        given:
            request.apiVersionStatusNotReady = true
            request.invalidApiAuthentication = invalidApiAuthentication
            interceptor.interceptorHelper = Mock(InterceptorHelper)
        when:
            def result = interceptor.before()
        then:
            !result
            response.status == 503
        where:
            invalidApiAuthentication << [true, false]
    }
}
