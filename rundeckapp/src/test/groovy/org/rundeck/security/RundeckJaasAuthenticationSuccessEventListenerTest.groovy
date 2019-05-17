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

import com.dtolabs.rundeck.jetty.jaas.LdapEmailPrincipal
import com.dtolabs.rundeck.jetty.jaas.LdapFirstNamePrincipal
import com.dtolabs.rundeck.jetty.jaas.LdapLastNamePrincipal
import org.grails.testing.GrailsUnitTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.jaas.JaasAuthenticationToken
import org.springframework.security.authentication.jaas.event.JaasAuthenticationSuccessEvent
import rundeck.services.UserService
import spock.lang.Specification

import javax.security.auth.Subject
import javax.security.auth.login.LoginContext
import java.security.Principal


class RundeckJaasAuthenticationSuccessEventListenerTest extends Specification implements GrailsUnitTest {
    def "OnApplicationEvent - syncLdap disabled"() {
        setup:
        grailsApplication.config.rundeck.security.syncLdapUser = false

        when:
        RundeckJaasAuthenticationSuccessEventListener listener = new RundeckJaasAuthenticationSuccessEventListener()
        listener.grailsApplication = grailsApplication
        listener.userService = Mock(UserService)
        0 * listener.userService.updateUserProfile(_,_,_,_)
        listener.onApplicationEvent(new JaasAuthenticationSuccessEvent(new JaasAuthenticationToken("un",null,Mock(LoginContext))))

        then:
        true
    }

    def "OnApplicationEvent - syncLdap enabled"() {
        setup:
        grailsApplication.config.rundeck.security.syncLdapUser = true

        when:
        RundeckJaasAuthenticationSuccessEventListener listener = new RundeckJaasAuthenticationSuccessEventListener()
        listener.grailsApplication = grailsApplication
        listener.userService = Mock(UserService) {
            1 * updateUserProfile({it == username},{it == last},{it == first},{it == email}) >> {}
        }
        Subject subject = new Subject(true, [new LdapEmailPrincipal(email),
                                             new LdapFirstNamePrincipal(first),
                                             new LdapLastNamePrincipal(last)] as Set<Principal>, []as Set<Principal>, []as Set<Principal>);
        def loginContext = Mock(LoginContext)
        loginContext.getSubject() >> subject
        listener.onApplicationEvent(new JaasAuthenticationSuccessEvent(new JaasAuthenticationToken(username,null,loginContext)))

        then:
        true

        where:
        username   | first   | last   | email
        "username" | "First" | "Last" | "user@example.com"
        "username" | "First" | null   | "user@example.com"
        "username" | null    | null   | "user@example.com"
    }

    def "OnApplicationEvent - syncLdap enabled with non ldap login"() {
        setup:
        grailsApplication.config.rundeck.security.syncLdapUser = true

        when:
        RundeckJaasAuthenticationSuccessEventListener listener = new RundeckJaasAuthenticationSuccessEventListener()
        listener.grailsApplication = grailsApplication
        listener.userService = Mock(UserService) {
            0 * updateUserProfile(_,_,_,_)
        }
        listener.onApplicationEvent(new JaasAuthenticationSuccessEvent(new UsernamePasswordAuthenticationToken("un","pwd")))

        then:
        noExceptionThrown()

    }
}
