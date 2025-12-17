package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.ExecutionLifecycleComponentException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;

import java.util.Optional;

/**
 * Can invoke execution lifecycle events given just execution context
 */
public interface ExecutionLifecycleComponentHandler {
    /**
     * Job start event
     *
     * @param executionContext input execution context
     * @throws ExecutionLifecycleComponentException
     */
    Optional<ExecutionLifecycleStatus> beforeJobStarts(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem workflowItem
    ) throws ExecutionLifecycleComponentException;

    /**
     * Job end event
     *
     * @param executionContext execution context
     * @param result           result of job execution
     * @throws ExecutionLifecycleComponentException
     */
    Optional<ExecutionLifecycleStatus> afterJobEnds(final StepExecutionContext executionContext, final JobEventResult result)
            throws ExecutionLifecycleComponentException;
}
