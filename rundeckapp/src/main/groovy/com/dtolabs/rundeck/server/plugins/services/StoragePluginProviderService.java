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

package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.storage.StoragePlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * StoragePluginProviderService is composed of the {@link BuiltinResourceStoragePluginProviderService} and
 * {@link PluggableStoragePluginProviderService}
 *
 * @author greg
 * @since 2014-02-21
 */
public class StoragePluginProviderService extends ChainedProviderService<StoragePlugin> implements
        DescribableService, PluggableProviderService<StoragePlugin> {
    public static final String SERVICE_NAME = ServiceNameConstants.Storage;

    private List<ProviderService<StoragePlugin>> serviceList;
    private PluggableStoragePluginProviderService pluggableStoragePluginProviderService;
    private BuiltinResourceStoragePluginProviderService builtinResourceStoragePluginProviderService;

    public StoragePluginProviderService(Framework framework) {
        serviceList = new ArrayList<ProviderService<StoragePlugin>>();
        builtinResourceStoragePluginProviderService =
                new BuiltinResourceStoragePluginProviderService(framework, SERVICE_NAME);
        serviceList.add(builtinResourceStoragePluginProviderService);
    }

    public List<String> getBundledProviderNames() {
        return builtinResourceStoragePluginProviderService.getBundledProviderNames();
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

    @Override
    public boolean isValidProviderClass(Class aClass) {
        return getPluggableStoragePluginProviderService().isValidProviderClass(aClass);
    }

    @Override
    public <X extends StoragePlugin> StoragePlugin createProviderInstance(Class<X> clazz, String name) throws PluginException, ProviderCreationException {
        return getPluggableStoragePluginProviderService().createProviderInstance(clazz, name);
    }

    @Override
    public boolean isScriptPluggable() {
        return getPluggableStoragePluginProviderService().isScriptPluggable();
    }

    @Override
    public StoragePlugin createScriptProviderInstance(ScriptPluginProvider scriptPluginProvider) throws
            PluginException {
        return null;
    }
}
