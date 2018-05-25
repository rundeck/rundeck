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

package org.rundeck.jaas.pam;

import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;
import org.rundeck.jaas.AbstractSharedLoginModule;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base login module for using libpam4j to authenticate.
 */
public abstract class AbstractPamLoginModule extends AbstractSharedLoginModule {
    public static final Logger logger = Logger.getLogger(AbstractPamLoginModule.class.getName());
    private String serviceName;

    private UnixUser unixUser;
    private boolean useUnixGroups;

    private List<String> supplementalRoles;


    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map shared, Map options) {
        super.initialize(subject, callbackHandler, shared, options);
        Object service = options.get("service");
        if (null == service) {
            throw new IllegalStateException("service is required");
        }
        this.serviceName = service.toString();

        Object useUnixGroups1 = options.get("useUnixGroups");
        if (null != useUnixGroups1) {
            this.useUnixGroups = Boolean.parseBoolean(useUnixGroups1.toString());
        } else {
            this.useUnixGroups = false;
        }
        Object supplementalRoles1 = options.get("supplementalRoles");
        if (null != supplementalRoles1) {
            this.supplementalRoles = new ArrayList<String>();
            this.supplementalRoles.addAll(Arrays.asList(supplementalRoles1.toString().split(", *")));
        }
    }


    /**
     * Authenticates using PAM
     * @param name
     * @param password
     * @return
     * @throws LoginException
     */
    protected boolean authenticate(String name, char[] password) throws LoginException {
        try {
            if ((name == null) || (password == null)) {
                debug("user or pass is null");
                return false;
            }
            debug("PAM authentication trying (" + serviceName + ") for: " + name);
            UnixUser authenticate = new PAM(serviceName).authenticate(name, new String(password));
            debug("PAM authentication succeeded for: " + name);
            this.unixUser = authenticate;

            return true;
        } catch (PAMException e) {
            debug(e.getMessage());
            if (isDebug()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Emit Debug message via System.err by default
     *
     * @param message
     */
    protected void debug(String message) {
        logger.log(Level.INFO, message);
    }


    @Override
    protected List<Principal> createRolePrincipals() {
        return createRolePrincipals(unixUser);
    }

    @Override
    protected Principal createUserPrincipal() {
        return createUserPrincipal(unixUser);
    }

    /**
     * Create a Principal for the user
     *
     * @param user
     *
     * @return
     */
    protected abstract Principal createUserPrincipal(UnixUser user);

    /**
     * Create a role Principal
     *
     * @param role
     *
     * @return
     */
    protected abstract Principal createRolePrincipal(String role);

    /**
     * Create Principals for any roles
     *
     * @param username
     *
     * @return
     */
    protected List<Principal> createRolePrincipals(UnixUser username) {
        ArrayList<Principal> principals = new ArrayList<Principal>();
        if (null != supplementalRoles) {
            for (String supplementalRole : supplementalRoles) {
                Principal rolePrincipal = createRolePrincipal(supplementalRole);
                if (null != rolePrincipal) {
                    principals.add(rolePrincipal);
                }
            }
        }
        if (useUnixGroups) {
            for (String s : username.getGroups()) {
                Principal rolePrincipal = createRolePrincipal(s);
                if (null != rolePrincipal) {
                    principals.add(rolePrincipal);
                }
            }
        }
        return principals;
    }

    @Override
    public boolean commit() throws LoginException {
        if (!isAuthenticated()) {
            unixUser = null;
        }
        return super.commit();
    }

    @Override
    public boolean abort() throws LoginException {
        unixUser = null;

        return super.abort();
    }

    @Override
    public boolean logout() throws LoginException {

        unixUser = null;

        return super.logout();
    }


    public boolean isUseUnixGroups() {
        return useUnixGroups;
    }
}
