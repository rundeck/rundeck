package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Map;

/**
 * Describes the state of a step within a workflow, which has an identifier, a possible sub workflow, and possible
 * node-oriented states.
 */
public interface WorkflowStepState {
    /**
     * The identifier
     *
     * @return
     */
    public StepIdentifier getStepIdentifier();

    /**
     * The step's state
     *
     * @return
     */
    public StepState getStepState();


    /**
     * Return a map of node name to step states for the step
     *
     * @return
     */
    public Map<String, ? extends StepState> getNodeStateMap();


    /**
     * Return true if the step contains a sub workflow
     *
     * @return
     */
    public boolean hasSubWorkflow();

    /**
     * Return the sub workflow state
     *
     * @return
     */
    public WorkflowState getSubWorkflowState();
}
