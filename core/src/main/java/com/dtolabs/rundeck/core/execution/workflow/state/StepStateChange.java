package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Date;

/**
 * Represents a change to the workflow state
 */
public interface StepStateChange  {
    /**
     * @return The new state
     */
    public StepState getStepState();

    /**
     * @return The node name
     *
     */
    public String getNodeName();

    /**
     * @return True if this state represents a node, false if it represents an overall step state
     *
     */
    public boolean isNodeState();
}
