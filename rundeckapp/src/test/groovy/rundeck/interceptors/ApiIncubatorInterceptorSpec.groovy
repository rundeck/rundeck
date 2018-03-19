package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class ApiIncubatorInterceptorSpec extends Specification implements InterceptorUnitTest<ApiIncubatorInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test apiIncubator interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(uri:"/api/apiVersion")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
