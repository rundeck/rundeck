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

/*
* WorkflowExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/28/11 4:46 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkflowExecutionService provides ability to execute workflows
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class WorkflowExecutionService extends ChainedProviderService<WorkflowExecutor> implements DescribableService {
    private static final String SERVICE_NAME = ServiceNameConstants.WorkflowExecution;

    private List<ProviderService<WorkflowExecutor>> serviceList;
    private final Map<String, Class<? extends WorkflowExecutor>> registry;

    public String getName() {
        return SERVICE_NAME;
    }

    WorkflowExecutionService(final Framework framework) {
        this.serviceList = new ArrayList<>();
        /*
         * WorkflowExecutionService chains several other services:
         * 1. builtin providers
         * 2. plugin providers
         */

        registry = new HashMap<>();
        //TODO:
        registry.put(WorkflowExecutor.NODE_FIRST, NodeFirstWorkflowExecutor.class);

        registry.put(WorkflowExecutor.STEP_FIRST, EngineWorkflowExecutor.class);
        registry.put(WorkflowExecutor.PARALLEL, EngineWorkflowExecutor.class);

        final ProviderService<WorkflowExecutor> primaryService = ServiceFactory.builtinService(
                framework,
                SERVICE_NAME,
                registry
        );

        serviceList.add(primaryService);
    }

    @Override
    protected List<ProviderService<WorkflowExecutor>> getServiceList() {
        return serviceList;
    }

    public static WorkflowExecutionService getInstanceForFramework(Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final WorkflowExecutionService service = new WorkflowExecutionService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (WorkflowExecutionService) framework.getService(SERVICE_NAME);
    }

    public WorkflowExecutor getExecutorForItem(final WorkflowExecutionItem workflow) throws ExecutionServiceException {
        String strategy = workflow.getWorkflow().getStrategy();
        if (registry.containsKey(strategy)) {
            return providerOfType(strategy);
        } else {
            return providerOfType(WorkflowExecutor.STEP_FIRST);
        }
    }

    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }


}
