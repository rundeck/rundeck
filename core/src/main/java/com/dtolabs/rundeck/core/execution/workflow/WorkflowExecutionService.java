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
* WorkflowExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/28/11 4:46 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.plugins.BaseProviderRegistryService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;

/**
 * WorkflowExecutionService provides ability to execute workflows
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class WorkflowExecutionService extends BaseProviderRegistryService<WorkflowExecutor> implements
    FrameworkSupportService {
    private static final String SERVICE_NAME = "WorkflowExecution";

    public String getName() {
        return SERVICE_NAME;
    }

    WorkflowExecutionService(final Framework framework) {
        super(framework);

        registry.put(WorkflowStrategy.STEP_FIRST, StepFirstWorkflowStrategy.class);
        registry.put(WorkflowStrategy.NODE_FIRST, NodeFirstWorkflowStrategy.class);
        registry.put(WorkflowStrategy.PARALLEL, ParallelWorkflowStrategy.class);
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
        return providerOfType(workflow.getWorkflow().getStrategy());
    }

}
