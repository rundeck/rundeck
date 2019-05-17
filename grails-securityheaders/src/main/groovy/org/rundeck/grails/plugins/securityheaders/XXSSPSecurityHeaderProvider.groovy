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
 * Adds `X-XSS-Protection: 1`  by default,
 * config 'block' value of true sends `X-XSS-Protection: 1; mode=block`
 * config `report: uri` value sends `X-XSS-Protection: 1; report=uri`
 * See:
 * <a href="https://www.owasp.org/index.php/OWASP_Secure_Headers_Project#xxxsp">OWASP</a>
 */
class XXSSPSecurityHeaderProvider implements SecurityHeaderProvider {
    public static final String X_XSS_PROTECTION_HEADER_NAME = 'X-XSS-Protection'
    String name = 'xxssp'
    Boolean defaultEnabled = true

    @Override
    List<SecurityHeader> getSecurityHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Map config
    ) {
        def value = '1'
        if (config.block in ['true', true]) {
            value = '1; mode=block'
        } else if (config.report) {
            value = '1; report=' + config.report
        }

        [
                new SecurityHeaderImpl(name: X_XSS_PROTECTION_HEADER_NAME, value: value)
        ]
    }
}
