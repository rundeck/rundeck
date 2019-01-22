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

/**
 * reference:
 *
 *  https://www.owasp.org/index.php/OWASP_Secure_Headers_Project#csp
 *  https://content-security-policy.com/
 *
 *  configuration:
 *
 *  `policy`: explicit policy string, if set individual directives will be ignored
 *  `<directive>`: individual directive values, see {@link CSPSecurityHeaderProvider#DIRECTIVES}
 *  `include-xcsp-header`: add policy in X-Content-Security-Policy header as well (default:true)
 *  `include-xwkcsp-header`: set policy in X-WebKit-CSP header as well (default:true)
 *
 */
@CompileStatic
class CSPSecurityHeaderProvider implements SecurityHeaderProvider {

    public static final String CSP_HEADER_NAME = 'Content-Security-Policy'
    public static final String XCSP_HEADER_NAME = 'X-Content-Security-Policy'
    public static final String X_WEBKIT_CSP_HEADER_NAME = 'X-WebKit-CSP'
    public static final String CONFIG_INCLUDE_X_CSP_HEADER = 'include-xcsp-header'
    public static final String CONFIG_INCLUDE_X_WEBKIT_CSP_HEADER = 'include-xwkcsp-header'
    public static final String CONFIG_POLICY = 'policy'

    String name = 'csp'
    Boolean defaultEnabled = false
    Map<String, String> directives

    List<SecurityHeader> builtHeaders

    @Override
    List<SecurityHeader> getSecurityHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            Map config
    ) {
        buildHeaders(config)
    }

    private List<SecurityHeader> buildHeaders(Map config) {
        if (null == builtHeaders) {
            synchronized (this) {
                if (builtHeaders == null) {
                    String headerValue
                    if (config.get(CONFIG_POLICY)) {
                        //explicit policy definition
                        headerValue = config.get(CONFIG_POLICY)
                    } else {
                        //define by directive name

                        List<String> valueParts = []
                        DIRECTIVES.each { String directive ->
                            String val = config.get(directive)
                            if (val) {
                                List<String> values = val.split(/\s+/).toList()
                                String cspString = generateCspHeader(directive, values)
                                if (cspString) {
                                    valueParts << cspString
                                }
                            }
                        }

                        if (valueParts) {
                            headerValue = valueParts.join(' ; ') + ' ;'
                        } else {
                            throw new IllegalStateException(
                                    "$CONFIG_POLICY or directive configuration is required, directives: $DIRECTIVES"
                            )
                        }
                    }
                    List<SecurityHeader> built = []

                    built << new SecurityHeaderImpl(name: CSP_HEADER_NAME, value: headerValue)
                    if (getBoolean(config, CONFIG_INCLUDE_X_CSP_HEADER, false)) {
                        built << new SecurityHeaderImpl(name: XCSP_HEADER_NAME, value: headerValue)
                    }
                    if (getBoolean(config, CONFIG_INCLUDE_X_WEBKIT_CSP_HEADER, false)) {
                        built << new SecurityHeaderImpl(name: X_WEBKIT_CSP_HEADER_NAME, value: headerValue)
                    }

                    builtHeaders = built

                }
            }
        }
        builtHeaders
    }

    static boolean getBoolean(final Map map, final String key, final boolean defVal) {
        if (null == map.get(key)) {
            return defVal
        }
        map.get(key) in [true, 'true']
    }

    static String generateCspHeader(String directive, List<String> allowed) {
        allowed = allowed?.findAll { it?.trim() }
        if (!allowed) {
            return null
        }
        directive + ' ' + allowed.collect {
            if (it in SRC_KEYWORDS || SRC_PREFIXES.any { pref -> it.startsWith(pref) }) {
                "'${it}'"
            } else {
                it
            }
        }.join(" ")
    }

    static final List<String> SRC_KEYWORDS = Collections.unmodifiableList(
            [
                    'none',
                    'self',
                    'unsafe-inline',
                    'unsafe-eval'
            ]
    )

    static final List<String> SRC_PREFIXES = Collections.unmodifiableList(
            [
                    'nonce-',
                    'sha256-',
            ]
    )

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
