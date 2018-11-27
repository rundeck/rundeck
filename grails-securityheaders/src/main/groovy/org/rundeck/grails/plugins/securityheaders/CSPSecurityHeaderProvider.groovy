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

import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@CompileStatic
class CSPSecurityHeaderProvider implements SecurityHeaderProvider {

    public static final String CSP_HEADER_NAME = 'Content-Security-Policy'

    String name = 'csp'
    Boolean defaultEnabled = false
    Map<String, String> directives

    @Override
    List<SecurityHeader> getSecurityHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            Map config
    ) {
        List<SecurityHeader> headers = []
        List<String> valueParts = []

        DIRECTIVES.each { String directive ->
            String uri = request.requestURI
            List<String> parts = uri.split(/\//).toList().findAll { it }

            String defCsp = config.get(directive + '.default')
            String uriCsp = config.get(directive + '.uri.' + parts.join('.'))


            List<String> allowed = uriCsp.split(/\s+/).toList()
            String cspString = generateCspHeader(directive, allowed)
            valueParts << cspString

        }

        if (valueParts) {
            headers << new SecurityHeaderImpl(name: CSP_HEADER_NAME, value: valueParts.join(';'))
        }

        headers
    }

    static String generateCspHeader(String directive, List<String> allowed) {
        allowed = allowed?.findAll { it }
        if (!allowed) {
            allowed = ['none']
        }
        directive + ' ' + allowed.collect { "'${it}'" }.join(" ")
    }

    static final List<String> DIRECTIVES = Collections.unmodifiableList(
            [
                    'base-uri',
                    'default-src',
                    'script-src',
                    'object-src',
                    'style-src',
                    'img-src',
                    'media-src',
                    'frame-src',
                    'child-src',
                    'frame-ancestors',
                    'font-src',
                    'connect-src',
                    'manifest-src',
                    'form-action',
                    'sandbox',
                    'script-nonce',
                    'plugin-types',
                    'reflected-xss',
                    'block-all-mixed-content',
                    'upgrade-insecure-requests',
                    'referrer',
                    'report-uri',
                    'report-to',
                    'worker-src'
            ]
    )
}
