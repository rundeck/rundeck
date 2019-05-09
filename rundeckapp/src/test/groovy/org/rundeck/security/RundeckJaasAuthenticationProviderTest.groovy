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

import org.eclipse.jetty.jaas.spi.PropertyFileLoginModule
import org.springframework.security.authentication.jaas.JaasAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.session.SessionDestroyedEvent
import org.springframework.security.web.session.HttpSessionDestroyedEvent
import spock.lang.Specification

import javax.security.auth.login.LoginContext
import javax.security.auth.login.LoginException


class RundeckJaasAuthenticationProviderTest extends Specification {
    def "Handle Normal Logout"() {
        when:
        RundeckJaasAuthenticationProvider provider = new RundeckJaasAuthenticationProvider()
        LoginContext lcontext = Stub(LoginContext) {
            logout() >> {}
        }
        TestSecurityContext testContext = new TestSecurityContext(testAuth: new JaasAuthenticationToken("user","pass",lcontext))
        def sessionDestroyedEvent = Stub(HttpSessionDestroyedEvent) {
            getSecurityContexts() >> [testContext]
        }

        provider.handleLogout(sessionDestroyedEvent)

        then:
        noExceptionThrown()
    }

    def "Handle Error Throwing Logout"() {
        when:
        RundeckJaasAuthenticationProvider provider = new RundeckJaasAuthenticationProvider()
        LoginContext lcontext = Stub(LoginContext) {
            logout() >> { throw new LoginException("Something bad happened")}
        }
        TestSecurityContext testContext = new TestSecurityContext(testAuth: new JaasAuthenticationToken("user","pass",lcontext))
        def sessionDestroyedEvent = Stub(HttpSessionDestroyedEvent) {
            getSecurityContexts() >> [testContext]
        }
        provider.metaClass.writeWarning = { msg, ex ->
            ex.message == "Something bad happened"
        }
        provider.handleLogout(sessionDestroyedEvent)

        then:
        noExceptionThrown()
    }

    def "Handle Ignored Error Logout"() {
        when:
        RundeckJaasAuthenticationProvider provider = new RundeckJaasAuthenticationProvider()
        LoginContext lcontext = Stub(LoginContext) {
            logout() >> { throw new LoginException(RundeckJaasAuthenticationProvider.IGNORE_THIS_ERROR)}
        }
        TestSecurityContext testContext = new TestSecurityContext(testAuth: new JaasAuthenticationToken("user","pass",lcontext))
        def sessionDestroyedEvent = Stub(HttpSessionDestroyedEvent) {
            getSecurityContexts() >> [testContext]
        }
        boolean neverCalled = true
        provider.metaClass.writeWarning = { msg, ex ->
            neverCalled = false
        }
        provider.handleLogout(sessionDestroyedEvent)

        then:
        noExceptionThrown()
        neverCalled
    }

    class TestSecurityContext implements SecurityContext {

        Authentication testAuth

        @Override
        Authentication getAuthentication() {
            return testAuth
        }

        @Override
        void setAuthentication(final Authentication authentication) {
            testAuth = authentication
        }
    }
}
