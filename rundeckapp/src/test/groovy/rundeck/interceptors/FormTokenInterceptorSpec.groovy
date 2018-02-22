package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class FormTokenInterceptorSpec extends Specification implements InterceptorUnitTest<FormTokenInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test formToken interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"formToken")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
