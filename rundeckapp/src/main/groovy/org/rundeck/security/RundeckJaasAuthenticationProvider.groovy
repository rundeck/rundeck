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

import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider
import org.springframework.security.authentication.jaas.JaasAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.session.SessionDestroyedEvent

import javax.security.auth.login.LoginContext
import javax.security.auth.login.LoginException


class RundeckJaasAuthenticationProvider extends DefaultJaasAuthenticationProvider {
    static final String IGNORE_THIS_ERROR = "java.lang.NullPointerException\n\tat org.eclipse.jetty.jaas.spi.AbstractLoginModule.logout(AbstractLoginModule.java"

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
                    //IGNORE_THIS_ERROR is caused by a jetty bug in the AbstractLoginModule where
                    //they don't check that this.currentUser is not null before calling a method on
                    //the object. This can legitimately happen if the user has configured multiple
                    //jaas modules and the first module has already logged out the user.
                    //This bug is fixed in later versions on jetty but we are pinned to this one right
                    //now because our grails version requires it.
                    if(!e.message.startsWith(IGNORE_THIS_ERROR)) writeWarning("Logout Error", e)
                }
            }
        }
    }

    void writeWarning(String message, LoginException e) {
        this.log.warn(message,e)
    }
}
