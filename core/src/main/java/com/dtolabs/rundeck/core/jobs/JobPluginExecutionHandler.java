package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.JobPluginException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

import java.util.Optional;

/**
 * Can invoke job events given just execution context
 */
public interface JobPluginExecutionHandler {
    /**
     * Job start event
     *
     * @param executionContext input execution context
     * @throws JobPluginException
     */
    Optional<JobEventStatus> beforeJobStarts(final StepExecutionContext executionContext) throws JobPluginException;

    /**
     * Job end event
     *
     * @param executionContext execution context
     * @param result           result of job execution
     * @throws JobPluginException
     */
    Optional<JobEventStatus> afterJobEnds(final StepExecutionContext executionContext, final JobEventResult result)
            throws JobPluginException;
}
