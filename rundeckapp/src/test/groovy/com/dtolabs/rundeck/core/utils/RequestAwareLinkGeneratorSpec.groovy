package com.dtolabs.rundeck.core.utils

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import grails.web.mapping.UrlCreator
import grails.web.mapping.UrlMappingsHolder
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Specification

class RequestAwareLinkGeneratorSpec extends Specification {

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
        WebUtils.storeGrailsWebRequest(
                new GrailsWebRequest(raw, new MockHttpServletResponse(), new MockServletContext()))
    }

    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    /**
     * A generator with a context path, which is the branch that consults the server url at all;
     * without one the class returns the relative url untouched.
     */
    private RequestAwareLinkGenerator generatorFor(String serverUrl) {
        UrlCreator creator = Stub(UrlCreator) {
            createRelativeURL(*_) >> '/rundeck/menu/index'
            createURL(*_) >> 'http://ignored/rundeck/menu/index'
        }
        def generator = new RequestAwareLinkGenerator(serverUrl, '/rundeck')
        generator.setUrlMappingsHolder(Stub(UrlMappingsHolder) {
            getReverseMappingNoDefault(*_) >> creator
            getReverseMapping(*_) >> creator
        })
        return generator
    }

    def "a link is still generated when the bound request has been recycled"() {
        given: "a generator configured with a server url, as a background task would use"
            def generator = generatorFor('http://rundeck.example:4440')

        when: "the container recycles the request that is still bound to this thread"
            raw.recycled = true
            String url = generator.link(controller: 'menu', action: 'index')

        then: "the recycled request does not take the caller down with it"
            noExceptionThrown()

        and: "the link falls back to the configured server url"
            url.startsWith('http://rundeck.example:4440')
    }

    def "the recycled request is left bound for whatever runs next"() {
        given:
            def generator = generatorFor('http://rundeck.example:4440')
            def bound = RequestContextHolder.getRequestAttributes()

        when:
            raw.recycled = true
            generator.link(controller: 'menu', action: 'index')

        then: "unbinding it is scoped to the call, not a side effect on the thread"
            RequestContextHolder.getRequestAttributes().is(bound)
    }

    def "a live bound request is still used for the server url"() {
        given:
            def generator = generatorFor('http://configured.example:4440')

        when: "the request is alive, so its own base url wins over the configured one"
            String url = generator.link(controller: 'menu', action: 'index')

        then:
            url.startsWith(GrailsWebRequest.lookup().baseUrl)
    }
}
