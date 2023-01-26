package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;

/**
 * ExecutionLifecyclePluginException thrown when an error occurs during execution of a ExecutionLifecyclePlugin.
 *
 * @author rnavarro
 * @version $Revision$
 */
public class ExecutionLifecyclePluginException extends ExecutionLifecycleComponentException {

    public ExecutionLifecyclePluginException() {
        super();
    }

    public ExecutionLifecyclePluginException(String msg) {
        super(msg);
    }

    public ExecutionLifecyclePluginException(Exception cause) {
        super(cause);
    }

    public ExecutionLifecyclePluginException(String msg, Exception cause) {
        super(msg, cause);
    }

    public ExecutionLifecyclePluginException(final String message, final FailureReason failureReason) {
        super(message);
        this.setFailureReason(failureReason);
    }

    private FailureReason failureReason = Reason.Unknown;

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
