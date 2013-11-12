package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.*

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/15/13
 * Time: 3:41 PM
 */
class MutableWorkflowStateImpl implements MutableWorkflowState {
    def ArrayList<String> mutableNodeSet;
    def long stepCount;
    def ExecutionState executionState;
    def Date timestamp;
    def Map<Integer,MutableWorkflowStepState> mutableStepStates;
    def List<Map<Date,Map>> stateChanges=[]

    MutableWorkflowStateImpl(List<String> nodeSet, long stepCount) {
        this(nodeSet,stepCount,null)
    }
    MutableWorkflowStateImpl(List<String> nodeSet, long stepCount, Map<Integer, MutableWorkflowStepStateImpl> steps) {
        this.mutableNodeSet = new ArrayList<>()
        if(null!=nodeSet){
            this.mutableNodeSet.addAll(nodeSet)
        }
        this.stepCount = stepCount
        mutableStepStates = new HashMap<Integer,MutableWorkflowStepState>()
        for (int i = 1; i <= stepCount; i++) {
            mutableStepStates[i - 1] = steps && steps[i-1]? steps[i-1] : new MutableWorkflowStepStateImpl(StateUtils.stepIdentifier(i))
        }
        this.executionState=ExecutionState.WAITING
    }

    MutableWorkflowStepState getAt(Integer index){
        return mutableStepStates[index-1]
    }

    @Override
    List<WorkflowStepState> getStepStates() {
        return mutableStepStates.sort().values() as List
    }

    @Override
    List<String> getNodeSet() {
        return mutableNodeSet
    }

    @Override
    void updateStateForStep(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
        touchWFState(timestamp)

        Map<Integer, MutableWorkflowStepState> states = mutableStepStates;
        MutableWorkflowStepState currentStep = locateStepWithContext(identifier, states)
        if (identifier.context.size() > 1) {
            descendUpdateStateForStep(currentStep, identifier, stepStateChange, timestamp)
            return
        }
        addStateChange(timestamp, stepStateChange)

        //update the step found
        MutableStepState toUpdate
        if (stepStateChange.isNodeState()) {
            //find node state in stepstate
            if (null == currentStep.nodeStateMap[stepStateChange.nodeName]) {
                //create it
                currentStep.mutableNodeStateMap[stepStateChange.nodeName] = new MutableStepStateImpl()
            }
            toUpdate = currentStep.mutableNodeStateMap[stepStateChange.nodeName]
            toUpdate.executionState = updateState(toUpdate.executionState, stepStateChange.stepState.executionState)
            if (!currentStep.nodeStep && nodeSet) {
                if (null == currentStep.nodeStepTargets) {
                    currentStep.setNodeStepTargets(nodeSet)
                }
            }
        } else if(!currentStep.nodeStep){
            //overall step state
            toUpdate = currentStep.mutableStepState

            toUpdate.executionState = updateState(toUpdate.executionState, stepStateChange.stepState.executionState)
        }else{
            toUpdate=currentStep.mutableStepState
            if (null == currentStep.nodeStepTargets && nodeSet) {
                currentStep.setNodeStepTargets(nodeSet)
            }
        }
        transitionIfWaiting(currentStep.mutableStepState)

        //update state
        toUpdate.errorMessage = stepStateChange.stepState.errorMessage
        if(stepStateChange.stepState.metadata){
            if(null==toUpdate.metadata){
                toUpdate.metadata=[:]
            }
            toUpdate.metadata << stepStateChange.stepState.metadata
        }

        if (stepStateChange.isNodeState() && currentStep.nodeStep && stepStateChange.stepState.executionState.isCompletedState()) {
            //if all target nodes have completed execution state, mark the overall step state
            finishNodeStepIfNodesFinished(currentStep)
        }else if(stepStateChange.isNodeState() && currentStep.nodeStep && currentStep.stepState.executionState.isCompletedState()
                && stepStateChange.stepState.executionState==ExecutionState.RUNNING_HANDLER){
            currentStep.mutableStepState.executionState=ExecutionState.RUNNING_HANDLER
        }
    }

    private void addStateChange(Date timestamp, StepStateChange stepStateChange) {
        this.stateChanges << [(timestamp): asChangeMap(stepStateChange)]
    }

    static Map asChangeMap(StepStateChange stepStateChange) {
        [
                node: stepStateChange.nodeName,
                nodeState: stepStateChange.nodeState,
        ] + asChangeMap(stepStateChange.stepState)
    }

    static Map asChangeMap(StepState stepState) {
        [
                errorMessage: stepState.errorMessage,
                executionState: stepState.executionState.toString(),
                meta: stepState.metadata,
        ]
    }
/**
     * Descend into a sub workflow to update state
     * @param currentStep
     * @param identifier
     * @param stepStateChange
     * @param timestamp
     */
    private void descendUpdateStateForStep(MutableWorkflowStepState currentStep, StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
        transitionIfWaiting(currentStep.mutableStepState)
        //recurse to the workflow list to find the right index

        MutableWorkflowState subflow = currentStep.hasSubWorkflow() ?
            currentStep.mutableSubWorkflowState :
            currentStep.createMutableSubWorkflowState(null, 0)
        //recursively update subworkflow state for the step in the subcontext
        subflow.updateStateForStep(StateUtils.stepIdentifierTail(identifier), stepStateChange, timestamp);
    }

    private finishNodeStepIfNodesFinished(MutableWorkflowStepState currentStep){
        boolean finished = currentStep.nodeStepTargets.every { node -> currentStep.nodeStateMap[node]?.executionState?.isCompletedState() }
        if (finished) {
            boolean aborted = currentStep.nodeStateMap.values()*.executionState.any { it == ExecutionState.ABORTED }
            boolean failed = currentStep.nodeStateMap.values()*.executionState.any { it == ExecutionState.FAILED }
            def overall = aborted ? ExecutionState.ABORTED : failed ? ExecutionState.FAILED : ExecutionState.SUCCEEDED
            finalizeNodeStep(overall, currentStep)
        }
    }
    private finalizeNodeStep(ExecutionState overall, MutableWorkflowStepState currentStep){
        def nodeTargets = currentStep.nodeStepTargets?:this.nodeSet
        boolean finished = currentStep.nodeStateMap && nodeTargets?.every { node -> currentStep.nodeStateMap[node]?.executionState?.isCompletedState() }
        boolean aborted = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.any { it == ExecutionState.ABORTED }
        boolean abortedAll = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.every { it == ExecutionState.ABORTED }
        boolean failed = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.any { it == ExecutionState.FAILED }
        boolean failedAll = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.every { it == ExecutionState.FAILED }
        boolean succeeded = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.any { it == ExecutionState.SUCCEEDED }
        boolean succeededAll = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.every { it == ExecutionState.SUCCEEDED }
        boolean notStartedAll = currentStep.nodeStateMap?.size() == 0 ||
                currentStep.nodeStateMap?.values()*.executionState.every { it == ExecutionState.WAITING || it == null }
        ExecutionState result=overall
        if(finished){
            //all nodes finished
            if(abortedAll){
                result=ExecutionState.ABORTED
            }else if(failedAll){
                result=ExecutionState.FAILED
            }else if(succeededAll){
                result=ExecutionState.SUCCEEDED
            }else{
                result=ExecutionState.NODE_MIXED
            }
        }else if (aborted && !failed && !succeeded) {
            //partial aborted
            result = ExecutionState.ABORTED
        } else if (!aborted && failed && !succeeded) {
            //partial failed
            result = ExecutionState.FAILED
        } else if (!failed && !aborted && succeeded) {
            //partial success
            result = ExecutionState.NODE_PARTIAL_SUCCEEDED
        }else if (notStartedAll) {
            //not started
            result = ExecutionState.NOT_STARTED
        } else {
            result = ExecutionState.NODE_MIXED
        }

        if(currentStep.nodeStep || currentStep.hasSubWorkflow()){
            currentStep.mutableStepState.executionState = result
        }else{
            currentStep.mutableStepState.executionState = updateState(currentStep.mutableStepState.executionState, result)
        }

        //update any node states which are WAITING to NOT_STARTED
        nodeTargets.each{String node->
            if(!currentStep.mutableNodeStateMap[node]){
                currentStep.mutableNodeStateMap[node] = new MutableStepStateImpl(executionState:ExecutionState.WAITING)
            }
            MutableStepState state = currentStep.mutableNodeStateMap[node]
            if (state && state.executionState == ExecutionState.WAITING) {
                state.executionState = updateState(state.executionState, ExecutionState.NOT_STARTED)
            }
        }
    }

    private MutableWorkflowStepState locateStepWithContext(StepIdentifier identifier, Map<Integer, MutableWorkflowStepState> states) {
        MutableWorkflowStepState currentStep
        StepContextId subid = identifier.context[0]
        int ndx=subid.step-1
        if (ndx >= states.size() || null == states[ndx]) {
            states[ndx] = new MutableWorkflowStepStateImpl(StateUtils.stepIdentifier(subid))
            stepCount = states.size()
        }
        currentStep = states[ndx]
        currentStep
    }

    private void touchWFState(Date timestamp) {
        executionState = transitionStateIfWaiting(executionState)
        if (null == this.timestamp || this.timestamp < timestamp) {
            this.timestamp = timestamp
        }
    }


    private void transitionIfWaiting(MutableStepState step) {
        step.executionState = transitionStateIfWaiting(step.executionState)
    }
    private ExecutionState transitionStateIfWaiting(ExecutionState state) {
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
        if(fromState==toState){
            return toState
        }
        def allowed=[
                (ExecutionState.WAITING):[null,ExecutionState.WAITING],
                (ExecutionState.RUNNING): [null, ExecutionState.WAITING,ExecutionState.RUNNING],
                (ExecutionState.RUNNING_HANDLER):[null,ExecutionState.WAITING,ExecutionState.FAILED, ExecutionState.RUNNING,ExecutionState.RUNNING_HANDLER],
        ]
        ExecutionState.values().findAll{it.isCompletedState()}.each{
            allowed[it]= [it,ExecutionState.RUNNING, ExecutionState.RUNNING_HANDLER, ExecutionState.WAITING]
        }
        if (toState == null) {
//            System.err.println("Cannot change state to ${toState}")
            throw new IllegalStateException("Cannot change state to ${toState}")
        }
        if(!(fromState in allowed[toState])){
//            System.err.println("Cannot change from " + fromState + " to " + toState)
            throw new IllegalStateException("Cannot change from " + fromState + " to " + toState)
        }

        toState
    }

    @Override
    void updateWorkflowState(ExecutionState executionState, Date timestamp, List<String> nodenames) {
        updateWorkflowState(false,executionState,timestamp,nodenames)
    }
    void updateWorkflowState(boolean subflow,ExecutionState executionState, Date timestamp, List<String> nodenames) {
        touchWFState(timestamp)
        this.executionState = updateState(this.executionState, executionState)
        if (null != nodenames && (null == mutableNodeSet || mutableNodeSet.size() < 1)) {
            mutableNodeSet = new ArrayList<>(nodenames)
        }
        if(executionState.isCompletedState()){
            cleanupSteps(executionState, timestamp)
        }
    }

    private void cleanupSteps(ExecutionState executionState, Date timestamp) {
        mutableStepStates.each { i, step ->
            if (!step.stepState.executionState.isCompletedState()) {
                resolveStepCompleted(executionState, timestamp, i+1, step)
            }
        }
    }

    /**
     * Resolve the completed state of a step based on overal workflow completion state
     * @param executionState
     * @param date
     * @param i
     * @param mutableWorkflowStepState
     */
    def resolveStepCompleted(ExecutionState executionState, Date date, int i, MutableWorkflowStepState mutableWorkflowStepState) {
        if(mutableWorkflowStepState.nodeStep){
            //a node step
            finalizeNodeStep(executionState,mutableWorkflowStepState)
        }else {
            def curstate= mutableWorkflowStepState.mutableStepState.executionState
            def newstate=executionState
            switch (curstate){
                case null:
                case ExecutionState.WAITING:
                    newstate=ExecutionState.NOT_STARTED
                    break
                case ExecutionState.RUNNING:
                    newstate = ExecutionState.ABORTED
                    break
            }
            mutableWorkflowStepState.mutableStepState.executionState = updateState(curstate, newstate)
        }
    }

    @Override
    void updateSubWorkflowState(StepIdentifier identifier, ExecutionState executionState, Date timestamp, List<String> nodeNames) {
        touchWFState(timestamp)
        Map<Integer, MutableWorkflowStepState> states = mutableStepStates;

        if (identifier.context.size() > 0) {
            //descend one step
            MutableWorkflowStepState nextStep = locateStepWithContext(identifier, states)
            MutableWorkflowState nextWorkflow = nextStep.hasSubWorkflow() ?
                nextStep.mutableSubWorkflowState :
                nextStep.createMutableSubWorkflowState(null, 0)

            transitionIfWaiting(nextStep.mutableStepState)
            //more steps to descend
            nextWorkflow.updateSubWorkflowState(StateUtils.stepIdentifierTail(identifier), executionState, timestamp,nodeNames);
        }else {
            //update the workflow state for this workflow
            updateWorkflowState(true, executionState, timestamp, nodeNames)
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
