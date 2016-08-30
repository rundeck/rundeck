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
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin;
import com.dtolabs.rundeck.plugins.storage.StoragePlugin;

/**
 * Created by greg on 8/26/16.
 */
public class UIPluginProviderService extends FrameworkPluggableProviderService<UIPlugin> {
    public static final String SERVICE_NAME = "UI";
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public UIPluginProviderService(final Framework framework) {
        super(SERVICE_NAME, framework, UIPlugin.class);
    }

    @Override
    public ServiceProviderLoader getPluginManager() {
        return rundeckServerServiceProviderLoader;
    }

    public ServiceProviderLoader getRundeckServerServiceProviderLoader() {
        return rundeckServerServiceProviderLoader;
    }

    public void setRundeckServerServiceProviderLoader(ServiceProviderLoader rundeckServerServiceProviderLoader) {
        this.rundeckServerServiceProviderLoader = rundeckServerServiceProviderLoader;
    }

    @Override
    public boolean isScriptPluggable() {
        return true;
    }

    @Override
    public UIPlugin createScriptProviderInstance(ScriptPluginProvider provider) throws
            PluginException
    {
        ScriptUIPlugin.validateScriptPlugin(provider);
        return new ScriptUIPlugin(provider, getFramework());
    }
}
