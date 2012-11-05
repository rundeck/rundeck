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
* NodeDispatchStepExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/2/12 10:47 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.BaseExecutionResult;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionItem;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;


/**
 * NodeDispatchStepExecutor dispatches the step execution item to all nodes, via the ExecutionService
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeDispatchStepExecutor implements StepExecutor {
    public static final String STEP_EXECUTION_TYPE = "NodeDispatch";

    @Override
    public boolean isNodeDispatchStep(ExecutionItem item) {
        return true;
    }

    @Override
    public StatusResult executeWorkflowStep(final ExecutionContext context, final ExecutionItem item) {
        final Framework framework = context.getFramework();
        try {
            return framework.getExecutionService().dispatchToNodes(context, item);
        } catch (DispatcherException e) {
            return new StepExecutionResultImpl(false, e);
        }
    }
}
