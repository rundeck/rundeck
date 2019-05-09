package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;

/**
 * JobLifeCycleException thrown when an error occurs during execution of a JobLifeCyclePlugin.
 *
 * @author rnavarro
 * @version $Revision$
 */
public class JobLifeCycleException extends Exception {

    public JobLifeCycleException() {
        super();
    }

    public JobLifeCycleException(String msg) {
        super(msg);
    }

    public JobLifeCycleException(Exception cause) {
        super(cause);
    }

    public JobLifeCycleException(String msg, Exception cause) {
        super(msg, cause);
    }

    public JobLifeCycleException(final String message, final FailureReason failureReason) {
        super(message);
        this.setFailureReason(failureReason);
    }

    private FailureReason failureReason = ExecutionException.Reason.Unknown;

    public FailureReason getFailureReason() {
        return failureReason;
    }

    protected void setFailureReason(FailureReason failureReason) {
        this.failureReason = failureReason;
    }

    static enum Reason
            implements FailureReason
    {
        Unknown
    }

}
