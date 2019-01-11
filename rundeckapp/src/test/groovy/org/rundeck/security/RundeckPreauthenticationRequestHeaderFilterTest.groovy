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

import spock.lang.Specification

import javax.servlet.http.HttpServletRequest


class RundeckPreauthenticationRequestHeaderFilterTest extends Specification {
    def "GetPreAuthenticatedPrincipal when provided by AJP"() {
        when:
        def request = Stub(HttpServletRequest) {
            getRemoteUser() >> "userByAjp"
        }

        RundeckPreauthenticationRequestHeaderFilter filter = new RundeckPreauthenticationRequestHeaderFilter()
        def result = filter.getPreAuthenticatedPrincipal(request)

        then:
        result == "userByAjp"
    }

    def "GetPreAuthenticatedCredentials when provided by AJP"() {
        when:
        def request = Stub(HttpServletRequest) {
            getAttribute("REMOTE_USER_GROUPS") >> "admin:user:group1"
        }

        RundeckPreauthenticationRequestHeaderFilter filter = new RundeckPreauthenticationRequestHeaderFilter()
        def result = filter.getPreAuthenticatedCredentials(request)

        then:
        result == "admin:user:group1"
    }

    def "GetPreAuthenticatedPrincipal from header"() {
        when:
        String usernameHeader = "username"
        def request = Stub(HttpServletRequest) {
            getHeader(usernameHeader) >> "the_user"
        }

        RundeckPreauthenticationRequestHeaderFilter filter = new RundeckPreauthenticationRequestHeaderFilter()
        filter.userNameHeader = usernameHeader
        def result = filter.getPreAuthenticatedPrincipal(request)

        then:
        result == "the_user"
    }

    def "GetPreAuthenticatedCredentials from header"() {
        when:
        def request = Stub(HttpServletRequest) {
            getHeader("roles") >> "admin:role1"
        }

        RundeckPreauthenticationRequestHeaderFilter filter = new RundeckPreauthenticationRequestHeaderFilter()
        filter.rolesHeader = "roles"
        def result = filter.getPreAuthenticatedCredentials(request)

        then:
        result == "admin:role1"
    }
}
