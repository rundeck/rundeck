package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.ExecutionLifecyclePluginException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;

import java.util.Optional;

/**
 * Can invoke execution lifecycle events given just execution context
 */
public interface ExecutionLifecyclePluginHandler {
    /**
     * Job start event
     *
     * @param executionContext input execution context
     * @throws ExecutionLifecyclePluginException
     */
    Optional<ExecutionLifecycleStatus> beforeJobStarts(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem workflowItem
    ) throws ExecutionLifecyclePluginException;

    /**
     * Job end event
     *
     * @param executionContext execution context
     * @param result           result of job execution
     * @throws ExecutionLifecyclePluginException
     */
    Optional<ExecutionLifecycleStatus> afterJobEnds(final StepExecutionContext executionContext, final JobEventResult result)
            throws ExecutionLifecyclePluginException;
}
