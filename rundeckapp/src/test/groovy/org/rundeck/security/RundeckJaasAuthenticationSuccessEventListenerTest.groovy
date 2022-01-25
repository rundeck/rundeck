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
import grails.core.GrailsApplication
import grails.events.bus.EventBus
import org.grails.testing.GrailsUnitTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.jaas.JaasAuthenticationToken
import org.springframework.security.authentication.jaas.event.JaasAuthenticationSuccessEvent
import rundeck.services.ConfigurationService
import rundeck.services.UserService
import spock.lang.Specification

import javax.security.auth.Subject
import javax.security.auth.login.LoginContext
import java.security.Principal


class RundeckJaasAuthenticationSuccessEventListenerTest extends Specification implements GrailsUnitTest {
    def "OnApplicationEvent - syncLdap disabled"() {
        setup:
        RundeckJaasAuthenticationSuccessEventListener listener = new RundeckJaasAuthenticationSuccessEventListener()
        listener.configurationService = Mock(ConfigurationService) {
            getGrailsApplication() >> grailsApplication
            1 * getBoolean('security.syncLdapUser', _) >> false
        }
        listener.targetEventBus = Mock(EventBus)

        when:
        listener.onApplicationEvent(new JaasAuthenticationSuccessEvent(new JaasAuthenticationToken("un",null,Mock(LoginContext))))

        then:
        0 * listener.eventBus.notify(*_)

    }

    def "OnApplicationEvent - syncLdap enabled"() {
        setup:
        RundeckJaasAuthenticationSuccessEventListener listener = new RundeckJaasAuthenticationSuccessEventListener()
        listener.configurationService = Mock(ConfigurationService) {
            getGrailsApplication() >> grailsApplication
            1 * getBoolean('security.syncLdapUser', _) >> true
        }
        listener.targetEventBus = Mock(EventBus)
        Subject subject = new Subject(true, [new LdapEmailPrincipal(email),
                                             new LdapFirstNamePrincipal(first),
                                             new LdapLastNamePrincipal(last)] as Set<Principal>, []as Set<Principal>, []as Set<Principal>);
        def loginContext = Mock(LoginContext)
        loginContext.getSubject() >> subject

        when:
        listener.onApplicationEvent(new JaasAuthenticationSuccessEvent(new JaasAuthenticationToken(username,null,loginContext)))

        then:
        1 * listener.eventBus.notify(
            UserService.G_EVENT_LOGIN_PROFILE_CHANGE,
            {
                it.username == username
                it.lastName == last
                it.firstName == first
                it.email == email
            }
        )

        where:
        username   | first   | last   | email
        "username" | "First" | "Last" | "user@example.com"
        "username" | "First" | null   | "user@example.com"
        "username" | null    | null   | "user@example.com"
    }

    def "OnApplicationEvent - syncLdap enabled with non ldap login"() {
        setup:
        grailsApplication.config.rundeck.security.syncLdapUser = 'true'
        RundeckJaasAuthenticationSuccessEventListener listener = new RundeckJaasAuthenticationSuccessEventListener()
        listener.configurationService = Mock(ConfigurationService) {
            getGrailsApplication() >> grailsApplication
            1 * getBoolean('security.syncLdapUser', _) >> true
        }
        listener.targetEventBus = Mock(EventBus)

        when:
        listener.onApplicationEvent(new JaasAuthenticationSuccessEvent(new UsernamePasswordAuthenticationToken("un","pwd")))

        then:
        0 * listener.eventBus.notify(*_)
        noExceptionThrown()
    }
}
