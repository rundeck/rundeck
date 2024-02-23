package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import rundeck.services.ConfigurationService
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ZZ_TimerInterceptorSpec extends Specification implements InterceptorUnitTest<ZZ_TimerInterceptor> {
    def "After"() {
        given:
        boolean afterRequestCalled = false
        AA_TimerInterceptor.metaClass.static.afterRequest = { HttpServletRequest request, HttpServletResponse response, session ->
            afterRequestCalled = true
        }
        interceptor.configurationService = Mock(ConfigurationService) {
            getValue(_,_) >> ["/ignore/me"]
        }
        request[AA_TimerInterceptor._REQ_URI] = requestForwardURI

        when:
        boolean result = interceptor.after()
        then:
        result
        expectedAfterRequestCalled == afterRequestCalled
        where:
        requestForwardURI   | expectedAfterRequestCalled
        "/ignore/me/please" | false
        "/api"              | true

    }
}
