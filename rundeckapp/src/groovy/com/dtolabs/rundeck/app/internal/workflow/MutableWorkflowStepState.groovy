package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStepState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/16/13
 * Time: 10:39 AM
 */
public interface MutableWorkflowStepState extends WorkflowStepState{

    /**
     * The step's state
     *
     * @return
     */
    MutableStepState getMutableStepState();

    /**
     * Return a map of node name to step states for the step
     *
     * @return
     */
    Map<String, MutableStepState> getMutableNodeStateMap();

    /**
     * Return a parameterized step state
     * @param ident
     * @param params
     * @return
     */
    public MutableWorkflowStepState getParameterizedStepState(StepIdentifier ident,Map<String,String> params);

    /**
     * Return a map of node name to step states for the step
     *
     * @return
     */
    Map<String, MutableWorkflowStepState> getMutableParameterizedStateMap();

    /**
     * Return the sub workflow state
     *
     * @return
     */
    MutableWorkflowState getMutableSubWorkflowState();
    MutableWorkflowStepState getOwnerStepState();

    /**
     * Creates a mutable sub workflow state and enables it
     * @return
     */
    MutableWorkflowState createMutableSubWorkflowState(List<String> nodeSet, long count);

    /**
     * Indicates that the step is a node step with the given targets
     * @param nodeset
     */
    void setNodeStepTargets(List<String> nodeset);

}
