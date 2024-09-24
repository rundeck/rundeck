package org.rundeck.grails.plugins.securityheaders

import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification
import spock.lang.Unroll

import static org.rundeck.grails.plugins.securityheaders.CacheControlSecurityHeaderProvider.*

class CacheControlSecurityHeaderProviderSpec extends Specification {

    @Unroll
    def "config directives won't cause errors"() {
        given:
        def request = new MockHttpServletRequest('GET', "/test/uri")
        def response = new MockHttpServletResponse()
        def secHeaderProvider = new CacheControlSecurityHeaderProvider()

        when:
        def list = secHeaderProvider.getSecurityHeaders(request, response, config)

        then:
        expectedSize == list.size()
        list.find { it.name == HttpHeaders.CACHE_CONTROL }?.value == expectedValue
        list.find { it.name == HttpHeaders.PRAGMA }?.value == expectedPragma
        list.find { it.name == HttpHeaders.EXPIRES }?.value == expectedExpires

        where:
        config                          | expectedSize | expectedValue               | expectedExpires       | expectedPragma
        [:]                             | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        ['wrong-directive': 'blahblah'] | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        null                            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
    }

    @Unroll
    def "Dont rewrite existing headers"() {
        given:
        def secHeaderProvider = new CacheControlSecurityHeaderProvider()
        def request = new MockHttpServletRequest('GET', "/test/uri")
        def response = new MockHttpServletResponse()

        when:
        for (header in existingHeadersMap) {
            response.addHeader(header.key, header.value)
        }

        def list = secHeaderProvider.getSecurityHeaders(request, response, [:])

        then:
        expectedSize == list.size()
        list.find { it.name == HttpHeaders.CACHE_CONTROL }?.value == expectedValue
        list.find { it.name == HttpHeaders.PRAGMA }?.value == expectedPragma
        list.find { it.name == HttpHeaders.EXPIRES }?.value == expectedExpires

        where:
        existingHeadersMap                      | expectedSize | expectedValue               | expectedExpires       | expectedPragma
        [:]                                     | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        ['Cache-Control': 'no-cache, no-store'] | 0            | null                        | null                  | null
        ['Pragma': 'no-cache']                  | 0            | null                        | null                  | null
        ['Expires': '0']                        | 0            | null                        | null                  | null
        ['Cache-Control': 'no-cache, no-store',
         'Pragma'       : 'no-cache',
         'Expires'      : '3600']               | 0            | null                        | null                  | null
    }

    @Unroll
    def "Dont rewrite depending on response status"() {
        given:
        def secHeaderProvider = new CacheControlSecurityHeaderProvider()
        def request = new MockHttpServletRequest('GET', "/test/uri")
        def response = new MockHttpServletResponse()

        when:
        response.setStatus(responseStatus)
        def list = secHeaderProvider.getSecurityHeaders(request, response, [:])

        then:
        expectedSize == list.size()
        list.find { it.name == HttpHeaders.CACHE_CONTROL }?.value == expectedValue
        list.find { it.name == HttpHeaders.PRAGMA }?.value == expectedPragma
        list.find { it.name == HttpHeaders.EXPIRES }?.value == expectedExpires

        where:
        responseStatus | expectedSize | expectedValue               | expectedExpires       | expectedPragma
        200            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        201            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        204            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        301            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        302            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        304            | 0            | null                        | null                  | null
        400            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        401            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        403            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        404            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        409            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        410            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        500            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
        503            | 3            | DEFAULT_CACHE_CONTROL_VALUE | DEFAULT_EXPIRES_VALUE | DEFAULT_PRAGMA_VALUE
    }

}
