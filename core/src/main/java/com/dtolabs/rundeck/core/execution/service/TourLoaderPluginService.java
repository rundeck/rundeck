/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin;

import java.util.List;

public class TourLoaderPluginService extends PluggableProviderRegistryService<TourLoaderPlugin> implements PluggableService<TourLoaderPlugin>,
                                                                                                           PluggableProviderService<TourLoaderPlugin>,
                                                                                                           JavaClassProviderLoadable<TourLoaderPlugin>,
                                                                                                           ScriptPluginProviderLoadable<TourLoaderPlugin> {

    private static final String SERVICE_NAME = ServiceNameConstants.TourLoader;

    protected TourLoaderPluginService(final Framework framework) {
        super(framework);
    }

    public TourLoaderPluginService(final Framework framework, final boolean cacheInstances) {
        super(framework, cacheInstances);
    }

    public static TourLoaderPluginService getInstanceForFramework(Framework framework) {
        if(null == framework.getService(SERVICE_NAME)) {
            final TourLoaderPluginService service = new TourLoaderPluginService(framework);
            framework.setService(SERVICE_NAME,service);
            return service;
        }
        return (TourLoaderPluginService)framework.getService(SERVICE_NAME);
    }

    @Override
    public boolean isValidProviderClass(final Class clazz) {
        return TourLoaderPlugin.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    @Override
    public <X extends TourLoaderPlugin> TourLoaderPlugin createProviderInstance(final Class<X> clazz, final String name)
            throws PluginException, ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    @Override
    public TourLoaderPlugin createScriptProviderInstance(final ScriptPluginProvider provider) throws PluginException {
        return new ScriptTourLoader(provider,framework);
    }

    @Override
    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }

    @Override
    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this, true);
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }
}
