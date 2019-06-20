package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class UserLogoutInterceptorSpec extends Specification implements InterceptorUnitTest<UserLogoutInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test userLogout interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller: 'user', action: 'logout')

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
