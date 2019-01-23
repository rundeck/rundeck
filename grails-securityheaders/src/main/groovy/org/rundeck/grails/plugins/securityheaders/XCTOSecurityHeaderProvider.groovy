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
 * Defines 'X-Content-Type-Options: nosniff' header
 */
class XCTOSecurityHeaderProvider implements SecurityHeaderProvider {
    public static final String X_CONTENT_TYPE_OPTIONS_HEADER_NAME = 'X-Content-Type-Options'
    public static final String XCTO_HEADER_VALUE_NOSNIFF = 'nosniff'
    public static final SecurityHeaderImpl XCTO_HEADER = new SecurityHeaderImpl(
            name: X_CONTENT_TYPE_OPTIONS_HEADER_NAME,
            value: XCTO_HEADER_VALUE_NOSNIFF
    )
    String name = 'xcto'
    Boolean defaultEnabled = true

    @Override
    List<SecurityHeader> getSecurityHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Map config
    ) {
        [XCTO_HEADER]
    }
}
