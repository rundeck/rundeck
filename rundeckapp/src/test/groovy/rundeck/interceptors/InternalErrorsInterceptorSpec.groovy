package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import org.apache.http.HttpStatus
import org.grails.taglib.GrailsTagException
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class InternalErrorsInterceptorSpec extends Specification implements InterceptorUnitTest<InternalErrorsInterceptor> {
    private static final String SHOW_TRACES_DEBUG_PROP_NAME = 'rundeck.feature.debug.showTracesOnResponse'
    private static final String[] EXCEPT_ATTR_NAMES = ['javax.servlet.error.exception', 'exception']

    def "should remove exceptions stacktrace from request by default"() {
        setup:
        request.setAttribute(EXCEPT_ATTR_NAMES[0], new Exception("This is a \"${EXCEPT_ATTR_NAMES[0]}\""))
        request.setAttribute(EXCEPT_ATTR_NAMES[1], new Exception("This is a \"${EXCEPT_ATTR_NAMES[1]}\""))
        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)

        when:
        interceptor.after()

        then:
        def e = thrown(Exception)
        e.message.contains('No tag library found for namespace')
        (request.getAttribute(EXCEPT_ATTR_NAMES[0]) as Throwable).stackTrace.size() == 0
        (request.getAttribute(EXCEPT_ATTR_NAMES[1]) as Throwable).stackTrace.size() == 0
    }

    def "should NOT remove exceptions stacktrace from request on debug property set"() {
        when:
        request.setAttribute(EXCEPT_ATTR_NAMES[0], new Exception("This is a \"${EXCEPT_ATTR_NAMES[0]}\""))
        request.setAttribute(EXCEPT_ATTR_NAMES[1], new Exception("This is a \"${EXCEPT_ATTR_NAMES[1]}\""))
        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        grailsApplication.config.put(SHOW_TRACES_DEBUG_PROP_NAME, 'true')
        withRequest(controller:"demo", action: 'index')

        then:
        !interceptor.doesMatch()
    }

}
