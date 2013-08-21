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

import org.rundeck.jaas.AbstractBaseLoginModule;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Map;

/**
 * Abstract login module supporting the `useFirstPass`,`tryFirstPass`,`storePass` and `clearPass` options.
 */
public abstract class AbstractSharedLoginModule extends AbstractBaseLoginModule {
    public static final String SHARED_LOGIN_NAME = "javax.security.auth.login.name";
    public static final String SHARED_LOGIN_PASSWORD = "javax.security.auth.login.password";
    private boolean useFirstPass;
    private boolean tryFirstPass;
    private boolean storePass;
    private boolean clearPass;
    private Map<String, Object> sharedState;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map shared, Map options) {
        super.initialize(subject, callbackHandler, shared, options);
        this.sharedState = shared;
        if (options.get("useFirstPass") != null) {
            useFirstPass = Boolean.parseBoolean(options.get("useFirstPass").toString());
        }
        if (options.get("tryFirstPass") != null) {
            tryFirstPass = Boolean.parseBoolean(options.get("tryFirstPass").toString());
        }
        if (options.get("storePass") != null) {
            storePass = Boolean.parseBoolean(options.get("storePass").toString());
        }
        if (options.get("clearPass") != null) {
            clearPass = Boolean.parseBoolean(options.get("clearPass").toString());
        }
    }

    @Override
    public boolean login() throws LoginException {
        if ((isUseFirstPass() || isTryFirstPass()) && isHasSharedAuth()) {
            debug(String.format("AbstractSharedLoginModule: login with shared auth, " +
                    "try? %s, use? %s", isTryFirstPass(), isUseFirstPass()));
            setAuthenticated(authenticate(getSharedUserName(), getSharedUserPass().toString().toCharArray()));
        }

        if (isUseFirstPass() && isHasSharedAuth()) {
            //finish with shared password auth attempt
            debug(String.format("AbstractSharedLoginModule: using login result: %s", isAuthenticated()));
            if (isAuthenticated()) {
                wasAuthenticated(getSharedUserName(), getSharedUserPass());
            }
            return isAuthenticated();
        }
        if (isHasSharedAuth()) {
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
        if (null == userPass || userPass.length < 2 || null == userPass[0] && null == userPass[1]) {
            setAuthenticated(false);
        } else {
            String name = (String) userPass[0];
            char[] password = (char[]) userPass[1];
            setAuthenticated(authenticate(name, password));

            if (isAuthenticated()) {
                wasAuthenticated(name, password);
            }
        }
        return isAuthenticated();
    }

    protected void wasAuthenticated(String user, Object pass) {

        //store shared credentials if successful and not already stored
        if (isAuthenticated() && isStorePass() && !isHasSharedAuth()) {
            sharedState.put(SHARED_LOGIN_NAME, user);
            sharedState.put(SHARED_LOGIN_PASSWORD, pass);
        }
        wasAuthenticated();
    }

    @Override
    public boolean commit() throws LoginException {

        if (isClearPass() && isHasSharedAuth()) {
            sharedState.remove(SHARED_LOGIN_NAME);
            sharedState.remove(SHARED_LOGIN_PASSWORD);
        }
        return super.commit();
    }

    /**
     * Return the object[] containing username and password, by using the callback mechanism
     *
     * @return
     *
     * @throws IOException
     * @throws UnsupportedCallbackException
     * @throws LoginException
     */
    protected abstract Object[] getCallBackAuth() throws IOException, UnsupportedCallbackException, LoginException;

    /**
     * Return true if the authentication succeeds.
     *
     * @param sharedUserName
     * @param chars
     *
     * @return
     *
     * @throws LoginException
     */
    protected abstract boolean authenticate(String sharedUserName, char[] chars) throws LoginException;


    protected boolean isHasSharedAuth() {
        return null != sharedState.get(SHARED_LOGIN_NAME) && null != sharedState.get(SHARED_LOGIN_PASSWORD);
    }

    protected String getSharedUserName() {
        return sharedState.get(SHARED_LOGIN_NAME).toString();
    }

    protected Object getSharedUserPass() {
        return sharedState.get(SHARED_LOGIN_PASSWORD);
    }

    protected boolean isUseFirstPass() {
        return useFirstPass;
    }

    protected boolean isTryFirstPass() {
        return tryFirstPass;
    }

    protected boolean isStorePass() {
        return storePass;
    }

    protected boolean isClearPass() {
        return clearPass;
    }
}
