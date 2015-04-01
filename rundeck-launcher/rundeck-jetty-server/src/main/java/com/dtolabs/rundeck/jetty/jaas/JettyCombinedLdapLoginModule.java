package com.dtolabs.rundeck.jetty.jaas;

import org.rundeck.jaas.SharedLoginCreds;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adds shared login credentials behavior, can be combined with other modules.
 * Extends the configuration of {@link com.dtolabs.rundeck.jetty.jaas.JettyCachingLdapLoginModule} with these
 * options:
 * <pre>
 * ldaploginmodule {
 *    com.dtolabs.rundeck.jetty.jaas.JettyCombinedLdapLoginModule required
 *    ...
 *    ignoreRoles="true"
 *    storePass="true"
 *    clearPass="true"
 *    useFirstPass="false"
 *    tryFirstPass="false"
 *    ;
 *    };
 * </pre>
 */
public class JettyCombinedLdapLoginModule extends JettyCachingLdapLoginModule {
    protected SharedLoginCreds shared;

    /**
     * if true, ignore ldap role membership
     */
    protected boolean _ignoreRoles = false;

    @Override
    public void initialize(
            final Subject subject,
            final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState,
            final Map<String, ?> options
    )
    {
        super.initialize(subject, callbackHandler, sharedState, options);
        this.shared = new SharedLoginCreds(sharedState, options);
        _ignoreRoles = Boolean.parseBoolean(
                String.valueOf(
                        getOption(
                                options,
                                "ignoreRoles",
                                Boolean.toString(_ignoreRoles)
                        )
                )
        );
    }

    /**
     * Override to perform behavior of "ignoreRoles" option
     *
     * @param dirContext context
     * @param username   username
     *
     * @return empty or supplemental roles list only if "ignoreRoles" is true, otherwise performs normal LDAP lookup
     *
     * @throws LoginException
     * @throws NamingException
     */
    @Override
    protected List getUserRoles(final DirContext dirContext, final String username)
            throws LoginException, NamingException
    {
        if (_ignoreRoles) {
            ArrayList<String> strings = new ArrayList<>();
            addSupplementalRoles(strings);
            return strings;
        } else {
            return super.getUserRoles(dirContext, username);
        }
    }

    /**
     * Override default login logic, to use shared login credentials if available
     *
     * @return true if authenticated
     *
     * @throws LoginException
     */
    @Override
    public boolean login() throws LoginException {
        if ((getShared().isUseFirstPass() || getShared().isTryFirstPass()) && getShared().isHasSharedAuth()) {
            debug(
                    String.format(
                            "JettyCombinedLdapLoginModule: login with shared auth, " +
                            "try? %s, use? %s",
                            getShared().isTryFirstPass(),
                            getShared().isUseFirstPass()
                    )
            );
            setAuthenticated(
                    authenticate(
                            getShared().getSharedUserName(),
                            getShared().getSharedUserPass()
                    )
            );
        }

        if (getShared().isUseFirstPass() && getShared().isHasSharedAuth()) {
            //finish with shared password auth attempt
            debug(String.format("AbstractSharedLoginModule: using login result: %s", isAuthenticated()));
            if (isAuthenticated()) {
                wasAuthenticated(getShared().getSharedUserName(), getShared().getSharedUserPass());
            }
            return isAuthenticated();
        }
        if (getShared().isHasSharedAuth()) {
            if (isAuthenticated()) {
                return isAuthenticated();
            }
            debug(String.format("AbstractSharedLoginModule: shared auth failed, now trying callback auth."));
        }
        Object[] userPass = new Object[0];
        try {
            userPass = getCallBackAuth();
        } catch (IOException e) {
            if (isDebug()) {
                e.printStackTrace();
            }
            throw new LoginException(e.toString());
        } catch (UnsupportedCallbackException e) {
            if (isDebug()) {
                e.printStackTrace();
            }
            throw new LoginException(e.toString());
        }
        if (null == userPass || userPass.length < 2) {
            setAuthenticated(false);
        } else {
            String name = (String) userPass[0];
            Object pass = userPass[1];
            setAuthenticated(authenticate(name, pass));

            if (isAuthenticated()) {
                wasAuthenticated(name, pass);
            }
        }
        return isAuthenticated();
    }

    protected void wasAuthenticated(String user, Object pass) {
        //store shared credentials if successful and not already stored
        if (isAuthenticated() && getShared().isStorePass() && !getShared().isHasSharedAuth()) {
            getShared().storeLoginCreds(user, pass);
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (getShared().isClearPass() && getShared().isHasSharedAuth()) {
            getShared().clear();
        }
        return super.commit();
    }


    public SharedLoginCreds getShared() {
        return shared;
    }

}
