/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* StepExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/2/12 10:42 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.plugins.BaseProviderRegistryService;


/**
 * StepExecutionService can provide executors for workflow steps.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class StepExecutionService extends BaseProviderRegistryService<StepExecutor> implements FrameworkSupportService {
    private static final String SERVICE_NAME = "StepExecution";
    public static final String DEFAULT_EXECUTION_TYPE = NodeDispatchStepExecutor.STEP_EXECUTION_TYPE;

    public String getName() {
        return SERVICE_NAME;
    }

    StepExecutionService(final Framework framework) {
        super(framework);

        registry.put(DEFAULT_EXECUTION_TYPE, NodeDispatchStepExecutor.class);
    }

    public static StepExecutionService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final StepExecutionService service = new StepExecutionService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (StepExecutionService) framework.getService(SERVICE_NAME);
    }

    public StepExecutor getExecutorForItem(final StepExecutionItem item) throws ExecutionServiceException {
        String type = DEFAULT_EXECUTION_TYPE;
//        if (null != item.getType()) {
            type = item.getType();
//        }
        return providerOfType(type);
    }
}
