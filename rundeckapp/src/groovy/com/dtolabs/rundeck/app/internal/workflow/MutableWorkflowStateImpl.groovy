package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/15/13
 * Time: 3:41 PM
 */
class MutableWorkflowStateImpl implements MutableWorkflowState{
    def HashSet<String> nodeSet;
    def long stepCount;
    def ExecutionState executionState;
    def Date timestamp;
    def ArrayList<StepState> stepStates;

    @Override
    void updateStateForStep(StepState stepState, Date timestamp) {
        /*
To update state:
locate step in the workflow context."1/2", look for stepstate[1].workflowstate.stepstate[2].
if(node state) index via node name
if(overall state)
change status
merge metadata
update timestamp. update timestamp on WorkflowState(s)
         */
        StepState found=null;
        List<StepState> states=stepStates;
        for (Long id : stepState.stepIdentifier.context) {
            if(states==null){
                throw new IllegalStateException("Could not update state for step context: "+ stepState.stepIdentifier
                        +": Invalid context")
            }
            if (id >= 0 && id < states.size()) {
                found = states[id]
                if (found.hasSubWorkflow()) {
                    states = found.subWorkflowState.stepStates;
                } else {
                    states = null
                }
            }
        }
        if(!found){
            throw new IllegalStateException("Could not update state for step context: " + stepState.stepIdentifier
                    +": context not found")
        }
        if(stepState.isNodeState()){
            //find node state in stepstate
        }else{

        }
    }

    @Override
    void updateWorkflowState(ExecutionState executionState, Date timestamp) {

    }
}
