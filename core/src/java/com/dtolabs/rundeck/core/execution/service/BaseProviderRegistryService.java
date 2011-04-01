/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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

/*
* BaseRegistryService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 6:26 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.plugins.PluggableService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * BaseProviderRegistryService is an abstract base that provides a registry of available service providers based on
 * simple names.  The service providers classes must have a simple single-argument constructor with a {@link Framework}
 * argument
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class BaseProviderRegistryService<T> implements FrameworkSupportService {
    protected HashMap<String, Class<? extends T>> registry;
    protected HashMap<String, T> instanceregistry;
    protected final Framework framework;

    public BaseProviderRegistryService(Framework framework) {
        this.framework = framework;
        instanceregistry = new HashMap<String, T>();
        registry = new HashMap<String, Class<? extends T>>();
    }

    public void registerClass(String name, Class<? extends T> clazz) {
        registry.put(name, clazz);
    }

    public void registerInstance(String name, T object) {
        instanceregistry.put(name, object);
    }


    protected T providerOfType(final String providerName) throws ExecutionServiceException {
        if (null == providerName) {
            throw new MissingProviderException("provider name was null", getName(), providerName);
        }
        if (null == instanceregistry.get(providerName)) {
            T instance = createProviderInstanceOfType(providerName);
            instanceregistry.put(providerName, instance);
            return instance;
        }
        return instanceregistry.get(providerName);
    }

    private T createProviderInstanceOfType(final String providerName) throws ExecutionServiceException {
        if (null == registry.get(providerName)) {
            throw new MissingProviderException("No provider with the specified name is registered.", getName(),
                providerName);
        }
        final Class<? extends T> execClass = registry.get(providerName);
        boolean ctrfound = true;
        try {
            final Constructor<? extends T> method = execClass.getDeclaredConstructor(new Class[]{Framework.class});
            final T executor = method.newInstance(framework);
            return executor;
        } catch (NoSuchMethodException e) {
            ctrfound = false;
        } catch (Exception e) {
            throw new ProviderCreationException("Unable to create provider instance: " + e.getMessage(), e, getName(),
                providerName);
        }
        try {
            final Constructor<? extends T> method = execClass.getDeclaredConstructor(new Class[0]);
            final T executor = method.newInstance();
            return executor;
        } catch (NoSuchMethodException e) {
            throw new ProviderCreationException(
                "No constructor found with signature (Framework) or (): " + e.getMessage(), e,
                getName(),
                providerName);
        } catch (Exception e) {
            throw new ProviderCreationException("Unable to create provider instance: " + e.getMessage(), e,
                getName(),
                providerName);
        }
    }

    protected boolean hasValidProviderSignature(final Class clazz) {

        try {
            final Constructor method = clazz.getDeclaredConstructor(new Class[]{Framework.class});
            return null != method;
        } catch (NoSuchMethodException e) {
        }
        try {
            final Constructor method = clazz.getDeclaredConstructor(new Class[0]);
            return null != method;
        } catch (NoSuchMethodException e) {
        }
        return false;
    }
}
