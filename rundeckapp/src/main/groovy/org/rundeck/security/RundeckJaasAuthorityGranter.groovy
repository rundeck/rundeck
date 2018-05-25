/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.security

import org.springframework.security.authentication.jaas.AuthorityGranter
import org.springframework.util.Assert

import java.security.Principal


class RundeckJaasAuthorityGranter implements AuthorityGranter {
    //Spring security default role. See SecurityContextHolderAwareRequestFilter class in the spring security docs
    private String rolePrefix = "ROLE_"

    @Override
    Set<String> grant(final Principal principal) {
        return [rolePrefix+ principal.name] as Set
    }

    public String getRolePrefix() { return rolePrefix }
    public String setRolePrefix(String rolePrefix) {
        Assert.notNull(rolePrefix, "Spring security dictates role prefix must not be null");
        this.rolePrefix=rolePrefix
    }
}
