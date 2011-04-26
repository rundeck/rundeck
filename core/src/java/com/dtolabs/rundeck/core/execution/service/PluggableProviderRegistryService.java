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
* PluggableProviderRegistryService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/14/11 3:53 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.PluggableService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;

/**
 * Overrides the {@link #providerOfType(String)} method to attempt to load a provider from the PluginManagerService.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class PluggableProviderRegistryService<T> extends BaseProviderRegistryService<T> implements
    PluggableService<T> {
    protected PluggableProviderRegistryService(final Framework framework) {
        super(framework);
    }


    @Override
    protected T providerOfType(final String providerName) throws ExecutionServiceException {
        T t = null;
        MissingProviderException caught = null;
        try {
            t = super.providerOfType(providerName);
        } catch (MissingProviderException e) {
            //ignore and attempt to load from the plugin manager
            caught = e;
        }
        if (null != t) {
            return t;
        }
        final ServiceProviderLoader pluginManager = framework.getPluginManager();
        if (null != pluginManager) {
            return pluginManager.loadProvider(this, providerName);
        } else if (null != caught) {
            throw caught;
        }else {
            throw new MissingProviderException("Provider not found", getName(), providerName);
        }
    }
}
