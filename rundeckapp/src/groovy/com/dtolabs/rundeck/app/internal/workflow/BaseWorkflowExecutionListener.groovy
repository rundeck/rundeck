package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.StatusResult
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult

/**
 * Base listener for overriding
 */
abstract class BaseWorkflowExecutionListener implements WorkflowExecutionListener{
    @Override
    void beginWorkflowExecution(final StepExecutionContext executionContext, final WorkflowExecutionItem item) {

    }

    @Override
    void finishWorkflowExecution(
            final WorkflowExecutionResult result,
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item
    )
    {

    }

    @Override
    void beginWorkflowItem(final int step, final StepExecutionItem item) {

    }

    @Override
    void beginWorkflowItemErrorHandler(final int step, final StepExecutionItem item) {

    }

    @Override
    void finishWorkflowItem(final int step, final StepExecutionItem item, final StepExecutionResult result) {

    }

    @Override
    void finishWorkflowItemErrorHandler(
            final int step,
            final StepExecutionItem item,
            final StepExecutionResult success
    )
    {

    }

    @Override
    void beginStepExecution(
            final StepExecutor executor,
            final StepExecutionContext context,
            final StepExecutionItem item
    )
    {

    }

    @Override
    void finishStepExecution(
            final StepExecutor executor,
            final StatusResult result,
            final StepExecutionContext context,
            final StepExecutionItem item
    )
    {

    }

    @Override
    void beginExecuteNodeStep(final ExecutionContext context, final NodeStepExecutionItem item, final INodeEntry node) {

    }

    @Override
    void finishExecuteNodeStep(
            final NodeStepResult result,
            final ExecutionContext context,
            final StepExecutionItem item,
            final INodeEntry node
    )
    {

    }
}
