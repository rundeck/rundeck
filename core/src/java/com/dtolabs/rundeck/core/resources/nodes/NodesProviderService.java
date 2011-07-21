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
* NodesProviderService.java
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
 * NodesProviderService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodesProviderService extends PluggableProviderRegistryService<NodesProviderFactory> {

    public static final String SERVICE_NAME = "NodesProvider";


    public NodesProviderService(final Framework framework) {
        super(framework);

//        registry.put("file", FileNodesProvider.class);
//        registry.put("url", URLNodesProvider.class);
        registry.put(FileNodesProviderFactory.SERVICE_PROVIDER_TYPE, FileNodesProviderFactory.class);
        registry.put(DirectoryNodesProviderFactory.SERVICE_PROVIDER_TYPE, DirectoryNodesProviderFactory.class);
    }

    public String getName() {
        return SERVICE_NAME;
    }


    public static NodesProviderService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final NodesProviderService service = new NodesProviderService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (NodesProviderService) framework.getService(SERVICE_NAME);
    }


    /**
     * Return a specific service provider that can be used for the node
     */
    public NodesProvider getProviderForConfiguration(final String type, final Properties configuration) throws
        ExecutionServiceException {

        //try to acquire supplier from registry
        final NodesProviderFactory nodesProvider = providerOfType(type);
        try {
            return nodesProvider.createNodesProvider(configuration);
        } catch (ConfigurationException e) {
            throw new NodesProviderServiceException(e);
        }
    }


    public boolean isValidProviderClass(Class clazz) {

        return NodesProvider.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    public NodesProviderFactory createProviderInstance(Class<NodesProviderFactory> clazz, String name) throws PluginException,
        ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    public boolean isScriptPluggable() {
        return false;
    }

    public NodesProviderFactory createScriptProviderInstance(ScriptPluginProvider provider) throws PluginException {
        //TODO
//        ScriptPluginNodeExecutor.validateScriptPlugin(provider);
//        return new ScriptPluginNodeExecutor(provider);
        return null;
    }
}
