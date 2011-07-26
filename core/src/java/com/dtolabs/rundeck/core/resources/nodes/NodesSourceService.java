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
* NodesSourceService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/19/11 10:53 AM
* 
*/
package com.dtolabs.rundeck.core.resources.nodes;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.PluggableProviderRegistryService;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;

import java.util.Properties;

/**
 * NodesSourceService provides NodeSource factories
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodesSourceService extends PluggableProviderRegistryService<NodesSourceFactory> {

    public static final String SERVICE_NAME = "NodesSource";


    public NodesSourceService(final Framework framework) {
        super(framework);

        registry.put(FileNodesSourceFactory.SERVICE_PROVIDER_TYPE, FileNodesSourceFactory.class);
        registry.put(DirectoryNodesSourceFactory.SERVICE_PROVIDER_TYPE, DirectoryNodesSourceFactory.class);
        registry.put(URLNodesSourceFactory.SERVICE_PROVIDER_TYPE, URLNodesSourceFactory.class);
    }

    public String getName() {
        return SERVICE_NAME;
    }


    public static NodesSourceService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final NodesSourceService service = new NodesSourceService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (NodesSourceService) framework.getService(SERVICE_NAME);
    }


    /**
     * Return a NodesSource of a give type with a given configuration
     */
    public NodesSource getSourceForConfiguration(final String type, final Properties configuration) throws
        ExecutionServiceException {

        //try to acquire supplier from registry
        final NodesSourceFactory nodesSourceFactory = providerOfType(type);
        try {
            return nodesSourceFactory.createNodesSource(configuration);
        } catch (ConfigurationException e) {
            throw new NodesSourceServiceException(e);
        }
    }


    public boolean isValidProviderClass(Class clazz) {

        return NodesSource.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    public NodesSourceFactory createProviderInstance(Class<NodesSourceFactory> clazz, String name) throws PluginException,
        ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    public boolean isScriptPluggable() {
        return false;
    }

    public NodesSourceFactory createScriptProviderInstance(ScriptPluginProvider provider) throws PluginException {
        //TODO
//        ScriptPluginNodeExecutor.validateScriptPlugin(provider);
//        return new ScriptPluginNodeExecutor(provider);
        return null;
    }
}
