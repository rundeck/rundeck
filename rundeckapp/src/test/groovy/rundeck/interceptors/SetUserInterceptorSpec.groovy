package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

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
}
