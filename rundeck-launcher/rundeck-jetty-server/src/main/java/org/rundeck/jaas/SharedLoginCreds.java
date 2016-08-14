/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package org.rundeck.jaas;

import java.util.Map;

/**
 * Manage shared login state
 */
public class SharedLoginCreds {

    public static final String SHARED_LOGIN_NAME = "javax.security.auth.login.name";
    public static final String SHARED_LOGIN_PASSWORD = "javax.security.auth.login.password";
    private boolean useFirstPass;
    private boolean tryFirstPass;
    private boolean storePass;
    private boolean clearPass;
    private Map<String, Object> sharedState;

    public SharedLoginCreds(
            final boolean useFirstPass,
            final boolean tryFirstPass,
            final boolean storePass,
            final boolean clearPass,
            final Map<String, Object> sharedState
    )
    {
        this.useFirstPass = useFirstPass;
        this.tryFirstPass = tryFirstPass;
        this.storePass = storePass;
        this.clearPass = clearPass;
        this.sharedState = sharedState;
    }

    public SharedLoginCreds(final Map sharedState, Map options) {
        this.sharedState = sharedState;
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

    public void storeLoginCreds(final String user, final Object pass) {
        sharedState.put(SHARED_LOGIN_PASSWORD, pass);
        sharedState.put(SHARED_LOGIN_NAME, user);
    }

    public void clear() {
        clearUser();
        clearPassword();
    }

    private void clearPassword() {
        sharedState.remove(SHARED_LOGIN_PASSWORD);
    }

    private void clearUser() {
        sharedState.remove(SHARED_LOGIN_NAME);
    }

    public boolean isHasSharedAuth() {
        return null != sharedState.get(SHARED_LOGIN_NAME) && null != sharedState.get(SHARED_LOGIN_PASSWORD);
    }

    public String getSharedUserName() {
        return sharedState.get(SHARED_LOGIN_NAME).toString();
    }

    public Object getSharedUserPass() {
        return sharedState.get(SHARED_LOGIN_PASSWORD);
    }

    public boolean isUseFirstPass() {
        return useFirstPass;
    }

    public boolean isTryFirstPass() {
        return tryFirstPass;
    }

    public boolean isStorePass() {
        return storePass;
    }

    public boolean isClearPass() {
        return clearPass;
    }
}
