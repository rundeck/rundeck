package rundeckapp.init

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Specification

class RecycleSafeGrailsWebRequestTaskDecoratorSpec extends Specification {

    /** Stands in for Jetty 12's ServletApiRequest once the container has recycled it. */
    static class RecyclableRequest extends HttpServletRequestWrapper {
        boolean recycled = false

        RecyclableRequest(HttpServletRequest delegate) { super(delegate) }

        @Override
        Object getAttribute(String name) {
            if (recycled) {
                throw new NullPointerException(
                        'Cannot invoke "org.eclipse.jetty.server.Request.getAttribute(String)" because ' +
                        'the return value of "...ServletApiRequest.getRequest()" is null')
            }
            super.getAttribute(name)
        }
    }

    private RecyclableRequest raw

    def setup() {
        raw = new RecyclableRequest(new MockHttpServletRequest())
    }

    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    private void bindWebRequest() {
        WebUtils.storeGrailsWebRequest(
                new GrailsWebRequest(raw, new MockHttpServletResponse(), new MockServletContext()))
    }

    def "the task still runs when the originating request has been recycled"() {
        given: "a task submitted while a web request was bound"
        bindWebRequest()
        boolean ran = false
        Runnable decorated = new RecycleSafeGrailsWebRequestTaskDecorator().decorate({ ran = true } as Runnable)

        when: "the container recycles the request before the worker picks the task up"
        raw.recycled = true
        decorated.run()

        then: "the exception does not escape and kill the pool thread"
        noExceptionThrown()

        and: "the work is not silently dropped"
        ran
    }

    def "the web request is propagated when it is still usable"() {
        given:
        bindWebRequest()
        GrailsWebRequest seen = null
        Runnable decorated = new RecycleSafeGrailsWebRequestTaskDecorator().
                decorate({ seen = GrailsWebRequest.lookup() } as Runnable)

        when:
        decorated.run()

        then:
        seen != null
        seen.currentRequest.is(raw)
    }

    def "the task is returned undecorated when no web request is bound"() {
        given:
        RequestContextHolder.resetRequestAttributes()
        Runnable task = {} as Runnable

        expect:
        new RecycleSafeGrailsWebRequestTaskDecorator().decorate(task).is(task)
    }
}
