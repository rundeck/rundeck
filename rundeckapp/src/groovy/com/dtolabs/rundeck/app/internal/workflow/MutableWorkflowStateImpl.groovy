package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.*

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/15/13
 * Time: 3:41 PM
 */
class MutableWorkflowStateImpl implements MutableWorkflowState {
    def HashSet<String> mutableNodeSet;
    def long stepCount;
    def ExecutionState executionState;
    def Date timestamp;
    def Map<Integer,MutableWorkflowStepState> mutableStepStates;

    MutableWorkflowStateImpl(Set<String> nodeSet, long stepCount) {
        this.mutableNodeSet = new HashSet<String>()
        if(null!=nodeSet){
            this.mutableNodeSet.addAll(nodeSet)
        }
        this.stepCount = stepCount
        mutableStepStates = new HashMap<Integer,MutableWorkflowStepState>()
        for (int i = 1; i <= stepCount; i++) {
            mutableStepStates[i - 1] = new MutableWorkflowStepStateImpl(StateUtils.stepIdentifier(i))
        }
    }

    @Override
    List<WorkflowStepState> getStepStates() {
        return mutableStepStates.sort().values() as List
    }

    @Override
    Set<String> getNodeSet() {
        return mutableNodeSet
    }

    @Override
    void updateStateForStep(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
        /*
To update state:
locate step in the workflow context."1/2", look for stepstate[1].workflowstate.stepstate[2].
if(node state) index via node name
if(overall state)
change status
merge metadata
update timestamp. update timestamp on WorkflowState(s)
         */
        executionState = transitionWaiting(executionState)
        if(null==this.timestamp || this.timestamp< timestamp){
            this.timestamp=timestamp
        }
        MutableWorkflowStepState found = null;
        Map<Integer, MutableWorkflowStepState> states = mutableStepStates;
        def subid = identifier.context[0]-1
        if (subid < 0) {
            throw new IllegalStateException("Could not update state for step context: " + identifier
                    + ": no step at index ${subid} found")
        } else if (subid >= states.size() || null == states[subid]) {
            states[subid]= new MutableWorkflowStepStateImpl(StateUtils.stepIdentifier(subid+1))
            stepCount=states.size()
        }
        found = states[subid]
        if (identifier.context.size() > 1) {
            //recurse to the workflow list to find the right index

            MutableWorkflowState subflow
            if (found.hasSubWorkflow()) {
                subflow= found.mutableSubWorkflowState
            } else {
                //create it
                //TODO
                subflow=found.createMutableSubWorkflowState(null, 0)
            }
            found.mutableStepState.executionState = transitionWaiting(found.stepState.executionState)
            def sublist = identifier.context.subList(1, identifier.context.size())
            subflow.updateStateForStep(StateUtils.stepIdentifier(sublist), stepStateChange, timestamp);
            return
        }

        //update the step found
        MutableStepState toUpdate = null
        if (stepStateChange.isNodeState()) {
            //find node state in stepstate
            if (found.nodeStateMap[stepStateChange.nodeName]) {
                toUpdate = found.mutableNodeStateMap[stepStateChange.nodeName]
            } else {
                //create it
                toUpdate = new MutableStepStateImpl()
                found.mutableNodeStateMap[stepStateChange.nodeName] = toUpdate
            }
            found.mutableStepState.executionState=transitionWaiting(found.mutableStepState.executionState)
        } else {
            //overall step state
            toUpdate = found.mutableStepState
        }
        //update state
        toUpdate.errorMessage = stepStateChange.stepState.errorMessage
        toUpdate.executionState = updateState(toUpdate.executionState,stepStateChange.stepState.executionState)
        toUpdate.metadata = stepStateChange.stepState.metadata
    }

    private ExecutionState transitionWaiting(ExecutionState state) {
        if (waitingState(state)) {
            return updateState(state, ExecutionState.RUNNING)
        }else{
            return state
        }
    }

    private static boolean waitingState(ExecutionState state) {
        null == state || state == ExecutionState.WAITING
    }

    /**
     * Update state change
     * @param fromState
     * @param toState
     * @return
     */
    public static ExecutionState updateState(ExecutionState fromState, ExecutionState toState) {
        switch (toState){
            case null:
                throw new IllegalStateException("Cannot change state to ${fromState}")
            case ExecutionState.WAITING:
                if (fromState != null) {
                    throw new IllegalStateException("Cannot change from " + fromState + " to " + toState)
                }
                break;
            case ExecutionState.RUNNING:
                if (fromState != null && fromState!= ExecutionState.WAITING) {
                    throw new IllegalStateException("Cannot change from " + fromState + " to " + toState)
                }
                break;
            case ExecutionState.SUCCEEDED:
            case ExecutionState.FAILED:
            case ExecutionState.ABORTED:
                if(toState!=fromState && fromState!=ExecutionState.RUNNING) {
                    throw new IllegalStateException("Cannot change from " + fromState + " to " + toState)
                }
        }
        toState
    }

    @Override
    void updateWorkflowState(ExecutionState executionState, Date timestamp, Set<String> nodenames) {
        this.executionState = updateState(this.executionState, executionState)
        this.timestamp = timestamp
        if (null != nodenames && (null == mutableNodeSet || mutableNodeSet.size() < 1)) {
            mutableNodeSet = new HashSet(nodenames)
        }
    }


    @Override
    public java.lang.String toString() {
        return "WF{" +
                "nodes=" + mutableNodeSet +
                ", stepCount=" + stepCount +
                ", state=" + executionState +
                ", timestamp=" + timestamp +
                ", steps=" + mutableStepStates +
                '}';
    }
}
