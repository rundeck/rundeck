/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

import org.rundeck.jaas.RundeckPrincipal
import org.rundeck.jaas.RundeckRole
import spock.lang.Specification

import java.security.Principal

class RundeckJaasAuthorityGranterTest extends Specification {

    def "grants RundeckRole principals"() {
        given:
        RundeckJaasAuthorityGranter granter = new RundeckJaasAuthorityGranter()

        when:
        def result = granter.grant(new RundeckRole("admin"))

        then:
        result == ["ROLE_admin"] as Set
    }

    def "grants role-like principals from native Jetty JAAS SPI login modules (e.g. JDBCLoginModule)"() {
        given: "a Principal whose class name follows the *Role convention, mirroring org.eclipse.jetty.security.jaas.JAASRole"
        RundeckJaasAuthorityGranter granter = new RundeckJaasAuthorityGranter()

        when:
        def result = granter.grant(new FakeJAASRole("dataadmin"))

        then:
        result == ["ROLE_dataadmin"] as Set
    }

    def "does not grant non-role principals"() {
        given:
        RundeckJaasAuthorityGranter granter = new RundeckJaasAuthorityGranter()

        when:
        def result = granter.grant(new RundeckPrincipal("someuser"))

        then:
        result == null
    }

    def "honors a custom rolePrefix"() {
        given:
        RundeckJaasAuthorityGranter granter = new RundeckJaasAuthorityGranter()
        granter.setRolePrefix("CUSTOM_")

        expect:
        granter.grant(new RundeckRole("admin")) == ["CUSTOM_admin"] as Set
        granter.grant(new FakeJAASRole("dataadmin")) == ["CUSTOM_dataadmin"] as Set
    }

    static class FakeJAASRole implements Principal {
        private final String name

        FakeJAASRole(String name) {
            this.name = name
        }

        @Override
        String getName() {
            return name
        }
    }
}
