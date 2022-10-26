/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import spock.lang.Specification

class ContainerRoleSourceSpec extends Specification {
    def "GetUserRoles"() {
        setup:
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, "***", userRoles)
        SecurityContextHolder.setContext(new SecurityContextImpl(auth))

        when:
        def roles = new ContainerRoleSource().getUserRoles(user, null)

        then:
        roles == expected

        where:
        user    | userRoles                                                                 | expected
        "admin" | [new SimpleGrantedAuthority("admin"), new SimpleGrantedAuthority("user")] | ["admin", "user"]
        "admin" | [new SimpleGrantedAuthority("admin")]                                     | ["admin"]
        "admin" | [new SimpleGrantedAuthority("admin,user")]                                | ["admin", "user"]
        "admin" | null                                                                      | []

    }
}
