/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.jaas

import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject

/**
 * Tests for RundeckPrincipal and RundeckRole to ensure correct equals() behavior.
 * 
 * Grails 7: Critical tests for JAAS Principal equals() bug fix.
 * These tests ensure that principals with the same name but different types
 * are not deduplicated when added to a Subject's Principal Set.
 * 
 * Bug Context: Previously, equals() compared principals only by name, causing
 * RundeckPrincipal[admin] and RundeckRole[admin] to be considered equal,
 * resulting in the role being deduplicated and the admin user losing admin privileges.
 */
class RundeckJaasPrincipalSpec extends Specification {

    def "RundeckPrincipal and RundeckRole with same name are not equal"() {
        given: "A principal and role with the same name"
        def principal = new RundeckPrincipal("admin")
        def role = new RundeckRole("admin")
        
        expect: "They are not equal"
        principal != role
        !principal.equals(role)
        !role.equals(principal)
    }

    def "RundeckPrincipal and RundeckRole have same hash code when same name"() {
        given: "A principal and role with the same name"
        def principal = new RundeckPrincipal("admin")
        def role = new RundeckRole("admin")
        
        expect: "They have the same hash code (both use name.hashCode())"
        // Note: Same hash code is OK - equals() is what prevents deduplication
        principal.hashCode() == role.hashCode()
    }

    def "Subject can contain both RundeckPrincipal and RundeckRole with same name"() {
        given: "An empty Subject"
        Subject subject = new Subject()
        
        when: "Adding principal and role with same name"
        boolean addedPrincipal = subject.getPrincipals().add(new RundeckPrincipal("admin"))
        boolean addedRole = subject.getPrincipals().add(new RundeckRole("admin"))
        
        then: "Both are successfully added"
        addedPrincipal
        addedRole
        
        and: "Subject contains both"
        subject.getPrincipals().size() == 2
        subject.getPrincipals(RundeckPrincipal).size() == 1
        subject.getPrincipals(RundeckRole).size() == 1
    }

    @Unroll
    def "Subject can contain principal and multiple roles including one matching username: #username"() {
        given: "An empty Subject"
        Subject subject = new Subject()
        
        when: "Adding principal and roles including one that matches username"
        subject.getPrincipals().add(new RundeckPrincipal(username))
        roles.each { roleName ->
            subject.getPrincipals().add(new RundeckRole(roleName))
        }
        
        then: "All principals are added"
        subject.getPrincipals().size() == 1 + roles.size()
        subject.getPrincipals(RundeckPrincipal).size() == 1
        subject.getPrincipals(RundeckRole).size() == roles.size()
        
        and: "All roles are present"
        def rolePrincipals = subject.getPrincipals(RundeckRole)
        roles.every { roleName ->
            rolePrincipals.find { it.name == roleName }
        }
        
        where:
        username     | roles
        "admin"      | ["admin", "user", "architect"]
        "developers" | ["developers", "users", "admin"]
        "user"       | ["user"]
        "deploy"     | ["deploy", "build", "architect"]
    }

    def "RundeckPrincipal equals itself"() {
        given:
        def principal = new RundeckPrincipal("testuser")
        
        expect:
        principal.equals(principal)
        principal == principal
    }

    def "RundeckRole equals itself"() {
        given:
        def role = new RundeckRole("testrole")
        
        expect:
        role.equals(role)
        role == role
    }

    def "Two RundeckPrincipals with same name are equal"() {
        given:
        def principal1 = new RundeckPrincipal("testuser")
        def principal2 = new RundeckPrincipal("testuser")
        
        expect:
        principal1 == principal2
        principal1.hashCode() == principal2.hashCode()
    }

    def "Two RundeckRoles with same name are equal"() {
        given:
        def role1 = new RundeckRole("testrole")
        def role2 = new RundeckRole("testrole")
        
        expect:
        role1 == role2
        role1.hashCode() == role2.hashCode()
    }

    def "RundeckPrincipal and RundeckRole with different names are not equal"() {
        given:
        def principal = new RundeckPrincipal("user1")
        def role = new RundeckRole("admin")
        
        expect:
        principal != role
        !principal.equals(role)
        !role.equals(principal)
    }

    def "RundeckPrincipal is not equal to null"() {
        given:
        def principal = new RundeckPrincipal("testuser")
        
        expect:
        !principal.equals(null)
        principal != null
    }

    def "RundeckRole is not equal to null"() {
        given:
        def role = new RundeckRole("testrole")
        
        expect:
        !role.equals(null)
        role != null
    }

    def "RundeckPrincipal is not equal to arbitrary object"() {
        given:
        def principal = new RundeckPrincipal("testuser")
        def arbitraryObject = "not a principal"
        
        expect:
        !principal.equals(arbitraryObject)
        principal != arbitraryObject
    }

    def "RundeckRole is not equal to arbitrary object"() {
        given:
        def role = new RundeckRole("testrole")
        def arbitraryObject = "not a role"
        
        expect:
        !role.equals(arbitraryObject)
        role != arbitraryObject
    }

    def "Subject deduplication works correctly for same principal type"() {
        given: "A Subject with a principal"
        Subject subject = new Subject()
        subject.getPrincipals().add(new RundeckPrincipal("admin"))
        
        when: "Adding the same principal again"
        boolean added = subject.getPrincipals().add(new RundeckPrincipal("admin"))
        
        then: "It is not added (deduplicated)"
        !added
        subject.getPrincipals().size() == 1
    }

    def "Subject deduplication works correctly for same role type"() {
        given: "A Subject with a role"
        Subject subject = new Subject()
        subject.getPrincipals().add(new RundeckRole("admin"))
        
        when: "Adding the same role again"
        boolean added = subject.getPrincipals().add(new RundeckRole("admin"))
        
        then: "It is not added (deduplicated)"
        !added
        subject.getPrincipals().size() == 1
    }

    def "Complex scenario: admin user with admin role among other roles"() {
        given: "An admin user authenticating"
        Subject subject = new Subject()
        def username = "admin"
        def roles = ["user", "admin", "architect", "deploy", "build"]
        
        when: "Adding principal and all roles"
        subject.getPrincipals().add(new RundeckPrincipal(username))
        roles.each { roleName ->
            subject.getPrincipals().add(new RundeckRole(roleName))
        }
        
        then: "Subject contains 1 principal + 5 roles = 6 principals"
        subject.getPrincipals().size() == 6
        
        and: "Principal and role with name 'admin' both exist"
        def adminPrincipal = subject.getPrincipals(RundeckPrincipal).find { it.name == "admin" }
        def adminRole = subject.getPrincipals(RundeckRole).find { it.name == "admin" }
        adminPrincipal != null
        adminRole != null
        adminPrincipal != adminRole
        
        and: "All roles are present"
        def rolePrincipals = subject.getPrincipals(RundeckRole)
        rolePrincipals.size() == 5
        roles.every { roleName ->
            rolePrincipals.find { it.name == roleName }
        }
    }
}

