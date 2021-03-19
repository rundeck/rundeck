package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.jobs.JobReference;

/**
 * Service for validating executions.
 */
public interface ExecutionValidator {

    /**
     * Validates if a job can run more executions.
     *
     * @param job the job reference
     * @param retry if the validation should consider retry.
     * @param prevId The previous id un case of retry.
     * @return true if the job has multiple executions enabled and the executions limit has not been reached. false otherwise.
     */
    public boolean canRunMoreExecutions(JobValidationReference job, boolean retry, long prevId);

}
