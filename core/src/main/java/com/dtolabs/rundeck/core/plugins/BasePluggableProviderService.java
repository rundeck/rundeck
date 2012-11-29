/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* BasePluggableProviderService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/12/12 5:02 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.MissingProviderException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.utils.Converter;

import java.lang.reflect.Constructor;
import java.util.*;


/**
 * BasePluggableProviderService is an abstract base for a provider service which can load providers from plugins.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class BasePluggableProviderService<T> implements ProviderService<T>, PluggableService<T> {
    private Framework framework;
    private Class<? extends T> implementationClass;
    private String name;

    protected BasePluggableProviderService(final String name,
                                           final Framework framework,
                                           final Class<? extends T> implementationClass) {
        this.name=name;
        this.framework = framework;
        this.implementationClass = implementationClass;
    }

    /*
     * Default implementation of isValidProviderClas, which checks the class is assignable from the specified
     * implementation class, and has a valid signature.
     */
    public boolean isValidProviderClass(final Class clazz) {
        return implementationClass.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    /*
     * default implementation of createProviderInstance
     */
    public T createProviderInstance(final Class<T> clazz, final String name)
        throws PluginException, ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    @Override
    public T providerOfType(final String providerName) throws ExecutionServiceException {
        final ServiceProviderLoader pluginManager = framework.getPluginManager();
        if (null != pluginManager) {
            return pluginManager.loadProvider(this, providerName);
        } else {
            throw new MissingProviderException("Provider not found", getName(), providerName);
        }
    }

    public List<ProviderIdent> listProviders() {
        final ArrayList<ProviderIdent> providerIdents = new ArrayList<ProviderIdent>();

        final ServiceProviderLoader pluginManager = framework.getPluginManager();
        if (null != pluginManager) {
            final List<ProviderIdent> providerIdents1 = pluginManager.listProviders();
            for (final ProviderIdent providerIdent : providerIdents1) {
                if (getName().equals(providerIdent.getService())) {
                    providerIdents.add(providerIdent);
                }
            }
        }
        return providerIdents;
    }

    /**
     * default implementation returns false, subclasses should override to
     */
    @Override
    public boolean isScriptPluggable() {
        return false;
    }

    @Override
    public T createScriptProviderInstance(ScriptPluginProvider provider)
        throws PluginException {
        throw new UnsupportedOperationException("This service does not support script plugins");
    }

    protected T createProviderInstanceFromType(final Class<? extends T> execClass, final String providerName) throws
                                                                                                              ProviderCreationException {
        boolean ctrfound = true;
        try {
            final Constructor<? extends T> method = execClass.getDeclaredConstructor(new Class[]{Framework.class});
            return method.newInstance(framework);
        } catch (NoSuchMethodException e) {
            ctrfound = false;
        } catch (Exception e) {
            throw new ProviderCreationException("Unable to create provider instance: " + e.getMessage(), e, getName(),
                                                providerName);
        }
        try {
            final Constructor<? extends T> method = execClass.getDeclaredConstructor(new Class[0]);
            return method.newInstance();
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

    /**
     * default implementation of listDescriptions that can be used if subclasses implement {@link
     * com.dtolabs.rundeck.core.plugins.configuration.DescribableService}
     */
    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this);
    }

    /**
     * default implementation of listDescribableProviders that can be used if subclasses implement {@link
     * com.dtolabs.rundeck.core.plugins.configuration.DescribableService}
     */
    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }

    protected Framework getFramework() {
        return framework;
    }

    /**
     * Create an adapted form of this service given a converter.
     */
    public <X> ProviderService<X> adapter(final Converter<T, X> converter) {
        return AdapterService.adaptFor(this, converter);
    }

    public String getName() {
        return name;
    }
}
