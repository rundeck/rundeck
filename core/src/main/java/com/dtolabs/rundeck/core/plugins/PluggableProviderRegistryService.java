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
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.MissingProviderException;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends BaseProviderRegistryService to support loading providers via plugins if not found in the registry.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class PluggableProviderRegistryService<T> extends BaseProviderRegistryService<T> implements
    PluggableService<T> {
    protected PluggableProviderRegistryService(final Framework framework) {
        super(framework);
    }


    @Override
    public T providerOfType(final String providerName) throws ExecutionServiceException {
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

    @Override
    public List<ProviderIdent> listProviders() {
        final ArrayList<ProviderIdent> providerIdents = new ArrayList<ProviderIdent>(super.listProviders());

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
}
