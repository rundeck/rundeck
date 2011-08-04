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

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.common.Framework;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * AuthorizationMgrFactory
 */
public class AuthorizationMgrFactory {

    private final LegacyAuthorization authorization;

    private AuthorizationMgrFactory(final String classname, final Framework framework, final File baseDir) {
        authorization = createAuthorization(classname, framework, baseDir);
    }

    public static AuthorizationMgrFactory create(final String classname, final Framework framework, final File baseDir) {
        return new AuthorizationMgrFactory(classname, framework, baseDir);
    }

    public LegacyAuthorization getAuthorizationMgr() {
        return authorization;
    }

    /**
     * Loads class that manages authorization.
     * The constructor signature is: {@link Framework}, {@link File}
     *
     * @param classname Name of class to instantiate. Should implement {@link Authorization}.
     * @param framework Framework instance
     * @param baseDir   Basedir containing Authorization config data
     * @return Instance of class that implements Authorization
     * @throws AuthorizationException if implementing class cannot be instantiated, doesn't support the
     *                                constructor signature, or is not found at all.
     */

    private LegacyAuthorization createAuthorization(final String classname, final Framework framework, final File baseDir)
            throws AuthorizationException {
        if (!baseDir.isDirectory()) {
            throw new IllegalArgumentException("baseDir does not exist or is not a directory: " + baseDir);
        }
        if (null == classname) {
            throw new IllegalArgumentException("classname was null");
        }

        final LegacyAuthorization authorizationImpl;
        try {
            final Class cls = Class.forName(classname);
            final Constructor method = cls.getDeclaredConstructor(new Class[]{Framework.class, File.class});
            authorizationImpl = (LegacyAuthorization) method.newInstance(framework, baseDir);
        } catch (InstantiationException e) {
            throw new AuthorizationException("error instantiating authorization class: " + classname, e);
        } catch (IllegalAccessException e) {
            throw new AuthorizationException("error instantiating authorization class: " + classname, e);
        } catch (NoSuchMethodException e) {
            throw new AuthorizationException("error instantiating authorization class: " + classname, e);
        } catch (InvocationTargetException e) {
            throw new AuthorizationException("error instantiating authorization class: " + classname, e);
        } catch (ClassNotFoundException e) {
            throw new AuthorizationException("error instantiating authorization class: " + classname, e);
        }

        return authorizationImpl;
    }
}
