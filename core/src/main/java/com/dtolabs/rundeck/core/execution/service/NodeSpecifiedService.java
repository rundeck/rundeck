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
* NodeSpecifiedService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 10:07 AM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.plugins.PluggableProviderRegistryService;
import com.dtolabs.rundeck.core.plugins.PluggableService;

/**
 * NodeSpecifiedService uses node metadata to select service provider implementation.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class NodeSpecifiedService<T> extends PluggableProviderRegistryService<T> implements PluggableService<T> {
    protected NodeSpecifiedService(final Framework framework) {
        super(framework);
    }

    /**
     * @return a specific service provider that can be used for the node
     * @param node node
     * @param project project
     *                @throws ExecutionServiceException on error
     */
    public T getProviderForNodeAndProject(final INodeEntry node, final String project) throws
        ExecutionServiceException {
        String copiername = getDefaultProviderNameForNodeAndProject(node, project);
        //look up node's attribute if it exists
        if (null != node.getAttributes() && null != node.getAttributes().get(getServiceProviderNodeAttributeForNode(
            node))) {
            copiername = node.getAttributes().get(getServiceProviderNodeAttributeForNode(node));
        }
        //try to acquire supplier from registry
        return providerOfType(copiername);
    }

    /**
     * @return name of Node attribute that specifies the service provider name for this service.
     * @param node node
     */
    protected abstract String getServiceProviderNodeAttributeForNode(INodeEntry node);

    /**
     * @return name of default provider for this service
     * @param node node
     * @param project project
     */
    protected abstract String getDefaultProviderNameForNodeAndProject(INodeEntry node, String project);

}
