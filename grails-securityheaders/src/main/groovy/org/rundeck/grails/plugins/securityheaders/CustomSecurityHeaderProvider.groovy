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
 * Allows custom header definitions.
 * Config:
 * rundeck.security.httpHeaders.provider.custom.enabled=true
 * rundeck.security.httpHeaders.provider.custom.config.name=header name
 * rundeck.security.httpHeaders.provider.custom.config.value=header value
 * # add additional name/value pairs starting at index 2:
 * rundeck.security.httpHeaders.provider.custom.config.name2=header 2 name
 * rundeck.security.httpHeaders.provider.custom.config.value2=header 2 value
 */
class CustomSecurityHeaderProvider implements SecurityHeaderProvider {
    String name = 'generic'
    Boolean defaultEnabled = false

    @Override
    List<SecurityHeader> getSecurityHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Map config
    ) {
        List<SecurityHeader> results = []
        String name = config.get("name")
        String value = config.get("value")
        if (name && value) {
            results << new SecurityHeaderImpl(name: name, value: value)
        }
        int count = 2
        while (config.get("name$count") && config.get("value$count")) {
            results << new SecurityHeaderImpl(name: config.get("name$count"), value: config.get("value$count"))
            count++
        }

        results
    }
}
