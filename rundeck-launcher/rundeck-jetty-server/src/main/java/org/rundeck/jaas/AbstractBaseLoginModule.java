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

package org.rundeck.jaas;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Base login module that supports `debug` option, and manages principals stored in the Subject.
 */
public abstract class AbstractBaseLoginModule implements LoginModule {
    private boolean debug;
    private boolean authenticated;
    private boolean committed;
    private Principal userPrincipal;
    private List<Principal> rolePrincipals;
    private Subject subject;

    private CallbackHandler callbackHandler;


    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> stringMap, Map<String, ?> options) {
        Object debug1 = options.get("debug");
        if (null != debug1) {
            this.debug = Boolean.parseBoolean(debug1.toString());
        }

        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    /**
     * Default behavior to emit to System.err
     * @param message
     */
    protected void debug(String message) {
        if (isDebug()) {
            System.err.println(message);
        }
    }


    public boolean isDebug() {
        return debug;
    }

    protected boolean isAuthenticated() {
        return authenticated;
    }

    protected void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    protected boolean isCommitted() {
        return committed;
    }

    protected void setCommitted(boolean committed) {
        this.committed = committed;
    }

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    /**
     * Called after authentication succeeds
     */
    protected void wasAuthenticated() {
        this.userPrincipal = createUserPrincipal();
        this.rolePrincipals = createRolePrincipals();
    }

    /**
     * Create the list of Principals for roles
     *
     * @return
     */
    protected abstract List<Principal> createRolePrincipals();

    /**
     * Create the use Principal
     *
     * @return
     */
    protected abstract Principal createUserPrincipal();

    /**
     * Set the principals for the Subject
     */
    private void setSubjectPrincipals() {
        if (null != userPrincipal) {
            this.subject.getPrincipals().add(userPrincipal);
        }
        if (null != rolePrincipals) {
            for (Principal rolePrincipal : rolePrincipals) {
                this.subject.getPrincipals().add(rolePrincipal);
            }
        }
    }

    private void clearSubjectPrincipals() {
        if (null != userPrincipal) {
            this.subject.getPrincipals().remove(userPrincipal);
            userPrincipal = null;
        }
        if (null != rolePrincipals) {
            this.subject.getPrincipals().removeAll(rolePrincipals);
            rolePrincipals = null;
        }
    }


    @Override
    public boolean commit() throws LoginException {
        if (!isAuthenticated()) {
            setCommitted(false);
        } else {
            setSubjectPrincipals();
            setCommitted(true);
        }
        return isCommitted();
    }

    @Override
    public boolean logout() throws LoginException {
        setAuthenticated(false);
        clearSubjectPrincipals();
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return isAuthenticated() && isCommitted();
    }


}
