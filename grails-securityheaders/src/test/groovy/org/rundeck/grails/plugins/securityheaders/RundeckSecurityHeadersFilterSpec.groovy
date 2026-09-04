/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.grails.plugins.securityheaders

import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification
import spock.lang.Unroll

import jakarta.servlet.FilterChain

class RundeckSecurityHeadersFilterSpec extends Specification {

    @Unroll
    def "test filter disabled"() {
        given:
            def filter = new RundeckSecurityHeadersFilter()
            filter.enabled = false
            filter.applicationContext = Mock(ApplicationContext)
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def chain = Mock(FilterChain)

        when:
            filter.doFilter(request, response, chain)
        then:
            0 * filter.applicationContext.getBeansOfType(SecurityHeaderProvider)
            1 * chain.doFilter(request, response)
    }


    @Unroll
    def "test filter with provider disabled, default enabled #defEnabled config #confEnabled"() {
        given:
            def provider1 = Mock(SecurityHeaderProvider) {
                getDefaultEnabled() >> defEnabled
                getName() >> 'testprovider1'
            }
            def filter = new RundeckSecurityHeadersFilter()
            filter.config = [testprovider1: [enabled: confEnabled]]
            filter.enabled = true
            filter.applicationContext = Mock(ApplicationContext)
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def chain = Mock(FilterChain)

        when:
            filter.doFilter(request, response, chain)
        then:
            1 * filter.applicationContext.getBeansOfType(SecurityHeaderProvider) >> [
                    testbean1: provider1
            ]
            1 * chain.doFilter(request, response)
            response.getHeader('x-test1') == null


        where:
            defEnabled | confEnabled
            false      | null
            true       | false
            true       | 'false'
    }

    @Unroll
    def "test filter with provider default enabled #defEnabled config #confEnabled"() {
        given:
            def provider1 = Mock(SecurityHeaderProvider) {
                getDefaultEnabled() >> defEnabled
                getName() >> 'testprovider1'
            }
            def filter = new RundeckSecurityHeadersFilter()
            filter.config = [testprovider1: [enabled: confEnabled]]
            filter.enabled = true
            filter.applicationContext = Mock(ApplicationContext)
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def chain = Mock(FilterChain)

        when:
            filter.doFilter(request, response, chain)
        then:
            1 * filter.applicationContext.getBeansOfType(SecurityHeaderProvider) >> [
                    testbean1: provider1
            ]
            1 * chain.doFilter(request, response)
            1 * provider1.getSecurityHeaders(request, response, [:]) >>
            new ArrayList<SecurityHeader>([new SecurityHeaderImpl(name: 'x-test1', value: 'x-value1')])
            response.getHeader('x-test1') == 'x-value1'

        where:
            defEnabled | confEnabled
            true       | null
            false      | 'true'
    }

    def "csp nonce request attribute is set when enabled"() {
        given:
            def filter = new RundeckSecurityHeadersFilter()
            filter.config = [:]
            filter.enabled = true
            filter.applicationContext = Mock(ApplicationContext) {
                getBeansOfType(SecurityHeaderProvider) >> [:]
            }
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def chain = Mock(FilterChain)

        when:
            filter.doFilter(request, response, chain)
        then:
            1 * chain.doFilter(request, response)
            CspNonceProvider.getNonce(request) != null
    }

    def "csp nonce request attribute is not set when disabled"() {
        given:
            def filter = new RundeckSecurityHeadersFilter()
            filter.enabled = false
            filter.applicationContext = Mock(ApplicationContext)
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def chain = Mock(FilterChain)

        when:
            filter.doFilter(request, response, chain)
        then:
            1 * chain.doFilter(request, response)
            CspNonceProvider.getNonce(request) == null
    }

    def "csp nonce request attribute differs between sessions"() {
        given:
            def filter = new RundeckSecurityHeadersFilter()
            filter.config = [:]
            filter.enabled = true
            filter.applicationContext = Mock(ApplicationContext) {
                getBeansOfType(SecurityHeaderProvider) >> [:]
            }
            def request1 = new MockHttpServletRequest('GET', "/test/uri")
            def request2 = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def chain = Mock(FilterChain)

        when:
            filter.doFilter(request1, response, chain)
            filter.doFilter(request2, response, chain)
        then:
            CspNonceProvider.getNonce(request1) != null
            CspNonceProvider.getNonce(request2) != null
            CspNonceProvider.getNonce(request1) != CspNonceProvider.getNonce(request2)
    }

    def "csp nonce request attribute is stable across requests in the same session"() {
        given:
            def filter = new RundeckSecurityHeadersFilter()
            filter.config = [:]
            filter.enabled = true
            filter.applicationContext = Mock(ApplicationContext) {
                getBeansOfType(SecurityHeaderProvider) >> [:]
            }
            def session = new org.springframework.mock.web.MockHttpSession()
            def request1 = new MockHttpServletRequest('GET', "/test/uri")
            request1.session = session
            def request2 = new MockHttpServletRequest('GET', "/test/uri")
            request2.session = session
            def response = new MockHttpServletResponse()
            def chain = Mock(FilterChain)

        when:
            filter.doFilter(request1, response, chain)
            filter.doFilter(request2, response, chain)
        then:
            CspNonceProvider.getNonce(request1) != null
            CspNonceProvider.getNonce(request1) == CspNonceProvider.getNonce(request2)
    }
}
