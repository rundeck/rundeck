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


import org.rundeck.grails.plugins.securityheaders.CSPSecurityHeaderProvider
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification
import spock.lang.Unroll

class CSPSecurityHeaderProviderSpec extends Specification {

    def " missing config"() {
        given:
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def secHeaderProvider = new CSPSecurityHeaderProvider()


        when: "A request without specific uri setting"
            def list = secHeaderProvider.getSecurityHeaders(request, response, config)
        then: "The interceptor does match"
            IllegalStateException e = thrown()
            e.message.contains 'policy or directive configuration is required'
        where:
            config                          | _
            [:]                             | _
            ['wrong-directive': 'blahblah'] | _

    }

    def "explicit policy"() {
        given:
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def secHeaderProvider = new CSPSecurityHeaderProvider()

            def config = ['policy': policy]
        when: "A request without specific uri setting"
            def list = secHeaderProvider.getSecurityHeaders(request, response, config)
        then: "The interceptor does match"
            list != null
            list.size() == 3
            list[0].name == 'Content-Security-Policy'
            list[0].value == policy
            list[1].name == 'X-Content-Security-Policy'
            list[1].value == policy
            list[2].name == 'X-WebKit-CSP'
            list[2].value == policy

        where:
            policy << ["default-src 'none' ;",
                       "default-src 'none' ; img-src 'self' ; ",
                       "default-src 'none' ; frame-ancestors *.somesite.com https://myfriend.site.com"]
    }


    def "disable x-headers"() {
        given:
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def secHeaderProvider = new CSPSecurityHeaderProvider()

        when: "A request without specific uri setting"
            def list = secHeaderProvider.getSecurityHeaders(
                    request,
                    response,
                    config + [policy: 'default-src \'none\' ;']
            )
        then: "The interceptor does match"
            list != null
            list.size() == expectCount
            def names = list*.name
            names.contains 'Content-Security-Policy'
            names.contains('X-Content-Security-Policy') == includeXcsp
            names.contains('X-WebKit-CSP') == includeXwkcsp


        where:
            config                             | expectCount | includeXcsp | includeXwkcsp
            [:]                                | 3           | true        | true
            ['include-xcsp-header': 'false']   | 2           | false       | true
            ['include-xwkcsp-header': 'false'] | 2           | true        | false
            ['include-xwkcsp-header': 'false',
             'include-xcsp-header'  : 'false'] | 1           | false       | false
            ['include-xwkcsp-header': 'true',
             'include-xcsp-header'  : 'true']  | 3           | true        | true
    }

    @Unroll
    def "csp response with directive #directive should quote keywords"() {
        given:
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def secHeaderProvider = new CSPSecurityHeaderProvider()

            def config = ['frame-ancestors': confVal]
        when: "A request without specific uri setting"
            def list = secHeaderProvider.getSecurityHeaders(request, response, config)
        then: "The interceptor does match"
            list != null
            list.size() == 3
            list[0].name == 'Content-Security-Policy'
            list[0].value == result
            list[1].name == 'X-Content-Security-Policy'
            list[1].value == result
            list[2].name == 'X-WebKit-CSP'
            list[2].value == result

        where:
            directive         | confVal                     | result
            'frame-ancestors' | 'none'                      | "frame-ancestors 'none' ;"
            'frame-ancestors' | "'none'"                    | "frame-ancestors 'none' ;"
            'frame-ancestors' | 'self'                      | "frame-ancestors 'self' ;"
            'frame-ancestors' | "'self'"                    | "frame-ancestors 'self' ;"
            'frame-ancestors' | '*.somesite.com'            | "frame-ancestors *.somesite.com ;"
            'frame-ancestors' | 'https://myfriend.site.com' | "frame-ancestors https://myfriend.site.com ;"
    }
}
