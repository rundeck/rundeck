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
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Map;

/**
 * Abstract login module supporting the `useFirstPass`,`tryFirstPass`,`storePass` and `clearPass` options.
 */
public abstract class AbstractSharedLoginModule extends AbstractBaseLoginModule {
    private Map<String, ?> sharedState;
    private SharedLoginCreds sharedLoginCreds;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        this.sharedState=sharedState;
        this.sharedLoginCreds = new SharedLoginCreds(sharedState, options);
    }

    @Override
    public boolean login() throws LoginException {
        if ((getSharedLoginCreds().isUseFirstPass() || getSharedLoginCreds().isTryFirstPass()) && getSharedLoginCreds().isHasSharedAuth()) {
            debug(String.format("AbstractSharedLoginModule: login with sharedLoginState auth, " +
                    "try? %s, use? %s", getSharedLoginCreds().isTryFirstPass(), getSharedLoginCreds().isUseFirstPass()));
            setAuthenticated(authenticate(getSharedLoginCreds().getSharedUserName(), getSharedLoginCreds().getSharedUserPass().toString().toCharArray()));
        }

        if (getSharedLoginCreds().isUseFirstPass() && getSharedLoginCreds().isHasSharedAuth()) {
            //finish with sharedLoginState password auth attempt
            debug(String.format("AbstractSharedLoginModule: using login result: %s", isAuthenticated()));
            if (isAuthenticated()) {
                wasAuthenticated(getSharedLoginCreds().getSharedUserName(), getSharedLoginCreds().getSharedUserPass());
            }
            return isAuthenticated();
        }
        if (getSharedLoginCreds().isHasSharedAuth()) {
            debug(String.format("AbstractSharedLoginModule: sharedLoginState auth failed, now trying callback auth."));
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

        //store sharedLoginState credentials if successful and not already stored
        if (isAuthenticated() && getSharedLoginCreds().isStorePass() && !getSharedLoginCreds().isHasSharedAuth()) {
            getSharedLoginCreds().storeLoginCreds(user, pass);
        }
        wasAuthenticated();
    }

    @Override
    public boolean commit() throws LoginException {

        if (getSharedLoginCreds().isClearPass() && getSharedLoginCreds().isHasSharedAuth()) {
            getSharedLoginCreds().clear();
        }
        return super.commit();
    }

    /**
     *
     * @return Return the object[] containing username and password, by using the callback mechanism
     *
     * @throws IOException
     * @throws UnsupportedCallbackException
     * @throws LoginException
     */
    protected abstract Object[] getCallBackAuth() throws IOException, UnsupportedCallbackException, LoginException;

    /**
     * @param sharedUserName user
     * @param chars password
     *
     * @return true if the authentication succeeds
     *
     * @throws LoginException
     */
    protected abstract boolean authenticate(String sharedUserName, char[] chars) throws LoginException;

    protected SharedLoginCreds getSharedLoginCreds() {
        return sharedLoginCreds;
    }
}
