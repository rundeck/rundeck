package rundeck.interceptors


import grails.testing.web.interceptor.InterceptorUnitTest
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

class AA_TimerInterceptorSpec extends Specification implements InterceptorUnitTest<AA_TimerInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test AA_TimerInterceptor log project using uri"() {
        given:
        def request = new MockHttpServletRequest('GET', uri)
        when:
            def proj = interceptor.extractProject(request)

        then:
        proj==expected
        where:
        uri           | expected
        "/menu/listExport"   | '?'
        "/project/testProj/index" | 'testProj'
    }
    void "Test AA_TimerInterceptor log project using params"() {
        given:
        def request = new MockHttpServletRequest('POST', uri)
        request.setAttribute("project", expected)
        when:
        def proj = interceptor.extractProject(request)

        then:
        proj==expected
        where:
        uri           | expected
        "/menu/listExport"   | 'testProj'
    }
}
