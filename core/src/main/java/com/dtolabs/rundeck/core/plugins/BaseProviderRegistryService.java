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

/*
* BaseRegistryService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 6:26 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * BaseProviderRegistryService is an abstract base that provides a registry of available service providers based on
 * simple names.  The service providers classes must have a no-arg constructor or a single-argument constructor with a {@link Framework}
 * argument
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @deprecated use {@link IFrameworkProviderRegistryService}
 */
@Deprecated
public abstract class BaseProviderRegistryService<T>
        extends AbstractProviderRegistryService<T>
        implements ProviderService<T>, ProviderRegistryService<T>
{
    protected final Framework                           framework;

    public BaseProviderRegistryService(final Framework framework) {
        this.framework = framework;
    }

    public BaseProviderRegistryService(final Framework framework, final boolean cacheInstances) {
        super(cacheInstances);
        this.framework = framework;
    }

    public BaseProviderRegistryService(
            final Framework framework,
            final Map<String, Class<? extends T>> classes
    )
    {
        super(classes);
        this.framework = framework;
    }

    public BaseProviderRegistryService(
            final Map<String, Class<? extends T>> registry,
            final Framework framework,
            final boolean cacheInstances
            )
    {
        super(registry, cacheInstances);
        this.framework = framework;
    }


    protected T createProviderInstanceFromType(final Class<? extends T> execClass, final String providerName) throws
        ProviderCreationException {
        try {
            final Constructor<? extends T> method = execClass.getDeclaredConstructor(Framework.class);
            return method.newInstance(framework);
        } catch (NoSuchMethodException ignored) {

        } catch (Exception e) {
            throw new ProviderCreationException("Unable to create provider instance: " + e.getMessage(), e, getName(),
                providerName);
        }
        return super.createProviderInstanceFromType(execClass, providerName);
    }

    protected boolean hasValidProviderSignature(final Class<?> clazz) {

        try {
            final Constructor method = clazz.getDeclaredConstructor(Framework.class);
            return null != method;
        } catch (NoSuchMethodException ignored) {
        }
        return super.hasValidProviderSignature(clazz);
    }
}
