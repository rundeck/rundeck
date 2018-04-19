package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class PostApiTokenInterceptorSpec extends Specification implements InterceptorUnitTest<PostApiTokenInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test postApiToken interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"postApiToken")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
