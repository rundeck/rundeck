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
    def ArrayList<String> mutableAllNodes;
    def long stepCount;
    def ExecutionState executionState;
    def Date updateTime;
    def Date startTime;
    def Date endTime;
    def Map<Integer,MutableWorkflowStepState> mutableStepStates;
    def Map<String,MutableWorkflowNodeState> mutableNodeStates;
    private StepIdentifier parentStepId
    def String serverNode

    MutableWorkflowStateImpl(List<String> nodeSet, long stepCount) {
        this(nodeSet,stepCount,null)
    }
    MutableWorkflowStateImpl(List<String> nodeSet, long stepCount, Map<Integer, MutableWorkflowStepStateImpl> steps) {
        this(nodeSet,stepCount,steps,null,null)
    }

    MutableWorkflowStateImpl(List<String> nodeSet, long stepCount, Map<Integer, MutableWorkflowStepStateImpl> steps, StepIdentifier parentStepId, String serverNode) {
        this.serverNode=serverNode
        this.parentStepId = parentStepId
        this.mutableNodeSet = new ArrayList<>()
        this.mutableAllNodes = new ArrayList<>()
        if (null != nodeSet) {
            this.mutableNodeSet.addAll(nodeSet)
        }
        this.mutableAllNodes.addAll(mutableNodeSet)
        this.stepCount = stepCount
        mutableStepStates = new HashMap<Integer, MutableWorkflowStepState>()
        for (int i = 1; i <= stepCount; i++) {
            mutableStepStates[i - 1] = steps && steps[i - 1] ? steps[i - 1] : new MutableWorkflowStepStateImpl(StateUtils.stepIdentifierAppend(parentStepId, StateUtils.stepIdentifier(i)))
        }
        this.executionState = ExecutionState.WAITING
        mutableNodeStates = new HashMap<String, MutableWorkflowNodeState>()
        mutableAllNodes.each { node ->
            mutableNodeStates[node] = new MutableWorkflowNodeStateImpl(node)
        }
        if (mutableNodeStates && mutableStepStates) {
            //link nodes to node step states
            mutableNodeStates.each { String node, MutableWorkflowNodeState nstate ->
                mutableStepStates.each { int index, MutableWorkflowStepState step ->
                    if (step.nodeStep) {
                        getOrCreateMutableNodeStepState(step, node, step.stepIdentifier)
                    }
                }
            }
        }
        if (mutableStepStates && serverNode) {
            mutableStepStates.each { int index, MutableWorkflowStepState step ->
                if (!step.nodeStep && !step.hasSubWorkflow()) {
                    //a workflow step, e.g. plugin, without a sub workflow
                    getOrCreateMutableNodeStepState(step, serverNode, step.stepIdentifier)
                }
            }
        }

    }

    MutableWorkflowStepState getAt(Integer index){
        return mutableStepStates[index-1]
    }

    @Override
    List<WorkflowStepState> getStepStates() {
        return mutableStepStates.sort().values() as List
    }

    @Override
    Map<String,? extends WorkflowNodeState> getNodeStates() {
        return mutableNodeStates
    }

    @Override
    List<String> getNodeSet() {
        return mutableNodeSet
    }

    @Override
    List<String> getAllNodes() {
        return mutableAllNodes
    }

    @Override
    synchronized void touchStateForStep(StepIdentifier identifier, int index, StepStateChange stepStateChange,
                                         Date timestamp) {
        touchWFState(identifier,timestamp)

        MutableWorkflowStepState currentStep = locateStepWithContext(identifier, index, mutableStepStates)
        transitionStateIfWaiting(identifier,currentStep.mutableStepState)
        if (identifier.context.size() - index > 1) {
            descendTouchStateForStep(currentStep, identifier, index, stepStateChange,timestamp)
            if(currentStep.ownerStepState){
                transitionStateIfWaiting(identifier,currentStep.ownerStepState.mutableStepState)
                descendTouchStateForStep(currentStep.ownerStepState, identifier, index, stepStateChange,timestamp)
            }
        }else if (stepStateChange.nodeState && currentStep.nodeStep) {
            MutableStepState toUpdate = getOrCreateMutableNodeStepState(currentStep, stepStateChange.nodeName, identifier)
            transitionStateIfWaiting(identifier,toUpdate)
        }
    }

    void updateStateForStep(StepIdentifier identifier,StepStateChange stepStateChange, Date timestamp) {
        updateStateForStep(identifier,0,stepStateChange,timestamp)
    }
    @Override
    synchronized void updateStateForStep(StepIdentifier identifier, int index,StepStateChange stepStateChange,
                                     Date timestamp) {
        touchStateForStep(identifier,index,stepStateChange,timestamp)

        MutableWorkflowStepState currentStep = locateStepWithContext(identifier, index, mutableStepStates)
        if (identifier.context.size() - index > 1) {
            descendUpdateStateForStep(currentStep, identifier, index, stepStateChange, timestamp)
            if(currentStep.ownerStepState){
                //also update state for parameterized step owner
                descendUpdateStateForStep(currentStep.ownerStepState, identifier, index, stepStateChange, timestamp)
            }
            return
        }

        //update the step found
        List<MutableStepState> toTouch=[]
        List<MutableStepState> toUpdateComplete=[]
        if(currentStep.ownerStepState){
            toTouch<<currentStep.ownerStepState.mutableStepState
        }
        if (stepStateChange.isNodeState()) {
            //find node state in stepstate
            def nodeName = stepStateChange.nodeName
            toUpdateComplete << updateNodeStepState(currentStep, nodeName, identifier, stepStateChange)

            if (currentStep.ownerStepState) {
                //also parameterized step owner
                toUpdateComplete << updateNodeStepState(currentStep.ownerStepState, nodeName, identifier,
                        stepStateChange)
            }

            if (!currentStep.nodeStep && nodeSet) {
                // change to a nodeStep since we have seen a node state for it
                if (null == currentStep.nodeStepTargets || currentStep.nodeStepTargets.size() < 1) {
                    currentStep.setNodeStepTargets(nodeSet)
                }
            }
        } else if (!currentStep.nodeStep) {
            //overall step state
            toUpdateComplete << currentStep.mutableStepState

            if(serverNode && !currentStep.hasSubWorkflow()){
                //treat server node as node owner for this step
                toUpdateComplete << updateNodeStepState(currentStep, serverNode, identifier, stepStateChange)
            }

            toUpdateComplete.each{ toup->
                updateState(identifier,toup, stepStateChange.stepState.executionState)
            }
        } else {
            toUpdateComplete << currentStep.mutableStepState
            if (nodeSet && (null == currentStep.nodeStepTargets || currentStep.nodeStepTargets.size() < 1)) {
                currentStep.setNodeStepTargets(nodeSet)
            }
        }
        transitionStateIfWaiting(identifier,currentStep.mutableStepState)
        if (currentStep.ownerStepState) {
            transitionStateIfWaiting(identifier,currentStep.ownerStepState.mutableStepState)
        }

        //update state
        toUpdateComplete*.errorMessage = stepStateChange.stepState.errorMessage
        if (stepStateChange.stepState.metadata) {
            toUpdateComplete.each {toup->
                if (null == toup.metadata) {
                    toup.metadata = [:]
                }
            }
            toUpdateComplete*.metadata << stepStateChange.stepState.metadata
        }

        toTouch.addAll(toUpdateComplete)
        toTouch.each { toup ->
            if (!toup.startTime) {
                toup.startTime = timestamp
            }
            toup.updateTime = timestamp
            if (toup.executionState.isCompletedState()) {
                toup.endTime = timestamp
            }
        }

        def nodeStepFinalizing=[currentStep]
        if(currentStep.ownerStepState){
            nodeStepFinalizing<<currentStep.ownerStepState
        }
        nodeStepFinalizing.each{ thisStep->
            if(stepStateChange.nodeState && thisStep.nodeStep
                    || !stepStateChange.nodeState && !thisStep.nodeStep && !thisStep.hasSubWorkflow() && serverNode) {
                //if it was a node state change
                //or a non-node step without a workflow (e.g. plugin), and we are treating the serverNode as the target

                if (stepStateChange.stepState.executionState.isCompletedState()) {
                    //if change state is completion:
                    finishNodeStepIfNodesFinished(thisStep, timestamp)
                } else if (thisStep.stepState.executionState.isCompletedState()
                        && stepStateChange.stepState.executionState == ExecutionState.RUNNING_HANDLER) {
                    //else if current step was completed, but step change is RUNNING_HANDLER
                    thisStep.mutableStepState.executionState = ExecutionState.RUNNING_HANDLER
                }
            }
        }

    }

    /**
     * Update a node state due to a node step state change
     * @param currentStep
     * @param nodeName
     * @param identifier
     * @param stepStateChange
     * @return
     */
    private MutableStepState updateNodeStepState(MutableWorkflowStepState currentStep, String nodeName, StepIdentifier identifier, StepStateChange stepStateChange) {
        MutableStepState toUpdate = getOrCreateMutableNodeStepState(currentStep, nodeName, identifier)
        updateState(currentStep.stepIdentifier,toUpdate, stepStateChange.stepState.executionState)
        if(stepStateChange.stepState.metadata) {
            if (toUpdate.metadata) {
                toUpdate.metadata << stepStateChange.stepState.metadata
            }else{
                toUpdate.metadata = stepStateChange.stepState.metadata
            }
        }
        if(stepStateChange.stepState.errorMessage){
            if (toUpdate.errorMessage) {
                toUpdate.errorMessage += stepStateChange.stepState.errorMessage
            } else {
                toUpdate.errorMessage = stepStateChange.stepState.errorMessage
            }
        }
        if(!toUpdate.startTime && stepStateChange.stepState.startTime){
            toUpdate.startTime = stepStateChange.stepState.startTime
        }
        if(!toUpdate.updateTime && stepStateChange.stepState.updateTime){
            toUpdate.updateTime = stepStateChange.stepState.updateTime
        }
        if(toUpdate.executionState.isCompletedState() && stepStateChange.stepState.endTime){
            toUpdate.endTime = stepStateChange.stepState.endTime
        }

        mutableNodeStates[nodeName].mutableNodeState.executionState = toUpdate.executionState

        //TODO: need to merge this data
        mutableNodeStates[nodeName].mutableNodeState.metadata = toUpdate.metadata
        mutableNodeStates[nodeName].mutableNodeState.errorMessage = toUpdate.errorMessage
        mutableNodeStates[nodeName].mutableNodeState.updateTime = toUpdate.updateTime
        mutableNodeStates[nodeName].mutableNodeState.startTime = toUpdate.startTime
        mutableNodeStates[nodeName].mutableNodeState.endTime = toUpdate.endTime

        mutableNodeStates[nodeName].lastIdentifier = identifier
        toUpdate
    }

    /**
     * For a node and step, create or return the shared node+step mutable state
     * @param currentStep
     * @param nodeName
     * @param identifier
     * @return
     */
    private MutableStepState getOrCreateMutableNodeStepState(MutableWorkflowStepState currentStep, String nodeName, StepIdentifier identifier) {
        if (null == currentStep.nodeStateMap[nodeName]) {
            //create it
            currentStep.mutableNodeStateMap[nodeName] = new MutableStepStateImpl()
        }
        //connect step-oriented state to node-oriented state
        if (null == mutableNodeStates[nodeName]) {
            mutableNodeStates[nodeName] = new MutableWorkflowNodeStateImpl(nodeName)
        }
        if (null == mutableNodeStates[nodeName].mutableStepStateMap[identifier]) {
            mutableNodeStates[nodeName].mutableStepStateMap[identifier] = currentStep.mutableNodeStateMap[nodeName]
        }
        return currentStep.mutableNodeStateMap[nodeName]
    }

/**
     * Descend into a sub workflow to update state
     * @param currentStep
     * @param identifier
     * @param stepStateChange
     * @param timestamp
     */
    private void descendUpdateStateForStep(MutableWorkflowStepState currentStep, StepIdentifier identifier, int index,StepStateChange stepStateChange, Date timestamp) {
        //recurse to the workflow list to find the right index

        MutableWorkflowState subflow = currentStep.hasSubWorkflow() ?
            currentStep.mutableSubWorkflowState :
            currentStep.createMutableSubWorkflowState(null, 0)
        //recursively update subworkflow state for the step in the subcontext
        subflow.updateStateForStep(identifier, index + 1, stepStateChange, timestamp);
    }
/**
     * Descend into a sub workflow to update state
     * @param currentStep
     * @param identifier
     * @param stepStateChange
     * @param timestamp
     */
    private void descendTouchStateForStep(MutableWorkflowStepState currentStep, StepIdentifier identifier, int index,
                                          StepStateChange stepStateChange,
                                       Date timestamp) {
        //recurse to the workflow list to find the right index

        MutableWorkflowState subflow = currentStep.hasSubWorkflow() ?
            currentStep.mutableSubWorkflowState :
            currentStep.createMutableSubWorkflowState(null, 0)
        //recursively update subworkflow state for the step in the subcontext
        subflow.touchStateForStep(identifier, index + 1,stepStateChange, timestamp);
    }

    /**
     * Finalize the execution state of a Node step, based on the collective state of all target nodes
     * @param overall
     * @param currentStep
     * @param timestamp
     * @return
     */
    private finalizeNodeStep(ExecutionState overall, MutableWorkflowStepState currentStep,Date timestamp){
        def nodeTargets = new HashSet<String>()
        if(currentStep.nodeStep){
            if(currentStep.nodeStepTargets){
                nodeTargets.addAll(currentStep.nodeStepTargets)
            }
            nodeTargets.addAll(nodeSet)
            if(currentStep.mutableNodeStateMap){
                nodeTargets.addAll(currentStep.mutableNodeStateMap.keySet())
            }
        }else{
            nodeTargets<<serverNode
        }
        def substates= nodeTargets.collect{ currentStep.nodeStateMap?.get(it)?.executionState?:null}

        ExecutionState result = summarizedSubStateResult(substates, overall)

        if(currentStep.nodeStep || currentStep.hasSubWorkflow()){
            currentStep.mutableStepState.executionState = result
        }else{
            updateState(currentStep.stepIdentifier,currentStep.mutableStepState, result)
        }
        currentStep.mutableStepState.endTime=timestamp

        //update any node states which are WAITING to NOT_STARTED
        nodeTargets.each{String node->
            if(!currentStep.mutableNodeStateMap[node]){
                currentStep.mutableNodeStateMap[node] = new MutableStepStateImpl(executionState:ExecutionState.WAITING)
            }
            MutableStepState state = currentStep.mutableNodeStateMap[node]
            if (state && state.executionState == ExecutionState.WAITING) {
                updateState(currentStep.stepIdentifier,state, ExecutionState.NOT_STARTED)
                state.endTime=timestamp
            }else if (state && (state.executionState == ExecutionState.RUNNING || state.executionState == ExecutionState.RUNNING_HANDLER)) {
                updateState(currentStep.stepIdentifier,state, ExecutionState.ABORTED)
                state.endTime=timestamp
            }
        }
    }
    /**
     * If all node step targets are completed, finalize the step state
     * @param currentStep
     * @param timestamp
     * @return
     */
    private finishNodeStepIfNodesFinished(MutableWorkflowStepState currentStep,Date timestamp){
        def nodes = currentStep.nodeStepTargets?:currentStep.mutableNodeStateMap?currentStep.mutableNodeStateMap.keySet() : [serverNode]
        boolean finished = nodes.every { node -> currentStep.nodeStateMap[node]?.executionState?.isCompletedState() }
        if (finished) {
            boolean aborted = currentStep.nodeStateMap.values()*.executionState.any { it == ExecutionState.ABORTED }
            boolean failed = currentStep.nodeStateMap.values()*.executionState.any { it == ExecutionState.FAILED }
            def overall = aborted ? ExecutionState.ABORTED : failed ? ExecutionState.FAILED : ExecutionState.SUCCEEDED
            resolveStepCompleted(overall, timestamp, currentStep)
        }
    }
    /**
     * for each parameterized context,
     * @param overall
     * @param currentStep
     * @param timestamp
     */
    private finalizeParameterizedStep(ExecutionState overall, MutableWorkflowStepState currentStep,Date timestamp){
        def substates= currentStep.parameterizedStateMap.values()*.stepState*.executionState

        ExecutionState result = summarizedSubStateResult(substates, overall)
        currentStep.mutableStepState.executionState = result
        currentStep.mutableStepState.endTime=timestamp

        //update any parameterized states which are WAITING to NOT_STARTED
        currentStep.mutableParameterizedStateMap.values().each{MutableWorkflowStepState state->

            if (state && state.mutableStepState.executionState == ExecutionState.WAITING) {
                updateState(state.stepIdentifier,state.mutableStepState, ExecutionState.NOT_STARTED)
                state.mutableStepState.endTime=timestamp
            }else if (state && (state.mutableStepState.executionState == ExecutionState.RUNNING || state
                    .mutableStepState.executionState == ExecutionState.RUNNING_HANDLER)) {
                updateState(state.stepIdentifier,state.mutableStepState, ExecutionState.ABORTED)
                state.mutableStepState.endTime=timestamp
            }
        }
        if(currentStep.nodeStep) {
            for (String node : currentStep.mutableNodeStateMap.keySet()) {
                MutableStepState nodeStepState = currentStep.mutableNodeStateMap.get(node)
                def nodeParamState = currentStep.parameterizedStateMap.get("node=${node}".toString())
                if(nodeParamState){
                    nodeStepState.executionState= nodeParamState.stepState.executionState
                    nodeStepState.endTime= nodeParamState.stepState.endTime
                    nodeStepState.updateTime= nodeParamState.stepState.updateTime
                    nodeStepState.errorMessage= nodeParamState.stepState.errorMessage
                    nodeStepState.metadata= nodeParamState.stepState.metadata
                }
            }
        }
    }

    protected ExecutionState summarizedSubStateResult(Collection<? extends ExecutionState> execStates,
                                                      ExecutionState overall) {
        boolean finished = execStates.every { it!=null && it.isCompletedState() }
        boolean aborted = execStates.any { it == ExecutionState.ABORTED }
        boolean abortedAll = execStates.every { it == ExecutionState.ABORTED }
        boolean failed = execStates.any { it == ExecutionState.FAILED }
        boolean failedAll = execStates.every { it == ExecutionState.FAILED }
        boolean succeeded = execStates.any { it == ExecutionState.SUCCEEDED }
        boolean succeededAll = execStates.every { it == ExecutionState.SUCCEEDED }
        boolean notStartedAll = execStates?.size() == 0 || execStates.every { it == ExecutionState.WAITING || it == null }
        ExecutionState result = overall
        if (finished) {
            //all nodes finished
            if (abortedAll) {
                result = ExecutionState.ABORTED
            } else if (failedAll) {
                result = ExecutionState.FAILED
            } else if (succeededAll) {
                result = ExecutionState.SUCCEEDED
            } else {
                result = ExecutionState.NODE_MIXED
            }
        } else if (aborted && !failed && !succeeded) {
            //partial aborted
            result = ExecutionState.ABORTED
        } else if (!aborted && failed && !succeeded) {
            //partial failed
            result = ExecutionState.FAILED
        } else if (!failed && !aborted && succeeded) {
            //partial success
            result = ExecutionState.NODE_PARTIAL_SUCCEEDED
        } else if (notStartedAll) {
            //not started
            result = ExecutionState.NOT_STARTED
        } else {
            result = ExecutionState.NODE_MIXED
        }
        result
    }

    private MutableWorkflowStepState locateStepWithContext(StepIdentifier identifier, int index,Map<Integer, MutableWorkflowStepState> states, boolean ignoreParameters=false) {
        MutableWorkflowStepState currentStep
        StepContextId subid = identifier.context[index]
        int ndx=subid.step-1
        if (ndx >= states.size() || null == states[ndx]) {
            states[ndx] = new MutableWorkflowStepStateImpl(StateUtils.stepIdentifier(subid))
            stepCount = states.size()
        }
        currentStep = states[ndx]
        //parameterized substep
        if (!ignoreParameters && null != subid.params && subid.aspect != StepAspect.ErrorHandler) {
            currentStep = currentStep.getParameterizedStepState(StateUtils.stepIdentifier(subid), subid.params)
        }
        currentStep
    }
    private void touchWFState(StepIdentifier identifier, Date timestamp) {
        transitionStateIfWaiting(identifier,this)
        if (null == this.updateTime || this.updateTime < timestamp) {
            this.updateTime = timestamp
        }
        if (null == this.startTime) {
            this.startTime = timestamp
        }
    }


    private void transitionStateIfWaiting(StepIdentifier stepIdentifier, MutableExecutionState target) {
        if (waitingState(target.executionState)) {
            updateState(stepIdentifier, target, ExecutionState.RUNNING)
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
    public static void updateState(StepIdentifier identifier,MutableExecutionState target, ExecutionState toState,boolean errorHandler=false) {
        def fromState=target.executionState
        if(fromState==toState){
            return
        }
        def allowed=[
                (ExecutionState.WAITING):[null,ExecutionState.WAITING],
                (ExecutionState.RUNNING): [null, ExecutionState.WAITING,ExecutionState.RUNNING],
                (ExecutionState.RUNNING_HANDLER):[null,ExecutionState.WAITING,ExecutionState.FAILED, ExecutionState.RUNNING,ExecutionState.RUNNING_HANDLER],
        ]
        ExecutionState.values().findAll{it.isCompletedState()}.each{
            allowed[it]= [it,ExecutionState.RUNNING, ExecutionState.RUNNING_HANDLER, ExecutionState.WAITING]
        }
        if (errorHandler) {
            allowed[ExecutionState.RUNNING] << ExecutionState.FAILED
        }
        if (toState == null) {
//            System.err.println("Cannot change state to ${toState}")
            throw new IllegalStateException("Cannot change state to ${toState}")
        }
        if(!(fromState in allowed[toState])){
            if (fromState.isCompletedState()) {
                return
            }
//            System.err.println("Cannot change from " + fromState + " to " + toState)
            throw new IllegalStateException("Cannot change from " + fromState + " to " + toState+": for ${identifier}: ${target}")
        }
        target.executionState = toState
    }

    @Override
    void updateWorkflowState(ExecutionState executionState, Date timestamp, List<String> nodenames) {
        updateWorkflowState(null,false,executionState,timestamp,nodenames,this)
    }
    synchronized void updateWorkflowState(StepIdentifier identifier, boolean quellFinalState, ExecutionState executionState, Date timestamp, List<String> nodenames, MutableWorkflowState parent) {
        touchWFState(identifier,timestamp)
        if (!(quellFinalState && executionState.isCompletedState())) {
            updateState(identifier,this, executionState,
                    identifier != null ? !(identifier.context.last().aspect.isMain()) : false)
        }
        if (null != nodenames && (null == mutableNodeSet || mutableNodeSet.size() < 1)) {
            mutableNodeSet = new ArrayList<>(nodenames)
            def mutableNodeStates=parent.mutableNodeStates
            def allNodes=parent.allNodes
            mutableNodeSet.each { node ->
                if(!mutableNodeStates[node]){
                    mutableNodeStates[node] = new MutableWorkflowNodeStateImpl(node)
                }
                if(!allNodes.contains(node)){
                    allNodes<<node
                }
                mutableStepStates.keySet().each {int ident->
                    if(mutableStepStates[ident].nodeStep){
                        if(null==mutableStepStates[ident].mutableNodeStateMap[node]){
                            mutableStepStates[ident].mutableNodeStateMap[node]=new MutableStepStateImpl()
                        }
                        mutableNodeStates[node].mutableStepStateMap[StateUtils.stepIdentifierAppend(identifier,StateUtils.stepIdentifier(ident + 1))]= mutableStepStates[ident].mutableNodeStateMap[node]
                    }
                }
            }
        }else if(null!=nodenames){
            def allNodes = parent.allNodes
            nodenames.each { node ->
                if (!allNodes.contains(node)) {
                    allNodes << node
                }
            }
        }
        if(executionState.isCompletedState() && !quellFinalState){
            cleanupSteps(executionState, timestamp)
            this.endTime=timestamp
        }
    }

    /**
     * Finalize all incomplete steps in the workflow with the given overall state
     * @param executionState
     * @param timestamp
     */
    private void cleanupSteps(ExecutionState executionState, Date timestamp) {
        mutableStepStates.each { i, step ->
            if (!step.stepState.executionState.isCompletedState()) {
                resolveStepCompleted(executionState, timestamp, step)
            }
        }
    }

    /**
     *
     * @param executionState
     * @param timestamp
     * @param states
     */

    /**
     * Resolve the completed state of a step based on overall workflow completion state
     * @param executionState
     * @param date
     * @param i
     * @param mutableWorkflowStepState
     */
    def resolveStepCompleted(ExecutionState executionState, Date date,  MutableWorkflowStepState mutableWorkflowStepState) {
        boolean finalized=false
        if(mutableWorkflowStepState.parameterizedStateMap){
            mutableWorkflowStepState.mutableParameterizedStateMap.values().each{MutableWorkflowStepState paramstep->
                resolveStepCompleted(executionState,date,paramstep)
            }
            finalizeParameterizedStep(executionState,mutableWorkflowStepState,date)
            finalized=true
        }else if (mutableWorkflowStepState.nodeStep){
            finalizeNodeStep(executionState,mutableWorkflowStepState,date)
            finalized=true
        }
        if(mutableWorkflowStepState.hasSubWorkflow()){
            finalizeSubWorkflowStep(mutableWorkflowStepState, executionState, date)
        } else if (!mutableWorkflowStepState.nodeStep && serverNode) {
            finalizeNodeStep(executionState, mutableWorkflowStepState, date)
            finalized=true
        }
        if(!finalized){
            finalizeStepExecutionState(mutableWorkflowStepState, executionState, date)
        }
    }

    /**
     * Finalize the execution state of a subworkflow step, finalize the sub workflow.
     * @param mutableWorkflowStepState
     * @param executionState
     * @param date
     */
    private void finalizeSubWorkflowStep(MutableWorkflowStepState mutableWorkflowStepState, ExecutionState executionState, Date date) {
        //resolve the sub workflow
        def states=[] as List<MutableWorkflowStepState>
        if(mutableWorkflowStepState.parameterizedStateMap){
            states.addAll(mutableWorkflowStepState.mutableParameterizedStateMap.values())
        }else{
            states=[mutableWorkflowStepState]
        }
        states.each{MutableWorkflowStepState step->
            step.mutableSubWorkflowState.updateSubWorkflowState(
                    step.stepIdentifier,
                    step.stepIdentifier.context.size(),
                    false,
                    executionState,
                    updateTime,
                    null,
                    this
            )
        }
        if(mutableWorkflowStepState.parameterizedStateMap){
            //use the parameterized step states to resolve the step states of the base workflow
            mutableWorkflowStepState.mutableSubWorkflowState.getStepStates().eachWithIndex{ eachstep, i ->
                def values = states*.mutableSubWorkflowState*.stepStates*.get(i)*.stepState*.executionState
                def summaryState = summarizedSubStateResult(values,executionState)
                if(eachstep.nodeStep){
                    def nodes = eachstep.nodeStepTargets?:eachstep.nodeStateMap?eachstep.nodeStateMap.keySet():[serverNode]
                    nodes.each{node->
                        mutableWorkflowStepState.mutableSubWorkflowState.updateStateForStep(eachstep.stepIdentifier,0,
                                StateUtils.stepStateChange(StateUtils.stepState(summaryState),node), updateTime)
                    }
                }
                mutableWorkflowStepState.mutableSubWorkflowState.updateStateForStep(eachstep.stepIdentifier,0,
                        StateUtils.stepStateChange(StateUtils.stepState(summaryState)), updateTime)
            }
            mutableWorkflowStepState.mutableSubWorkflowState.updateSubWorkflowState(
                    mutableWorkflowStepState.stepIdentifier,
                    mutableWorkflowStepState.stepIdentifier.context.size(),
                    false,
                    executionState,
                    updateTime,
                    null,
                    this
            )
        }
        if(!mutableWorkflowStepState.nodeStep && !mutableWorkflowStepState.parameterizedStateMap){
            //finalize this step based on the subworkflow steps
            def substates = mutableWorkflowStepState.subWorkflowState.stepStates*.stepState*.executionState
            mutableWorkflowStepState.mutableStepState.executionState = summarizedSubStateResult(substates,
                    executionState)
            mutableWorkflowStepState.mutableStepState.endTime = date
        }
    }

    /**
     * Finalize execution state only for a step
     * @param mutableWorkflowStepState
     * @param executionState
     * @param date
     */
    private void finalizeStepExecutionState(MutableWorkflowStepState mutableWorkflowStepState,
                                            ExecutionState executionState, Date date) {
        def curstate = mutableWorkflowStepState.mutableStepState.executionState
        if(null== curstate || !curstate.completedState){
            def newstate = executionState
            if ( curstate in [null,ExecutionState.WAITING]) {
                newstate = ExecutionState.NOT_STARTED
            } else if (curstate in [ExecutionState.RUNNING, ExecutionState.RUNNING_HANDLER]) {
                newstate = ExecutionState.ABORTED
            }
            updateState(mutableWorkflowStepState.stepIdentifier,mutableWorkflowStepState.mutableStepState, newstate)
        }
        mutableWorkflowStepState.mutableStepState.endTime = date
    }

    @Override
    synchronized void updateSubWorkflowState(StepIdentifier identifier, int index, boolean quellFinalState,
                                 ExecutionState executionState, Date timestamp, List<String> nodeNames, MutableWorkflowState parent) {
        touchWFState(identifier,timestamp)
        Map<Integer, MutableWorkflowStepState> states = mutableStepStates;
        if (identifier.context.size() - index > 0) {
            //descend one step
            MutableWorkflowStepState nextStep = locateStepWithContext(identifier, index, states)
            MutableWorkflowState nextWorkflow = nextStep.hasSubWorkflow() ?
                nextStep.mutableSubWorkflowState :
                nextStep.createMutableSubWorkflowState(null, 0)

            transitionStateIfWaiting(identifier,nextStep.mutableStepState)
            if (nextStep.ownerStepState) {
                transitionStateIfWaiting(identifier,nextStep.ownerStepState.mutableStepState)
            }
            //more steps to descend
            nextWorkflow.updateSubWorkflowState(identifier, index + 1, nextStep.nodeStep, executionState, timestamp, nodeNames, parent ?: this);
        } else {
            //update the workflow state for this workflow
            updateWorkflowState(identifier,  quellFinalState, executionState, timestamp, nodeNames, parent ?: this)
        }
    }

    @Override
    public java.lang.String toString() {
        return "WF{" +
                "nodes=" + mutableNodeSet +
                ", stepCount=" + stepCount +
                ", state=" + executionState +
                ", timestamp=" + updateTime +
                ", steps=" + mutableStepStates +
                '}';
    }
}
