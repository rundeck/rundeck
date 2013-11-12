package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Represents the state of a workflow
 */
public interface WorkflowState {
    /**
     * The set of nodes the workflow is running on
     *
     * @return
     */
    public List<String> getNodeSet();

    /**
     * The number of steps the workflow will run
     *
     * @return
     */
    public long getStepCount();

    /**
     * The execution state of the workflow
     *
     * @return
     */
    public ExecutionState getExecutionState();

    /**
     * The latest timestamp for the workflow state
     *
     * @return
     */
    public Date getTimestamp();

    /**
     * The list of states for the steps
     *
     * @return
     */
    public List<WorkflowStepState> getStepStates();
}
