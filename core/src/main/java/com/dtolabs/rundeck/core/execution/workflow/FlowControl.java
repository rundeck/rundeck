package com.dtolabs.rundeck.core.execution.workflow;

/**
 * Control workflow execution behavior
 */
public interface FlowControl {
    public static final String STATUS_SUCCEEDED = "succeeded";
    public static final String STATUS_FAILED = "failed";

    /**
     * execution halts with custom status
     */
    public void Halt(String statusString);

    /**
     * execution halts with success or failure
     */
    public void Halt(boolean success);

    /**
     * execution continues
     */
    public void Continue();
}
