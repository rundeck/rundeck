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
     * The set of nodes the workflow is running on
     *
     * @return
     */
    public List<String> getNodeSet();

    /**
     * Return the set of all nodes this and and all sub workflows are operating on
     * @return
     */
    public List<String> getAllNodes();

    /**
     * Return the name of the server node
     * @return
     */
    public String getServerNode();

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
    public Date getUpdateTime();

    public Date getStartTime();

    public Date getEndTime();

    /**
     * The list of states for the steps
     *
     * @return
     */
    public List<WorkflowStepState> getStepStates();

    /**
     * The list of states for all nodes
     *
     * @return
     */
    public Map<String,? extends WorkflowNodeState> getNodeStates();
}
