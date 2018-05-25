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

import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.FrameworkService

import javax.servlet.http.HttpServletRequest

/**
 * Returns list of known roles that user is in via {@link HttpServletRequest#isUserInRole(java.lang.String)}
 */
class ContainerRoleSource implements AuthorizationRoleSource {
    boolean enabled
    @Autowired
    def FrameworkService frameworkService
    @Override
    Collection<String> getUserRoles(final String username, final HttpServletRequest request) {
        def roles=new ArrayList<String>()
        //try to determine roles based on aclpolicy group definitions
        frameworkService.getFrameworkRoles().each { rolename ->
            if (request.isUserInRole(rolename)) {
                roles<<rolename
            }
        }
        roles
    }
}
