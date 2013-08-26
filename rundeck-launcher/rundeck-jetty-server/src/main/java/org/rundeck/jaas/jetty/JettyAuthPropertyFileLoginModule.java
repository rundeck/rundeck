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

import org.eclipse.jetty.plus.jaas.callback.ObjectCallback;
import org.eclipse.jetty.plus.jaas.spi.PropertyFileLoginModule;
import org.eclipse.jetty.plus.jaas.spi.UserInfo;
import org.rundeck.jaas.AbstractSharedLoginModule;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Augments Jetty property file login module {@link PropertyFileLoginModule}, to only perform authentication
 * via property file login, handles shared credentials logic, and does not use property file roles.
 */
public class JettyAuthPropertyFileLoginModule extends AbstractSharedLoginModule {
    public static final Logger logger = Logger.getLogger(JettyAuthPropertyFileLoginModule.class.getName());
    PropertyFileLoginModule module;
    UserInfo userInfo;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map shared, Map options) {
        super.initialize(subject, callbackHandler, shared, options);
        module = new PropertyFileLoginModule();
        module.initialize(subject, callbackHandler, shared, options);
    }

    /**
     * Uses jetty callbacks to retrieve auth credentials
     * @return
     * @throws IOException
     * @throws UnsupportedCallbackException
     * @throws LoginException
     */
    protected Object[] getCallBackAuth() throws IOException, UnsupportedCallbackException, LoginException {
        return JettySupport.performCallbacks(getCallbackHandler());
    }

    @Override
    protected Principal createUserPrincipal() {
        return JettySupport.createUserPrincipal(userInfo.getUserName());
    }

    /**
     * Does not generate role principals for the user.
     * @return
     */
    @Override
    protected List<Principal> createRolePrincipals() {
        //Do not use roles from property file.
        return null;
    }


    @Override
    protected boolean authenticate(String userName, char[] chars) throws LoginException {
        try {
            this.userInfo = module.getUserInfo(userName);
            if (null == this.userInfo) {
                debug(String.format("JettyAuthPropertyFileLoginModule: userInfo not found for %s", userName));
                return false;
            }
            boolean b = this.userInfo.checkCredential(new String(chars));
            debug(String.format("JettyAuthPropertyFileLoginModule: checkCredential? %s", b));
            return b;
        } catch (Exception e) {
            if (isDebug()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (!isAuthenticated()) {
            userInfo = null;
        }
        return super.commit();
    }

    @Override
    public boolean abort() throws LoginException {
        userInfo = null;

        return super.abort();
    }

    @Override
    public boolean logout() throws LoginException {

        userInfo = null;

        return super.logout();
    }


    /**
     * Emit Debug message via System.err by default
     *
     * @param message
     */
    protected void debug(String message) {
        logger.log(Level.INFO, message);
    }
}
