package com.dtolabs.rundeck.execution;

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

public class WorkflowExecutionListenerTest implements WorkflowExecutionListener {
    @Override
    public void beginWorkflowExecution(StepExecutionContext executionContext, WorkflowExecutionItem item) {

    }

    @Override
    public void finishWorkflowExecution(WorkflowExecutionResult result, StepExecutionContext executionContext, WorkflowExecutionItem item) {

    }

    @Override
    public void beginWorkflowItem(int step, StepExecutionItem item) {

    }

    @Override
    public void beginWorkflowItemErrorHandler(int step, StepExecutionItem item) {

    }

    @Override
    public void finishWorkflowItem(int step, StepExecutionItem item, StepExecutionResult result) {

    }

    @Override
    public void finishWorkflowItemErrorHandler(int step, StepExecutionItem item, StepExecutionResult success) {

    }

    @Override
    public void beginStepExecution(StepExecutor executor, StepExecutionContext context, StepExecutionItem item) {

    }

    @Override
    public void finishStepExecution(StepExecutor executor, StatusResult result, StepExecutionContext context, StepExecutionItem item) {

    }

    @Override
    public void beginExecuteNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node) {

    }

    @Override
    public void finishExecuteNodeStep(NodeStepResult result, ExecutionContext context, StepExecutionItem item, INodeEntry node) {

    }
}
