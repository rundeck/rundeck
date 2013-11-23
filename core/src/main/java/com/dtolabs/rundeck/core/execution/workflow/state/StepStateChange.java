package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Date;

/**
 * Represents a change to the workflow state
 */
public interface StepStateChange  {
    /**
     * The new state
     * @return
     */
    public StepState getStepState();

    /**
     * The node name
     *
     * @return
     */
    public String getNodeName();

    /**
     * True if this state represents a node, false if it represents an overall step state
     *
     * @return
     */
    public boolean isNodeState();
}
