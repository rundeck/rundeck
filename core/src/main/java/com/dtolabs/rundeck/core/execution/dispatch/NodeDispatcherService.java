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
import com.dtolabs.rundeck.core.plugins.BaseProviderRegistryService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;


/**
 * NodeProcessorService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeDispatcherService extends BaseProviderRegistryService<NodeDispatcher>{

    private static final String SERVICE_NAME = ServiceNameConstants.NodeDispatcher;

    public NodeDispatcherService(Framework framework) {
        super(framework);
        registry.put("parallel", ParallelNodeDispatcher.class);
        registry.put("sequential", SequentialNodeDispatcher.class);
        registry.put("orchestrator", OrchestratorNodeDispatcher.class);
    }

    public  NodeDispatcher getNodeDispatcher(ExecutionContext context) throws ExecutionServiceException {
        //this gets called for each node as well if we already have data then the parent orchestrator has fired
        if(context.getOrchestrator() != null && 
                !context.getDataContext().containsKey(OrchestratorNodeDispatcher.ORCHESTRATOR_DATA)){
            return providerOfType("orchestrator");
        }
        if (context.getThreadCount() > 1 && context.getNodes().getNodeNames().size() > 1) {
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
