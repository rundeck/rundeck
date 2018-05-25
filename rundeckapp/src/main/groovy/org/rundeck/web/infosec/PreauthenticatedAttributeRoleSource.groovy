/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package org.rundeck.web.infosec

import javax.servlet.http.HttpServletRequest
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Returns list of roles defined in a request attribute
 */
class PreauthenticatedAttributeRoleSource implements AuthorizationRoleSource {
    String attributeName
    String delimiter=','
    boolean enabled
    @Override
    Collection<String> getUserRoles(final String username, final HttpServletRequest request) {
        if(enabled && attributeName){
            def value=request.getAttribute(attributeName)
            if(value && value instanceof String){
                return value.split(" *${Pattern.quote(delimiter)} *").collect{it.trim()} as List<String>
            }
        }
        []
    }
}
