package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class AuthorizationInterceptorSpec extends Specification implements InterceptorUnitTest<AuthorizationInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test authorization interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"authorization")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
