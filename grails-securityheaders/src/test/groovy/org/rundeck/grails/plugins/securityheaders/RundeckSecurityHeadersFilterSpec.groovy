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

import javax.servlet.FilterChain

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
}
