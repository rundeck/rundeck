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

import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * abstract base that provides a registry of available service providers based on simple names.  The service providers
 * classes must have a no-arg constructor or a single-argument constructor with a {@link IFramework} argument, if the
 * registry service is created with a null IFramework, then loaded classes must have no-arg constructors.
 *
 * @param <T> plugin service type
 */
public abstract class IFrameworkProviderRegistryService<T>
        extends AbstractProviderRegistryService<T>
        implements ProviderService<T>, ProviderRegistryService<T>
{
    protected final IFramework framework;
    @Deprecated
    public IFrameworkProviderRegistryService() {
        this((IFramework) null);
    }

    public IFrameworkProviderRegistryService(IFramework framework) {
        super();
        this.framework = framework;
    }
    @Deprecated
    public IFrameworkProviderRegistryService(final IFramework framework, final boolean cacheInstances) {
        super(cacheInstances);
        this.framework = framework;
    }

    public IFrameworkProviderRegistryService(IFramework framework, Map<String, Class<? extends T>> classes) {
        super(classes);
        this.framework = framework;
    }

    @Deprecated
    public IFrameworkProviderRegistryService(
            final Map<String, Class<? extends T>> registry,
            final IFramework framework,
            final boolean cacheInstances
    )
    {
        super(registry, cacheInstances);
        this.framework = framework;
    }

    @Override
    public CloseableProvider<T> closeableProviderOfType(final String providerName) throws ExecutionServiceException {
        final T t = providerOfType(providerName);
        if (t == null) {
            return null;
        }
        return Closeables.closeableProvider(t);
    }

    protected T createProviderInstanceFromType(final Class<? extends T> execClass, final String providerName)
            throws ProviderCreationException
    {
        if (null != framework) {
            try {
                final Constructor<? extends T> method = execClass.getDeclaredConstructor(IFramework.class);
                return method.newInstance(framework);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new ProviderCreationException(
                        "Unable to create provider instance: " + e.getMessage(),
                        e,
                        getName(),
                        providerName
                );
            }
        }
        return super.createProviderInstanceFromType(execClass, providerName);
    }

    protected boolean hasValidProviderSignature(final Class<?> clazz) {

        if (null != framework) {
            try {
                final Constructor method = clazz.getDeclaredConstructor(IFramework.class);
                return null != method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return super.hasValidProviderSignature(clazz);
    }
}
