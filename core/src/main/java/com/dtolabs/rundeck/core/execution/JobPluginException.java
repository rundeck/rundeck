package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;

/**
 * JobPluginException thrown when an error occurs during execution of a JobPlugin.
 *
 * @author rnavarro
 * @version $Revision$
 */
public class JobPluginException extends Exception {

    public JobPluginException() {
        super();
    }

    public JobPluginException(String msg) {
        super(msg);
    }

    public JobPluginException(Exception cause) {
        super(cause);
    }

    public JobPluginException(String msg, Exception cause) {
        super(msg, cause);
    }

    public JobPluginException(final String message, final FailureReason failureReason) {
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
