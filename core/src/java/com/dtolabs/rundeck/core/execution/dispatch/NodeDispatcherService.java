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
* NodeProcessorService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:06 PM
* 
*/
package com.dtolabs.rundeck.core.execution.dispatch;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.BaseProviderRegistryService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.resources.nodes.ConfigurationException;
import com.dtolabs.rundeck.core.resources.nodes.FileNodesProvider;
import com.dtolabs.rundeck.core.resources.nodes.NodesProviderException;

/**
 * NodeProcessorService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeDispatcherService extends BaseProviderRegistryService<NodeDispatcher>{

    private static final String SERVICE_NAME = "NodeDispatcher";

    public NodeDispatcherService(Framework framework) {
        super(framework);
        registry.put("parallel", ParallelNodeDispatcher.class);
        registry.put("sequential", SequentialNodeDispatcher.class);
    }

    public  NodeDispatcher getNodeDispatcher(ExecutionContext context) throws ExecutionServiceException {
        final NodesSelector nodeset = context.getNodeSelector();
        final FrameworkProject frameworkProject = framework.getFrameworkProjectMgr().getFrameworkProject(
            context.getFrameworkProject());
        INodeSet filtered=null;
        try {
            INodeSet unfiltered;
            if(null!= context.getNodesFile()) {
                unfiltered = FileNodesProvider.parseFile(context.getNodesFile(), framework,
                    context.getFrameworkProject());
            }else{
                unfiltered=frameworkProject.getNodeSet();
            }
            filtered = NodeFilter.filterNodes(nodeset, unfiltered);
        } catch (NodeFileParserException e) {
            throw new ExecutionServiceException(e, getName());
        } catch (NodesProviderException e) {
            throw new ExecutionServiceException(e, getName());
        } catch (ConfigurationException e) {
            throw new ExecutionServiceException(e, getName());
        }

        if (context.getThreadCount() > 1 && filtered.getNodeNames().size() > 1) {
            return providerOfType("parallel");
        }else{
            return providerOfType("sequential");
        }
    }

    public static NodeDispatcherService getInstanceForFramework(Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final NodeDispatcherService service = new NodeDispatcherService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (NodeDispatcherService) framework.getService(SERVICE_NAME);
    }

    public String getName() {
        return SERVICE_NAME;
    }
}
