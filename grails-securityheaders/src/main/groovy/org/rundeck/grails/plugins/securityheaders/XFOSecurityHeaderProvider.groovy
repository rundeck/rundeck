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

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Define 'X-Frame-Options' header, default value 'deny',
 * config values: 'sameorigin' enables value 'sameorigin',
 * config value 'allowFrom' defines domain used in `allow-from: DOMAIN`
 * see: <a href="https://www.owasp.org/index.php/OWASP_Secure_Headers_Project#xfo">OWASP</a>
 */
class XFOSecurityHeaderProvider implements SecurityHeaderProvider {
    public static final String XFO_HEADER_NAME = 'X-Frame-Options'
    public static final String XFO_VALUE_DENY = 'deny'
    public static final String XFO_VALUE_SAME_ORIGIN = 'sameorigin'
    public static final String XFO_VALUE_ALLOW_FROM_PREFIX = 'allow-from: '
    String name = 'xfo'
    Boolean defaultEnabled = true

    String defaultValue = XFO_VALUE_DENY

    @Override
    List<SecurityHeader> getSecurityHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Map config
    ) {
        def value = defaultValue
        if (config.get('sameorigin') in ['true', true]) {
            value = XFO_VALUE_SAME_ORIGIN
        } else if (config.get('allowFrom') instanceof String) {
            value = XFO_VALUE_ALLOW_FROM_PREFIX + config.get('allowFrom')
        }
        [
                new SecurityHeaderImpl(name: XFO_HEADER_NAME, value: value)
        ]
    }
}
