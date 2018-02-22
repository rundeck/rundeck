package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class ApiVersionInterceptorSpec extends Specification implements InterceptorUnitTest<ApiVersionInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test apiVersion interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"apiVersion")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
