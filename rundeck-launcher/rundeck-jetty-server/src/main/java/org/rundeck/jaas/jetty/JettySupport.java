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

import org.eclipse.jetty.plus.jaas.JAASPrincipal;
import org.eclipse.jetty.plus.jaas.JAASRole;
import org.eclipse.jetty.plus.jaas.callback.ObjectCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;

/**
 * $INTERFACE is ... User: greg Date: 8/16/13 Time: 10:26 AM
 */
public class JettySupport {
    private static Callback[] createCallbacks() {
        Callback[] calls = new Callback[2];
        calls[0] = new NameCallback("Username: ");
        calls[1] = new ObjectCallback();
        return calls;
    }

    public static Object[] performCallbacks(CallbackHandler handler) throws IOException,
            UnsupportedCallbackException, LoginException {
        if (handler == null) {
            throw new LoginException("No callback handler");
        }
        Callback[] callbacks = createCallbacks();
        handler.handle(callbacks);
        String name = ((NameCallback) callbacks[0]).getName();
        Object creds = ((ObjectCallback) callbacks[1]).getObject();
        return new Object[]{name, getPassword(creds)};
    }

    private static char[] getPassword(Object object) {
        if (object instanceof String) {
            return object.toString().toCharArray();
        } else if (object instanceof char[]) {
            return (char[]) object;
        }
        return null;
    }

    public static Principal createUserPrincipal(String username) {
        return new JAASPrincipal(username);
    }
    public static Principal createRolePrincipal(String rolename) {
        return new JAASRole(rolename);
    }
}
