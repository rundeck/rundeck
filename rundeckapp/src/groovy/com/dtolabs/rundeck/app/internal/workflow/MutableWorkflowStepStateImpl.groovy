package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/16/13
 * Time: 11:05 AM
 */
class MutableWorkflowStepStateImpl implements MutableWorkflowStepState {
    MutableStepState mutableStepState
    MutableWorkflowState mutableSubWorkflowState
    StepIdentifier stepIdentifier;
    Map<String, MutableStepState> mutableNodeStateMap;
    boolean subworkflow

    MutableWorkflowStepStateImpl(StepIdentifier stepIdentifier) {
        this.stepIdentifier = stepIdentifier
        this.mutableStepState=new MutableStepStateImpl()
        this.mutableStepState.executionState= ExecutionState.WAITING
        this.mutableNodeStateMap = new HashMap<String, MutableStepState>()
        this.subworkflow=false
    }

    public StepState getStepState(){
        return mutableStepState
    }

    /**
     * Return a map of node name to step states for the step
     *
     * @return
     */
    public Map<String, ? extends StepState> getNodeStateMap(){
        return mutableNodeStateMap
    }

    /**
     * Return true if the step contains a sub workflow
     *
     * @return
     */
    public boolean hasSubWorkflow(){
        subworkflow
    }

    /**
     * Return the sub workflow state
     *
     * @return
     */
    public WorkflowState getSubWorkflowState(){
        mutableSubWorkflowState
    }

    @Override
    MutableWorkflowState createMutableSubWorkflowState(Set<String> nodeSet,int count) {
        subworkflow=true
        mutableSubWorkflowState = new MutableWorkflowStateImpl(nodeSet, count)
    }


    @Override
    public java.lang.String toString() {
        return "WFStep{" +
                "step=" + mutableStepState +
                (subworkflow? ", sub=" + mutableSubWorkflowState :'') +
                ", id=" + stepIdentifier +
                ", nodes=" + mutableNodeStateMap +
                '}';
    }
}
