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
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider
import org.springframework.security.authentication.jaas.JaasAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.session.SessionDestroyedEvent

import javax.security.auth.login.LoginContext
import javax.security.auth.login.LoginException
import java.security.Principal


class RundeckJaasAuthenticationProvider extends DefaultJaasAuthenticationProvider {

    @Override
    Authentication authenticate(final Authentication auth) throws AuthenticationException {
        // Removed Jetty JAASLoginService thread-local setup - no longer needed
        // with standard Java JAAS implementation
        return super.authenticate(auth)
    }

    /**
     * Override to NOT filter out principals matching the username.
     * 
     * Spring Security's DefaultJaasAuthenticationProvider filters out principals
     * that have the same name as the authenticated username. This causes issues
     * when the username is "admin" and there's also an "admin" role.
     * 
     * Grails 7: This override ensures all role principals are processed,
     * even if they match the username.
     */
    @Override
    protected Set<GrantedAuthority> getAuthorityGranters(Set<Principal> principals) {
        Set<GrantedAuthority> authorities = new HashSet<>()
        
        for (Principal principal : principals) {
            // Process ALL principals through authority granters
            // Don't filter based on username matching
            for (AuthorityGranter granter : getAuthorityGranters()) {
                Set<String> roles = granter.grant(principal)
                if (roles != null) {
                    for (String role : roles) {
                        authorities.add(new SimpleGrantedAuthority(role))
                    }
                }
            }
        }
        
        return authorities
    }

    @Override
    protected void handleLogout(final SessionDestroyedEvent event) {
        List<SecurityContext> contexts = event.getSecurityContexts();

        if (contexts.isEmpty()) {
            this.log.debug("The destroyed session has no SecurityContexts");

            return;
        }

        for (SecurityContext context : contexts) {
            Authentication auth = context.getAuthentication();
            if ((auth != null) && (auth instanceof JaasAuthenticationToken)) {
                JaasAuthenticationToken token = (JaasAuthenticationToken) auth;

                try {
                    LoginContext loginContext = token.getLoginContext();
                    boolean debug = this.log.isDebugEnabled();
                    if (loginContext) {
                        if (debug) {
                            this.log.debug("Logging ${token.getPrincipal()} out of LoginContext(${loginContext.getClass().getSimpleName()})");
                        }
                        loginContext.logout();
                    }
                    else if (debug) {
                        this.log.debug("Unable to logout ${token.getPrincipal()} LoginContext is unavailable");
                    }
                }
                catch (LoginException e) {
                    // Jetty AbstractLoginModule logout bug no longer applies
                    // with our custom JAAS implementation
                    writeWarning("Logout Error", e)
                }
            }
        }
    }

    void writeWarning(String message, LoginException e) {
        this.log.warn(message,e)
    }
}
