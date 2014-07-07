package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Return a map of parameter strings to representative WorkflowStepStates
     *
     * @return
     */
    Map<String, ? extends WorkflowStepState> getParameterizedStateMap();


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

    /**
     * Return the set of node targets if this is a node step, or null
     * @return
     */
    public List<String> getNodeStepTargets();

    /**
     * Return true if this is a node step
     * @return
     */
    public boolean isNodeStep();
}
