package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.dispatcher.ExecutionState;

import java.util.Set;

/**
 * The state of a job
 */
public interface JobState {
    /**
     * @return true if the job is currently running
     */
    public boolean isRunning();

    /**
     * @return if running, return the execution ID(s) of the execution. otherwise return null.
     */
    public Set<String> getRunningExecutionIds();

    /**
     * @return the execution state of the last execution, or null if not execution has completed.
     */
    public ExecutionState getPreviousExecutionState();

    /**
     * @return the custom status string of the last execution, or null if not set
     */
    public String getPreviousExecutionStatusString();
}
