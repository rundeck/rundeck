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
* WorkflowExecutionListener.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/28/11 1:44 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

/**
 * WorkflowExecutionListener is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface WorkflowExecutionListener  {

    public void beginWorkflowExecution(StepExecutionContext executionContext, WorkflowExecutionItem item);

    public void finishWorkflowExecution(WorkflowExecutionResult result, StepExecutionContext executionContext,
                                        WorkflowExecutionItem item);
    public void beginWorkflowItem(int step, StepExecutionItem item);
    public void beginWorkflowItemErrorHandler(int step, StepExecutionItem item);
    public void finishWorkflowItem(int step, StepExecutionItem item, StepExecutionResult result);
    public void finishWorkflowItemErrorHandler(int step, StepExecutionItem item, StepExecutionResult success);

    /**
     * Called when execution begins for a step
     * @param executor executor
     * @param context context
     * @param item step
     */
    public void beginStepExecution(StepExecutor executor,StepExecutionContext context, StepExecutionItem item);

    /**
     * Called when execution finishes for a step
     * @param executor executor
     * @param result result
     * @param context context
     * @param item step
     */
    public void finishStepExecution(StepExecutor executor, StatusResult result, StepExecutionContext context, StepExecutionItem item);

    /**
     * Begin execution of a node step
     * @param context context
     * @param item step
     * @param node node
     */
    public void beginExecuteNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node);

    /**
     * Finish execution of a node step
     * @param result result
     * @param context context
     * @param item step
     * @param node node
     */
    public void finishExecuteNodeStep(NodeStepResult result, ExecutionContext context, StepExecutionItem item,
            INodeEntry node);
}
