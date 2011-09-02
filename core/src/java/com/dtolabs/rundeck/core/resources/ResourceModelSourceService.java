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
* ResourceModelSourceService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/19/11 10:53 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.PluggableProviderRegistryService;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * ResourceModelSourceService provides NodeSource factories
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ResourceModelSourceService extends PluggableProviderRegistryService<ResourceModelSourceFactory> implements
    ConfigurableService<ResourceModelSource>, DescribableService {

    public static final String SERVICE_NAME = "ResourceModelSource";


    public ResourceModelSourceService(final Framework framework) {
        super(framework);

        registry.put(FileResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, FileResourceModelSourceFactory.class);
        registry.put(DirectoryResourceModelSourceFactory.SERVICE_PROVIDER_TYPE,
            DirectoryResourceModelSourceFactory.class);
        registry.put(URLResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, URLResourceModelSourceFactory.class);
        registry.put(ScriptResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, ScriptResourceModelSourceFactory.class);
    }

    public String getName() {
        return SERVICE_NAME;
    }


    public static ResourceModelSourceService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final ResourceModelSourceService service = new ResourceModelSourceService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (ResourceModelSourceService) framework.getService(SERVICE_NAME);
    }


    /**
     * Return a ResourceModelSource of a give type with a given configuration
     */
    public ResourceModelSource getSourceForConfiguration(final String type, final Properties configuration) throws
        ExecutionServiceException {

        //try to acquire supplier from registry
        final ResourceModelSourceFactory nodesSourceFactory = providerOfType(type);
        try {
            return nodesSourceFactory.createResourceModelSource(configuration);
        } catch (ConfigurationException e) {
            throw new ResourceModelSourceServiceException(e);
        } catch (Throwable e){
            throw new ResourceModelSourceServiceException(e);
        }
    }

    public boolean isValidProviderClass(Class clazz) {

        return ResourceModelSourceFactory.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    public ResourceModelSourceFactory createProviderInstance(Class<ResourceModelSourceFactory> clazz,
                                                             String name) throws PluginException,
        ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    public boolean isScriptPluggable() {
        return true;
    }

    public ResourceModelSourceFactory createScriptProviderInstance(final ScriptPluginProvider provider) throws
        PluginException {
        ScriptPluginResourceModelSourceFactory.validateScriptPlugin(provider);
        return new ScriptPluginResourceModelSourceFactory(provider,framework);
    }

    public ResourceModelSource getProviderForConfiguration(final String type,
                                                           final Properties configuration) throws
        ExecutionServiceException {
        return getSourceForConfiguration(type, configuration);
    }

    public List<Description> listDescriptions() {
        final ArrayList<Description> list = new ArrayList<Description>();
        for (final ProviderIdent providerIdent : listProviders()) {
            try {
                final ResourceModelSourceFactory providerForType = providerOfType(providerIdent.getProviderName());
                if (providerForType instanceof Describable) {
                    Describable desc = (Describable) providerForType;
                    list.add(desc.getDescription());
                }
            } catch (ExecutionServiceException e) {
                e.printStackTrace();
            }

        }
        return list;
    }

    public List<ProviderIdent> listDescribableProviders() {
        final ArrayList<ProviderIdent> list = new ArrayList<ProviderIdent>();
        for (final ProviderIdent providerIdent : listProviders()) {
            try {
                final ResourceModelSourceFactory providerForType = providerOfType(providerIdent.getProviderName());
                if (providerForType instanceof Describable) {
                    list.add(providerIdent);
                }
            } catch (ExecutionServiceException e) {
                e.printStackTrace();
            }

        }
        return list;
    }
}
