package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class ApiAccessInterceptorSpec extends Specification implements InterceptorUnitTest<ApiAccessInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test apiRequest interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"apiRequest")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
