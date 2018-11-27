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

package org.rundeck.security

import org.grails.spring.beans.factory.InstanceFactoryBean
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import rundeck.services.ConfigurationService
import spock.lang.Specification

class CSPSecurityHeaderProviderSpec extends Specification {

    def "test csp response default"() {
        given:
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def secHeaderProvider = new CSPSecurityHeaderProvider()

            def config = ['frame-ancestors': defCsp]
        when: "A request without specific uri setting"
            def list = secHeaderProvider.getSecurityHeaders(request, response, config)
        then: "The interceptor does match"
            list != null
            list.size() == 1
            list[0].name == 'Content-Security-Policy'
            list[0].value == result

        where:
            defCsp                                     | result
            null                                       | "frame-ancestors 'none'"
            'none'                                     | "frame-ancestors 'none'"
            'self'                                     | "frame-ancestors 'self'"
            '*.somesite.com https://myfriend.site.com' | "frame-ancestors '*.somesite.com' 'https://myfriend.site.com'"
    }

    def "test csp response uri config"() {
        given:
            def request = new MockHttpServletRequest('GET', reqUri)
            def response = new MockHttpServletResponse()
            def secHeaderProvider = new CSPSecurityHeaderProvider()

            def config = ['frame-ancestors': defCsp, 'uri.test2.uri': confCsp]
        when: "A request without specific uri setting"
            def list = secHeaderProvider.getSecurityHeaders(request, response, config)
        then: "The interceptor does match"
            list != null
            list.size() == 1
            list[0].name == 'Content-Security-Policy'
            list[0].value == result

        where:
            reqUri       | defCsp | confCsp | result
            '/test/uri'  | null   | 'self'  | "frame-ancestors 'none'"
            '/test/uri'  | 'none' | 'self'  | "frame-ancestors 'none'"
            '/test/uri'  | 'self' | 'none'  | "frame-ancestors 'self'"
            '/test2/uri' | 'none' | 'self'  | "frame-ancestors 'self'"
            '/test2/uri' | 'self' | 'none'  | "frame-ancestors 'none'"
            '/test2/uri' | null   | 'self'  | "frame-ancestors 'self'"
    }

}
