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
            list.size() == 1
            list[0].name == 'Content-Security-Policy'
            list[0].value == policy


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
            config                            | expectCount | includeXcsp | includeXwkcsp
            [:]                               | 1           | false       | false
            ['include-xcsp-header': 'true']   | 2           | true        | false
            ['include-xwkcsp-header': 'true'] | 2           | false       | true
            ['include-xwkcsp-header': 'true',
             'include-xcsp-header'  : 'true'] | 3           | true        | true
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
            list.size() == 1
            list[0].name == 'Content-Security-Policy'
            list[0].value == result

        where:
            directive         | confVal                      | result
            'frame-ancestors' | 'none'                       | "frame-ancestors 'none' ;"
            'frame-ancestors' | "'none'"                     | "frame-ancestors 'none' ;"
            'frame-ancestors' | 'self'                       | "frame-ancestors 'self' ;"
            'frame-ancestors' | "'self'"                     | "frame-ancestors 'self' ;"
            'frame-ancestors' | "unsafe-inline"              | "frame-ancestors 'unsafe-inline' ;"
            'frame-ancestors' | "'unsafe-inline'"            | "frame-ancestors 'unsafe-inline' ;"
            'frame-ancestors' | "unsafe-eval"                | "frame-ancestors 'unsafe-eval' ;"
            'frame-ancestors' | "'unsafe-eval'"              | "frame-ancestors 'unsafe-eval' ;"
            'frame-ancestors' | "nonce-asdfasdf"             | "frame-ancestors 'nonce-asdfasdf' ;"
            'frame-ancestors' | "'nonce-asdfasdf'"           | "frame-ancestors 'nonce-asdfasdf' ;"
            'frame-ancestors' | "sha256-asdfasdf"            | "frame-ancestors 'sha256-asdfasdf' ;"
            'frame-ancestors' | "'sha256-asdfasdf'"          | "frame-ancestors 'sha256-asdfasdf' ;"
            'frame-ancestors' | '*'                          | "frame-ancestors * ;"
            'frame-ancestors' | '*.somesite.com'             | "frame-ancestors *.somesite.com ;"
            'frame-ancestors' | 'https://myfriend.site.com'  | "frame-ancestors https://myfriend.site.com ;"
            'frame-ancestors' | 'https:'                     | "frame-ancestors https: ;"
            'frame-ancestors' | 'data:'                      | "frame-ancestors data: ;"
            'frame-ancestors' | 'self data: unsafe-inline *' | "frame-ancestors 'self' data: 'unsafe-inline' * ;"
    }

    @Unroll
    def "nonce marker #confVal is replaced with the per-request nonce"() {
        given:
            def request = new MockHttpServletRequest('GET', "/test/uri")
            request.setAttribute(CspNonceProvider.HTTP_ATTRIBUTE_NAME, 'abc123nonce')
            def response = new MockHttpServletResponse()
            def secHeaderProvider = new CSPSecurityHeaderProvider()

            def config = ['script-src': confVal]
        when:
            def list = secHeaderProvider.getSecurityHeaders(request, response, config)
        then:
            list.size() == 1
            list[0].name == 'Content-Security-Policy'
            list[0].value == result

        where:
            confVal                    | result
            'nonce'                    | "script-src 'nonce-abc123nonce' ;"
            "'nonce'"                  | "script-src 'nonce-abc123nonce' ;"
            'self nonce'               | "script-src 'self' 'nonce-abc123nonce' ;"
            'self nonce unsafe-eval'   | "script-src 'self' 'nonce-abc123nonce' 'unsafe-eval' ;"
    }

    def "nonce marker is dropped when no nonce is available for the request"() {
        given: "a request with no nonce attribute set"
            def request = new MockHttpServletRequest('GET', "/test/uri")
            def response = new MockHttpServletResponse()
            def secHeaderProvider = new CSPSecurityHeaderProvider()

            def config = ['script-src': 'self nonce']
        when:
            def list = secHeaderProvider.getSecurityHeaders(request, response, config)
        then: "no literal 'nonce' keyword is emitted"
            list[0].value == "script-src 'self' ;"
    }

    def "nonce differs between requests and header is not cached"() {
        given:
            def response = new MockHttpServletResponse()
            def secHeaderProvider = new CSPSecurityHeaderProvider()
            def config = ['script-src': 'self nonce']

            def request1 = new MockHttpServletRequest('GET', "/test/uri")
            def request2 = new MockHttpServletRequest('GET', "/test/uri")
            def nonce1 = CspNonceProvider.storeNonce(request1)
            def nonce2 = CspNonceProvider.storeNonce(request2)
        when:
            def list1 = secHeaderProvider.getSecurityHeaders(request1, response, config)
            def list2 = secHeaderProvider.getSecurityHeaders(request2, response, config)
        then:
            nonce1 != nonce2
            list1[0].value == "script-src 'self' 'nonce-${nonce1}' ;"
            list2[0].value == "script-src 'self' 'nonce-${nonce2}' ;"
            list1[0].value != list2[0].value
            secHeaderProvider.builtHeaders == null
    }

    def "legacy behavior without nonce marker is unchanged and cached"() {
        given:
            def response = new MockHttpServletResponse()
            def secHeaderProvider = new CSPSecurityHeaderProvider()
            def config = ['script-src': 'self unsafe-inline unsafe-eval']

            def request1 = new MockHttpServletRequest('GET', "/test/uri")
            def request2 = new MockHttpServletRequest('GET', "/test/uri")
            CspNonceProvider.storeNonce(request1)
            CspNonceProvider.storeNonce(request2)
        when:
            def list1 = secHeaderProvider.getSecurityHeaders(request1, response, config)
            def list2 = secHeaderProvider.getSecurityHeaders(request2, response, config)
        then: "identical header content, computed once and cached"
            list1[0].value == "script-src 'self' 'unsafe-inline' 'unsafe-eval' ;"
            list2[0].value == list1[0].value
            secHeaderProvider.builtHeaders != null
            list1.is(list2)
    }

    @Unroll
    def "isNonceRequested #config == #expected"() {
        expect:
            CSPSecurityHeaderProvider.isNonceRequested(config) == expected

        where:
            config                                             | expected
            [:]                                                | false
            ['script-src': 'self unsafe-inline']                | false
            ['script-src': 'self nonce-fixedvalue']             | false
            ['script-src': 'self nonce']                        | true
            ['style-src': "'nonce'"]                            | true
            ['policy': 'script-src nonce ;']                    | false
    }
}
