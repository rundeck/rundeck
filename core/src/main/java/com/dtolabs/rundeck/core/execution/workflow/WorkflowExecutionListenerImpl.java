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
* WorkflowExecutionListenerImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/28/11 3:30 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

/**
 * WorkflowExecutionListenerImpl uses the {@link WorkflowExecutionListener} methods to maintain workflow execution
 * context data while executing workflows, allowing the ContextLogger to have proper context.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class WorkflowExecutionListenerImpl extends ContextualExecutionListener implements WorkflowExecutionListener,
        ExecutionListener
{


    private WorkflowExecutionListenerImpl delegate;

    protected WorkflowExecutionListenerImpl(WorkflowExecutionListenerImpl delegate, final ExecutionLogger logger) {
        super(delegate, logger);
        this.delegate = delegate;
    }

    public WorkflowExecutionListenerImpl(
            final FailedNodesListener failedNodesListener,
            final ExecutionLogger logger
    )
    {
        super(failedNodesListener, logger);
    }

    public void ignoreErrors(boolean value){

    }
    @Override
    public void beginExecuteNodeStep(
            final ExecutionContext context,
            final NodeStepExecutionItem item,
            final INodeEntry node
    )
    {
        if (null != delegate) {
            delegate.beginExecuteNodeStep(context, item, node);
            return;
        }
        super.beginExecuteNodeStep(context, item, node);
        log(
                Constants.DEBUG_LEVEL,
                "[workflow] beginExecuteNodeStep(" + node.getNodename() + "): " + item.getType() + ": " + item
        );
    }

    @Override
    public void finishExecuteNodeStep(
            final NodeStepResult result, final ExecutionContext context,
            final StepExecutionItem item, final INodeEntry node
    )
    {
        if (null != delegate) {
            delegate.finishExecuteNodeStep(result, context, item, node);
            return;
        }
        super.finishExecuteNodeStep(result, context, item, node);
        log(
                Constants.DEBUG_LEVEL,
                "[workflow] finishExecuteNodeStep(" + node.getNodename() + "): " + item.getType() + ": " + result
        );
    }


    public void beginWorkflowExecution(final StepExecutionContext executionContext, final WorkflowExecutionItem item) {
        if (null != delegate) {
            delegate.beginWorkflowExecution(executionContext, item);
            return;
        }
        log(
                Constants.DEBUG_LEVEL,
                "[workflow] Begin execution: " + item.getType()
        );
    }


    public void finishWorkflowExecution(
            final WorkflowExecutionResult result, final StepExecutionContext executionContext,
            final WorkflowExecutionItem item
    )
    {
        if (null != delegate) {
            delegate.finishWorkflowExecution(result, executionContext, item);
            return;
        }
        log(
                Constants.DEBUG_LEVEL,
                "[workflow] Finish execution:  " + item.getType() + ": " + result
        );
    }

    public void beginWorkflowItem(final int step, final StepExecutionItem item) {
        if (null != delegate) {
            delegate.beginWorkflowItem(step, item);
            return;
        }
        log(
                Constants.DEBUG_LEVEL,
                "[workflow] Begin step: " + step + "," + item.getType()
        );
    }

    @Override
    public void beginWorkflowItemErrorHandler(int step, StepExecutionItem item) {
        if (null != delegate) {
            delegate.beginWorkflowItemErrorHandler(step, item);
            return;
        }
        log(
                Constants.DEBUG_LEVEL,
                "[workflow] Begin error handler: " + step + "," + item.getType()
        );
    }

    public void finishWorkflowItem(final int step, final StepExecutionItem item, StepExecutionResult result) {
        if (null != delegate) {
            delegate.finishWorkflowItem(step, item, result);
            return;
        }
        log(
                Constants.DEBUG_LEVEL,
                "[workflow] Finish step: " + step + "," + item.getType()
        );
    }

    @Override
    public void finishWorkflowItemErrorHandler(int step, StepExecutionItem item, StepExecutionResult result) {
        if (null != delegate) {
            delegate.finishWorkflowItemErrorHandler(step, item, result);
            return;
        }
        log(
                Constants.DEBUG_LEVEL,
                "[workflow] Finish error handler: " + step + "," + item.getType()
        );
    }

    @Override
    public ExecutionListenerOverride createOverride() {
        return new WorkflowExecutionListenerImpl(this, getLogger());
    }
}
