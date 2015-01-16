package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the state of a workflow
 */
public interface WorkflowState extends HasExecutionState {
    /**
     * @return The set of nodes the workflow is running on
     *
     */
    public List<String> getNodeSet();

    /**
     * @return the set of all nodes this and and all sub workflows are operating on
     */
    public List<String> getAllNodes();

    /**
     * @return the name of the server node
     */
    public String getServerNode();

    /**
     * @return The number of steps the workflow will run
     */
    public long getStepCount();

    /**
     * @return The execution state of the workflow
     */
    public ExecutionState getExecutionState();

    /**
     * @return The latest timestamp for the workflow state
     */
    public Date getUpdateTime();

    public Date getStartTime();

    public Date getEndTime();

    /**
     * @return The list of states for the steps
     */
    public List<WorkflowStepState> getStepStates();

    /**
     * @return The list of states for all nodes
     */
    public Map<String,? extends WorkflowNodeState> getNodeStates();
}
