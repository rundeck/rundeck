/*
 * Copyright 2013 SimplifyOps
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.jaas.jetty;

import org.jvnet.libpam.UnixUser;
import org.rundeck.jaas.pam.AbstractPamLoginModule;

import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;

/**
 * Jetty 6 login module using PAM, uses Jetty6 principal classes and authentication callback.
 */
public class JettyPamLoginModule extends AbstractPamLoginModule {
    @Override
    protected Principal createUserPrincipal(UnixUser user) {
        return JettySupport.createUserPrincipal(user.getUserName());
    }

    /**
     * Return the result of handling the Jetty callbacks
     *
     * @return
     *
     * @throws IOException
     * @throws UnsupportedCallbackException
     * @throws LoginException
     */
    protected Object[] getCallBackAuth() throws IOException, UnsupportedCallbackException, LoginException {
        return JettySupport.performCallbacks(getCallbackHandler());
    }

    @Override
    protected Principal createRolePrincipal(String role) {
        return JettySupport.createRolePrincipal(role);
    }
}
