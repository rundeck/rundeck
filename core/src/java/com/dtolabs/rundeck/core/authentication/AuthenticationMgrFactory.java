/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.authentication;


import com.dtolabs.rundeck.core.authorization.AuthorizationException;
import com.dtolabs.rundeck.core.common.Framework;
import org.apache.log4j.Category;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * AuthenticationMgrFactory
 */
public class AuthenticationMgrFactory {
    public static final Category logger = Category.getInstance(AuthenticationMgrFactory.class);

    private final Authenticator authenticator;

    private AuthenticationMgrFactory(final String classname, final Framework framework) {
        if (null==classname) throw new IllegalArgumentException("A null java class name was specified.");
        authenticator = createAuthenticationMgr(classname,framework);
    }

    public static AuthenticationMgrFactory create(final String classname, final Framework framework) {
        return new AuthenticationMgrFactory(classname,framework);
    }


    public Authenticator getAuthenticationMgr() {
        return authenticator;
    }

    private Authenticator createAuthenticationMgr(final String classname, final Framework framework) {
        if (null == classname) {
            throw new IllegalArgumentException("A null java class name was specified.");
        }
        logger.debug("using authentication class: " + classname);
        final Authenticator auth;
        try {
            final Class cls = Class.forName(classname);
            final Method method = cls.getDeclaredMethod("getAuthenticator", new Class[]{Framework.class});
            auth = (Authenticator) method.invoke(null, new Object[]{framework});
        } catch (ClassNotFoundException e) {
            throw new AuthorizationException("error instantiating authorization class: " + classname, e);
        } catch (IllegalAccessException e) {
            throw new AuthorizationException("error instantiating authorization class: " + classname, e);
        } catch (NoSuchMethodException e) {
            throw new AuthorizationException("error instantiating authorization class: " + classname, e);
        } catch (InvocationTargetException e) {
            throw new AuthorizationException("error instantiating authorization class: " + classname, e);
        }

        return auth;

    }
}
