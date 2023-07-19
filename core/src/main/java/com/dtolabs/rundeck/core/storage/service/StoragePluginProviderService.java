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

package com.dtolabs.rundeck.core.storage.service;

import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.storage.StoragePlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StoragePluginProviderService is composed of builtin services and
 * {@link PluggableStoragePluginProviderService}
 *
 * @author greg
 * @since 2014-02-21
 */
public class StoragePluginProviderService extends ChainedProviderService<StoragePlugin> implements
        DescribableService, PluggableProviderService<StoragePlugin> {
    public static final String SERVICE_NAME = ServiceNameConstants.Storage;

    private final List<ProviderService<StoragePlugin>> serviceList;
    private PluggableStoragePluginProviderService pluggableStoragePluginProviderService;

    private Map<String, Class<? extends StoragePlugin>> builtinProviders = new HashMap<>();

    public StoragePluginProviderService() {
        serviceList = new ArrayList<>();
    }

    public StoragePluginProviderService(Map<String, Class<? extends StoragePlugin>> builtinProviders) {
        serviceList = new ArrayList<>();
        this.builtinProviders = builtinProviders;
        final ProviderRegistryService<StoragePlugin>
                builtinResourceStoragePluginProviderService =
                ServiceFactory.builtinService(SERVICE_NAME, builtinProviders);
        serviceList.add(builtinResourceStoragePluginProviderService);
    }

    @Override
    public boolean canLoadWithLoader(final ProviderLoader loader) {
        return pluggableStoragePluginProviderService.canLoadWithLoader(loader);
    }

    @Override
    public StoragePlugin loadWithLoader(final String providerName, final ProviderLoader loader)
        throws ProviderLoaderException
    {
        return pluggableStoragePluginProviderService.loadWithLoader(providerName, loader);
    }

    @Override
    public CloseableProvider<StoragePlugin> loadCloseableWithLoader(
        final String providerName, final ProviderLoader loader
    ) throws ProviderLoaderException
    {
        return pluggableStoragePluginProviderService.loadCloseableWithLoader(providerName, loader);
    }

    public List<String> getBundledProviderNames() {
        return new ArrayList<>(builtinProviders.keySet());
    }

    @Override
    protected List<ProviderService<StoragePlugin>> getServiceList() {
        return serviceList;
    }

    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    public PluggableStoragePluginProviderService getPluggableStoragePluginProviderService() {
        return pluggableStoragePluginProviderService;
    }

    public void setPluggableStoragePluginProviderService(PluggableStoragePluginProviderService
            pluggableStoragePluginProviderService) {
        this.pluggableStoragePluginProviderService = pluggableStoragePluginProviderService;
        serviceList.add(this.pluggableStoragePluginProviderService);
    }
}
