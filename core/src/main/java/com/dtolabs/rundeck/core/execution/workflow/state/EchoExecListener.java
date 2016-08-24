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

package com.dtolabs.rundeck.core.execution.workflow.state;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

/**
 * $INTERFACE is ... User: greg Date: 10/17/13 Time: 10:32 AM
 */
public class EchoExecListener implements WorkflowExecutionListener {
    public void beginWorkflowExecution(StepExecutionContext executionContext, WorkflowExecutionItem item) {
        System.err.println(String.format("beginWorkflowExecution(%s,%s)", executionContext, item));
    }

    public void finishWorkflowExecution(WorkflowExecutionResult result, StepExecutionContext executionContext,
            WorkflowExecutionItem item) {
        System.err.println(String.format("finishWorkflowExecution(%s,%s,%s)", result, executionContext, item));
    }

    public void beginWorkflowItem(int step, StepExecutionItem item) {
        System.err.println(String.format("beginWorkflowItem(%s,%s)", step, item));
    }

    @Override
    public void beginWorkflowItemErrorHandler(int step, StepExecutionItem item) {
        System.err.println(String.format("beginWorkflowItemErrorHandler(%s,%s)", step, item));
    }

    public void finishWorkflowItem(int step, StepExecutionItem item, StepExecutionResult result) {
        System.err.println(String.format("finishWorkflowItem(%s,%s,%s)", step, item, result));
    }

    @Override
    public void finishWorkflowItemErrorHandler(int step, StepExecutionItem item, StepExecutionResult result) {
        System.err.println(String.format("finishWorkflowItemErrorHandler(%s,%s,%s)", step, item, result));
    }

    public void beginStepExecution(StepExecutor executor, StepExecutionContext context, StepExecutionItem item) {
        System.err.println(String.format("beginStepExecution(%s,%s,%s)", executor, context, item));
    }


    public void finishStepExecution(StepExecutor executor, StatusResult result, StepExecutionContext context,
            StepExecutionItem item) {
        System.err.println(String.format("finishStepExecution(%s,%s,%s,%s)", executor, result, context, item));
    }

    public void beginExecuteNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node) {
        System.err.println(String.format("beginExecuteNodeStep(%s,%s,%s)", context, item, node));
    }

    public void finishExecuteNodeStep(NodeStepResult result, ExecutionContext context, StepExecutionItem item,
            INodeEntry node) {
        System.err.println(String.format("finishExecuteNodeStep(%s,%s,%s,%s)", result, context, item, node));
    }
}
