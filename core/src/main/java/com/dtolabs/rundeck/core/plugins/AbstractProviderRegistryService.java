/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.MissingProviderException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Common base for registry based services providers
 * @param <T>
 */
public abstract class AbstractProviderRegistryService<T>
        implements ProviderRegistryService<T>, ProviderService<T>
{
    protected HashMap<String, Class<? extends T>> registry;
    protected HashMap<String, T> instanceregistry;
    @Getter @Setter private boolean cacheInstances = false;

    public AbstractProviderRegistryService() {
        instanceregistry = new HashMap<>();
        registry = new HashMap<>();
    }

    public AbstractProviderRegistryService(final boolean cacheInstances) {
        this();
        this.cacheInstances = cacheInstances;
    }

    public AbstractProviderRegistryService(Map<String, Class<? extends T>> classes) {
        this();
        instanceregistry = new HashMap<>();
        registry = new HashMap<>(classes);

    }

    public AbstractProviderRegistryService(
            final Map<String, Class<? extends T>> registry,
            final boolean cacheInstances
    )
    {
        this();
        instanceregistry = new HashMap<>();
        this.registry = new HashMap<>(registry);
        this.cacheInstances = cacheInstances;
    }

    @Override
    public void registerClass(String name, Class<? extends T> clazz) {
        registry.put(name, clazz);
    }

    @Override
    public boolean isRegistered(final String name) {
        return instanceregistry.containsKey(name) || registry.containsKey(name);
    }

    @Override
    public void registerInstance(String name, T object) {
        instanceregistry.put(name, object);
    }

    /**
     * Return the provider instance of the given name.
     */
    public T providerOfType(final String providerName) throws ExecutionServiceException {
        if (null == providerName) {
            throw new NullPointerException("provider name was null for Service: " + getName());
        }
        if (isCacheInstances()) {
            if (null == instanceregistry.get(providerName)) {
                T instance = createProviderInstanceOfType(providerName);
                instanceregistry.put(providerName, instance);
                return instance;
            }
            return instanceregistry.get(providerName);
        } else {
            return createProviderInstanceOfType(providerName);
        }
    }

    @Override
    public CloseableProvider<T> closeableProviderOfType(final String providerName) throws ExecutionServiceException {
        final T t = providerOfType(providerName);
        if (t == null) {
            return null;
        }
        return Closeables.closeableProvider(t);
    }


    private T createProviderInstanceOfType(final String providerName) throws ExecutionServiceException {
        if (null == registry.get(providerName)) {
            throw new MissingProviderException("Not found", getName(),
                                               providerName
            );
        }
        final Class<? extends T> execClass = registry.get(providerName);
        return createProviderInstanceFromType(execClass, providerName);
    }

    public List<ProviderIdent> listProviders() {

        final HashSet<ProviderIdent> providers = new HashSet<>();

        for (final String s : registry.keySet()) {
            providers.add(new ProviderIdent(getName(), s));
        }
        for (final String s : instanceregistry.keySet()) {
            providers.add(new ProviderIdent(getName(), s));
        }
        return new ArrayList<>(providers);
    }

    protected T createProviderInstanceFromType(final Class<? extends T> execClass, final String providerName)
            throws ProviderCreationException
    {
        try {
            final Constructor<? extends T> method = execClass.getDeclaredConstructor();
            return method.newInstance();
        } catch (NoSuchMethodException e) {
            throw new ProviderCreationException(
                    "No constructor found with signature (): " + e.getMessage(), e,
                    getName(),
                    providerName
            );
        } catch (Exception e) {
            throw new ProviderCreationException("Unable to create provider instance: " + e.getMessage(), e,
                                                getName(),
                                                providerName
            );
        }
    }

    protected boolean hasValidProviderSignature(final Class<?> clazz) {

        try {
            final Constructor method = clazz.getDeclaredConstructor();
            return null != method;
        } catch (NoSuchMethodException ignored) {
        }
        return false;
    }
}
