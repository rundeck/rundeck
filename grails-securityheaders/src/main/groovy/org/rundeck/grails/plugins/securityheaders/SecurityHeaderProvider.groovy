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
 * provides headers to add to the response
 */
@CompileStatic
interface SecurityHeaderProvider {
    /**
     * provider name
     * @return
     */
    String getName()

    /**
     *
     * @return true if enabled by default
     */
    Boolean getDefaultEnabled()

    /**
     *
     * @param request
     * @param response
     * @param config
     * @return list of headers to add to response
     */
    List<SecurityHeader> getSecurityHeaders(HttpServletRequest request, HttpServletResponse response, Map config)

    /**
     * Builds a header with the given name and value.
     */
    default SecurityHeader header(String name, String value) {
        new SecurityHeaderImpl(name: name, value: value)
    }
}
