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

package com.dtolabs.rundeck.core.dispatcher;


import com.dtolabs.rundeck.core.common.Framework;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;

/**
 * CentralDispatcherMgrFactory provides methods for creating a CentralDispatcher instance, configured by a classname.
 */
public class CentralDispatcherMgrFactory {
    /**
     * logger
     */
    public static final Logger logger = Logger.getLogger(CentralDispatcherMgrFactory.class);

    private final CentralDispatcher centralDispatcher;

    /**
     * private constructor
     *
     * @param classname class name
     * @param framework framework instance
     *
     * @throws CentralDispatcherException if an error occurs initializing a CentralDispatcher instance.
     */
    private CentralDispatcherMgrFactory(final String classname, final Framework framework) throws
        CentralDispatcherException {
        if (null == classname) {
            throw new IllegalArgumentException("A null java class name was specified.");
        }
        centralDispatcher = createCentralDispatcher(classname, framework);
    }

    /**
     * Factory method to create the factory instance.
     *
     * @param classname classname for the CentralDispatcher implementation
     * @param framework framework instance
     *
     * @return the factory
     *
     * @throws CentralDispatcherException if an error occurs
     */
    public static CentralDispatcherMgrFactory create(final String classname, final Framework framework) throws
        CentralDispatcherException {
        return new CentralDispatcherMgrFactory(classname, framework);
    }

    /**
     * Return the CentralDispatcher implementation from this factory
     *
     * @return the instance
     */
    public CentralDispatcher getCentralDispatcher() {
        return centralDispatcher;
    }

    /**
     * Create instance from classname
     *
     * @param classname class name
     * @param framework framework instance
     *
     * @return instance
     *
     * @throws CentralDispatcherException if an error occurs
     */
    private CentralDispatcher createCentralDispatcher(final String classname, final Framework framework) throws
        CentralDispatcherException {
        if (null == classname) {
            throw new IllegalArgumentException("A null java class name was specified.");
        }
        logger.debug("using centralDispatcher class: " + classname);
        final CentralDispatcher centralDispatcher;
        try {
            final Class cls = Class.forName(classname);
            final Constructor method = cls.getDeclaredConstructor(Framework.class);
            centralDispatcher = (CentralDispatcher) method.newInstance(framework);
        } catch (ClassNotFoundException e) {
            throw new CentralDispatcherException("error instantiating centralDispatcher class: " + classname, e);
        } catch (IllegalAccessException e) {
            throw new CentralDispatcherException("error instantiating centralDispatcher class: " + classname, e);
        } catch (NoSuchMethodException e) {
            throw new CentralDispatcherException("error instantiating centralDispatcher class: " + classname, e);
        } catch (InvocationTargetException e) {
            throw new CentralDispatcherException("error instantiating centralDispatcher class: " + classname, e);
        } catch (InstantiationException e) {
            throw new CentralDispatcherException("error instantiating centralDispatcher class: " + classname, e);
        }

        return centralDispatcher;

    }
}