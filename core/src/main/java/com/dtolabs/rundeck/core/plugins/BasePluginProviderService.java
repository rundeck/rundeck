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

package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;

import java.util.List;

/**
 * Created by greg on 9/9/15.
 */
public class BasePluginProviderService<T> extends BasePluggableProviderService<T> {
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public BasePluginProviderService(
            final String name,
            final Class<? extends T> implementationClass
    )
    {
        super(name, implementationClass);
    }


    @Override
    public ServiceProviderLoader getPluginManager() {
        return getRundeckServerServiceProviderLoader();
    }

    public ServiceProviderLoader getRundeckServerServiceProviderLoader() {
        return rundeckServerServiceProviderLoader;
    }

    public void setRundeckServerServiceProviderLoader(ServiceProviderLoader rundeckServerServiceProviderLoader) {
        this.rundeckServerServiceProviderLoader = rundeckServerServiceProviderLoader;
    }

    @Override
    public <X extends T> T
    createProviderInstance(Class<X> clazz, String name) throws PluginException, ProviderCreationException
    {
        return createProviderInstanceFromType(clazz, name);
    }

    public boolean isScriptPluggable() {
        return false;
    }

    @Override
    public T createScriptProviderInstance(final ScriptPluginProvider provider)
            throws PluginException
    {
        return null;
    }


    public List<Description> listDescriptions() {
        //TODO: enable field annotations for properties, update plugin Interface and deprecate use of Factory
        return DescribableServiceUtil.listDescriptions(this, false);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }

}
