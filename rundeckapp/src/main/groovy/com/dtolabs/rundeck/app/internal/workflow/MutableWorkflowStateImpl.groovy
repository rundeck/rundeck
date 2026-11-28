/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.*

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/15/13
 * Time: 3:41 PM
 */
class MutableWorkflowStateImpl implements MutableWorkflowState {
    def List<String> mutableNodeSet;
    def List<String> mutableAllNodes;
    def long stepCount;
    def ExecutionState executionState;
    def Date updateTime;
    def Date startTime;
    def Date endTime;
    final Map<Integer, MutableWorkflowStepState> mutableStepStates;
    final Map<String, MutableWorkflowNodeState> mutableNodeStates;
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
        this.mutableNodeSet = new CopyOnWriteArrayList<>()
        this.mutableAllNodes = new CopyOnWriteArrayList<>()
        if (null != nodeSet) {
            this.mutableNodeSet.addAll(nodeSet)
        }
        this.mutableAllNodes.addAll(mutableNodeSet)
        this.stepCount = stepCount
        mutableStepStates = new ConcurrentHashMap<>(steps ?: [:])
        for (int i = 1; i <= stepCount; i++) {
            mutableStepStates.computeIfAbsent(
                    i - 1,
                    {
                        new MutableWorkflowStepStateImpl(
                                StateUtils.stepIdentifierAppend(
                                        parentStepId,
                                        StateUtils.stepIdentifier(i)
                                )
                        )
                    }
            )
        }
        this.executionState = ExecutionState.WAITING
        mutableNodeStates = new ConcurrentHashMap<>()
        initNodeStepStates()
        if (mutableStepStates && serverNode) {
            mutableStepStates.each { int index, MutableWorkflowStepState step ->
                if (!step.nodeStep && !step.hasSubWorkflow()) {
                    //a workflow step, e.g. plugin, without a sub workflow
                    getOrCreateMutableNodeStepState(step, getRunnerNode(step), step.stepIdentifier)
                }
            }
        }

    }

    /**
     * init node states and associate with node step states of steps
     */
    void initNodeStepStates() {
        mutableAllNodes.each { node ->
            mutableNodeStates.putIfAbsent(node, new MutableWorkflowNodeStateImpl(node))
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

        MutableWorkflowStepState currentStep = locateStepWithContext(identifier, index)
        transitionStateIfWaiting(identifier, currentStep.mutableStepState)
        if (identifier.context.size() - index > 1) {
            descendTouchStateForStep(currentStep, identifier, index, stepStateChange,timestamp)

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

        MutableWorkflowStepState currentStep = locateStepWithContext(identifier, index)
        if (identifier.context.size() - index > 1) {
            descendUpdateStateForStep(currentStep, identifier, index, stepStateChange, timestamp)
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

            if (!currentStep.nodeStep) {
                // change to a nodeStep since we have seen a node state for it.
                // The node set can be empty, e.g. for the parameterized copy of a pre-built job reference
                // workflow whose node filter could not be resolved up front (#10243); target the seen node
                // so the step is still finalized when its nodes finish instead of being left RUNNING
                if (null == currentStep.nodeStepTargets || currentStep.nodeStepTargets.size() < 1) {
                    currentStep.setNodeStepTargets(nodeSet ?: [nodeName])
                }
            }
        } else if (!currentStep.nodeStep) {
            //overall step state
            toUpdateComplete << currentStep.mutableStepState
            String runnerNode = getRunnerNode(currentStep)
            if(runnerNode && !currentStep.hasSubWorkflow()){
                //treat server node as node owner for this step
                toUpdateComplete << updateNodeStepState(currentStep, runnerNode, identifier, stepStateChange)
            }

            toUpdateComplete.each{ toup->
                updateState(identifier,toup, stepStateChange.stepState.executionState)
            }
        } else {
            boolean skipped = stepStateChange.stepState.executionState == ExecutionState.SKIPPED
            //a skip reported inside one parameterized invocation (one node of a node step job reference) is
            //replayed into the shared sub workflow, where it must only apply to that invocation's node
            String invocationNode = skipped ? parameterizedInvocationNode(identifier, index) : null
            if (!invocationNode) {
                toUpdateComplete << currentStep.mutableStepState
            }
            if (nodeSet && (null == currentStep.nodeStepTargets || currentStep.nodeStepTargets.size() < 1)) {
                currentStep.setNodeStepTargets(nodeSet)
            }
            if (skipped) {
                //the engine skipped this whole node step without running it on any node, so it is
                //skipped on every node it targets, or only on the invocation's node when scoped to one.
                //A scoped skip leaves the step state to be resolved from all of its node states.
                List<String> skipNodes = invocationNode ? [invocationNode] : (currentStep.nodeStepTargets ?: nodeSet)
                skipNodes?.each { String targetNode ->
                    toUpdateComplete << updateNodeStepState(currentStep, targetNode, identifier, stepStateChange)
                }
                if (!skipNodes && !invocationNode) {
                    //no known targets, e.g. a prebuilt job reference whose node filter could not be resolved:
                    //there is no node to skip it on, so mark the step itself skipped
                    updateState(identifier, currentStep.mutableStepState, ExecutionState.SKIPPED)
                }
                finishNodeStepIfNodesFinished(currentStep, timestamp)
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

        //a node step with node states already propagated the skip in finishNodeStepIfNodesFinished, so only
        //propagate here for other steps, otherwise nested job references repeat the work at every level
        if (!stepStateChange.nodeState && stepStateChange.stepState.executionState == ExecutionState.SKIPPED
                && !(currentStep.nodeStep && currentStep.nodeStateMap)) {
            skipSubWorkflowSteps(currentStep, timestamp)
        }
    }

    /**
     * Find the node of the parameterized invocation, if any, that an update identified by the context
     * entries above the given index belongs to, e.g. "node a" for step context [1@node=a, 1].
     *
     * @param identifier step identifier
     * @param index index of the context entry being updated in the current workflow
     * @return the node name of the enclosing parameterized invocation, or null when there is none
     */
    private static String parameterizedInvocationNode(StepIdentifier identifier, int index) {
        for (int i = index - 1; i >= 0; i--) {
            String node = identifier.context[i].params?.get('node')
            if (node) {
                return node
            }
        }
        return null
    }

    /**
     * When a step that contains a prebuilt sub workflow (e.g. a job reference) is skipped, the steps of
     * that sub workflow never run, so skip them too, recursively. Otherwise they stay WAITING or are
     * finalized as NOT_STARTED, which leaves the parent step and node summaries incomplete.
     *
     * @param skippedStep the step that was skipped
     * @param timestamp time of the skip
     */
    private void skipSubWorkflowSteps(MutableWorkflowStepState skippedStep, Date timestamp) {
        //a parameterized step has one sub workflow per parameter set
        Collection<MutableWorkflowStepState> owners = skippedStep.parameterizedStateMap ?
                skippedStep.mutableParameterizedStateMap.values() :
                [skippedStep]
        owners.each { MutableWorkflowStepState owner ->
            if (!owner.hasSubWorkflow()) {
                return
            }
            MutableWorkflowState subflow = owner.mutableSubWorkflowState
            subflow.stepStates.each { WorkflowStepState child ->
                //job reference sub steps use local identifiers, so address the child by its own (last) context
                //entry rather than the owner's depth. The sub step's own update recurses into its sub
                //workflow, and fans out to its nodes
                subflow.updateStateForStep(
                        child.stepIdentifier,
                        child.stepIdentifier.context.size() - 1,
                        StateUtils.stepStateChange(StateUtils.stepState(ExecutionState.SKIPPED)),
                        timestamp
                )
            }
            //the container itself must complete: normal cleanup cannot, as the parent step is already SKIPPED.
            //The children keep their SKIPPED states unless a real result arrives later (see the allowed table in updateState)
            subflow.updateSubWorkflowState(
                    owner.stepIdentifier,
                    owner.stepIdentifier.context.size(),
                    false,
                    ExecutionState.SUCCEEDED,
                    timestamp,
                    null,
                    this
            )
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
    private MutableStepState getOrCreateMutableNodeStepState(
            MutableWorkflowStepState currentStep,
            String nodeName,
            StepIdentifier identifier
    ) {
        def stepState = currentStep.getOrCreateMutableNodeState(nodeName)
        //connect step-oriented state to node-oriented state
        mutableNodeStates.computeIfAbsent(
                nodeName,
                {
                    new MutableWorkflowNodeStateImpl(nodeName)
                }
        )
        mutableNodeStates[nodeName].mutableStepStateMap.putIfAbsent(
                identifier,
                stepState
        )
        stepState
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
            currentStep.getOrCreateMutableSubWorkflowState(null, 0)
        //recursively update subworkflow state for the step in the subcontext
        subflow.updateStateForStep(identifier, index + 1, stepStateChange, timestamp);
        if (currentStep.ownerStepState) {
            //also update state for parameterized step owner
            List<String> skipTargets = invocationSkipTargets(currentStep, identifier, index, stepStateChange)
            if (skipTargets) {
                //replay the skip into the shared sub workflow only for the nodes this invocation targets
                skipTargets.each { String node ->
                    descendUpdateStateForStep(
                            currentStep.ownerStepState,
                            identifier,
                            index,
                            StateUtils.stepStateChange(StateUtils.stepState(ExecutionState.SKIPPED), node),
                            timestamp
                    )
                }
            } else {
                descendUpdateStateForStep(currentStep.ownerStepState, identifier, index, stepStateChange, timestamp)
            }
        }
    }

    /**
     * A step skipped inside one parameterized invocation (for example 1@node=a/1) is also replayed into the
     * shared sub workflow of the parameterized step's owner. When the skipped step is a node step there, the
     * skip applies only to the invocation's node when the child runs there, or else to the nodes the
     * invocation's own sub workflow targets, e.g. a job reference run on localhost whose child targets node-0.
     *
     * @param invocation the parameterized copy of the step being descended into
     * @param identifier step identifier of the step being updated
     * @param index index of the invocation's context entry
     * @param stepStateChange the state change being replayed
     * @return the nodes to skip the shared node step on, or empty when the change is replayed unchanged
     */
    private static List<String> invocationSkipTargets(
            MutableWorkflowStepState invocation,
            StepIdentifier identifier,
            int index,
            StepStateChange stepStateChange
    ) {
        if (stepStateChange.nodeState ||
            stepStateChange.stepState.executionState != ExecutionState.SKIPPED ||
            identifier.context.size() - index != 2 ||
            !invocation.hasSubWorkflow()) {
            return []
        }
        MutableWorkflowState shared = invocation.ownerStepState.hasSubWorkflow() ?
                invocation.ownerStepState.mutableSubWorkflowState : null
        MutableWorkflowStepState sharedChild =
                (shared instanceof MutableWorkflowStateImpl) ?
                ((MutableWorkflowStateImpl) shared).mutableStepStates[identifier.context[index + 1].step - 1] : null
        if (!sharedChild?.nodeStep) {
            return []
        }
        List<String> targets = invocation.mutableSubWorkflowState.nodeSet ?: []
        String invocationNode = identifier.context[index].params?.get('node')
        //the child runs on the invocation's own node when that is one of its targets, otherwise on its targets
        if (invocationNode && (targets.isEmpty() || targets.contains(invocationNode))) {
            return [invocationNode]
        }
        return new ArrayList<String>(targets)
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
        currentStep.touchStateForSubStep(identifier, index, stepStateChange, timestamp)
        if (currentStep.ownerStepState) {
            transitionStateIfWaiting(identifier, currentStep.ownerStepState.mutableStepState)
            descendTouchStateForStep(currentStep.ownerStepState, identifier, index, stepStateChange, timestamp)
        }
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
            nodeTargets<<getRunnerNode(currentStep)
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

        String runnerNode = getRunnerNode(currentStep)
        def nodes = currentStep.nodeStepTargets?:currentStep.mutableNodeStateMap?currentStep.mutableNodeStateMap.keySet() : [runnerNode]
        if(currentStep.nodeStateMap.keySet().size()>nodes.size()) {
            nodes = currentStep.nodeStateMap.keySet().
                findAll { currentStep.nodeStateMap[it].executionState != ExecutionState.WAITING }
        }
        boolean finished = nodes.every { node -> currentStep.nodeStateMap[node]?.executionState?.isCompletedState() }
        if (finished) {
            boolean aborted = currentStep.nodeStateMap.values()*.executionState.any { it == ExecutionState.ABORTED }
            boolean failed = currentStep.nodeStateMap.values()*.executionState.any { it == ExecutionState.FAILED }
            //a step whose nodes were all skipped is itself skipped rather than succeeded
            boolean skippedAll = currentStep.nodeStateMap &&
                                 currentStep.nodeStateMap.values()*.executionState.every { it == ExecutionState.SKIPPED }
            def overall = aborted ? ExecutionState.ABORTED :
                          failed ? ExecutionState.FAILED :
                          skippedAll ? ExecutionState.SKIPPED :
                          ExecutionState.SUCCEEDED
            if (overall == ExecutionState.SKIPPED && currentStep.hasSubWorkflow()) {
                //every node skipped the step, so its prebuilt sub workflow steps never run either
                skipSubWorkflowSteps(currentStep, timestamp)
            }
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
        //SKIPPED states are neutral: they do not count for or against success, failure or abort
        boolean skippedAll = execStates.size() > 0 && execStates.every { it == ExecutionState.SKIPPED }
        def considered = execStates.findAll { it != ExecutionState.SKIPPED }
        boolean aborted = considered.any { it == ExecutionState.ABORTED }
        boolean abortedAll = considered.every { it == ExecutionState.ABORTED }
        boolean failed = considered.any { it == ExecutionState.FAILED }
        boolean failedAll = considered.every { it == ExecutionState.FAILED }
        boolean succeeded = considered.any { it == ExecutionState.SUCCEEDED }
        boolean succeededAll = considered.every { it == ExecutionState.SUCCEEDED }
        boolean notStartedAll = execStates?.size() == 0 || execStates.every { it == ExecutionState.WAITING || it == null }
        ExecutionState result = overall
        if (finished) {
            //all nodes finished
            if (skippedAll) {
                result = ExecutionState.SKIPPED
            } else if (abortedAll) {
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

    private MutableWorkflowStepState locateStepWithContext(
            StepIdentifier identifier,
            int index,
            boolean ignoreParameters = false,
            List<String> nodeNames = []
    ) {
        StepContextId subid = identifier.context[index]
        int ndx=subid.step-1
        mutableStepStates.computeIfAbsent(ndx, { new MutableWorkflowStepStateImpl(StateUtils.stepIdentifier(subid)) })
        MutableWorkflowStepState currentStep = mutableStepStates[ndx]
        stepCount = mutableStepStates.size()

        //parameterized substep
        if (!ignoreParameters && subid.aspect != StepAspect.ErrorHandler) {
            currentStep.updateMutableSubWorkflowStateNodeSet(nodeNames)
            if(null != subid.params) {
                currentStep = currentStep.getParameterizedStepState(StateUtils.stepIdentifier(subid), subid.params, nodeNames)
            }
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
        //NOT_STARTED is an inferred state: it is assigned at finalization to any node/step
        //that had no observed activity yet. A genuine state event that arrives afterwards
        //(e.g. a node step completion delivered just after the workflow is reported complete)
        //must be allowed to override it, otherwise the step is shown as "not started" even
        //though it actually ran and produced output (RUN-2939).
        def allowed=[
                (ExecutionState.WAITING):[null,ExecutionState.WAITING],
                (ExecutionState.RUNNING): [null, ExecutionState.WAITING,ExecutionState.RUNNING, ExecutionState.NOT_STARTED],
                (ExecutionState.RUNNING_HANDLER):[null,ExecutionState.WAITING,ExecutionState.FAILED, ExecutionState.RUNNING,ExecutionState.RUNNING_HANDLER, ExecutionState.NOT_STARTED],
        ]
        ExecutionState.values().findAll{it.isCompletedState()}.each{
            allowed[it]= [it,ExecutionState.RUNNING, ExecutionState.RUNNING_HANDLER, ExecutionState.WAITING, ExecutionState.NOT_STARTED]
        }
        //SKIPPED, like every completed state, is allowed from RUNNING on purpose: updateStateForStep first
        //touches the step (WAITING -> RUNNING) before applying the reported state, so even a step that was
        //skipped without ever running is RUNNING when its SKIPPED arrives. Do not narrow this list.
        //A skip can also be the first thing reported for a step, before any WAITING state was observed.
        allowed[ExecutionState.SKIPPED] = allowed[ExecutionState.SKIPPED] + [null]
        //a SKIPPED state can be replayed from one parameterized invocation into a step state that is shared with
        //other invocations, so a real result reported by another invocation must override it. A late RUNNING
        //event still does not.
        //So a real result always beats SKIPPED, whichever arrives first: SKIPPED is not allowed over a completed
        //state (that skip is ignored below), while every completed state is allowed over SKIPPED. Real results do
        //not replace each other. There is no counting of parameterized copies, so while another invocation is still
        //RUNNING, a skip reported by one invocation shows the shared state as SKIPPED until that invocation completes.
        //The per-node states stay exact, and summaries are computed from those.
        //The list is broad on purpose: it includes NOT_STARTED and NODE_MIXED, which are also completed states.
        ExecutionState.values().findAll { it.isCompletedState() && it != ExecutionState.SKIPPED }.each {
            allowed[it] = allowed[it] + [ExecutionState.SKIPPED]
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
            mutableNodeSet = new CopyOnWriteArrayList<>(nodenames)
            def mutableNodeStates=parent.mutableNodeStates
            def allNodes=parent.allNodes
            mutableNodeSet.each { node ->
                mutableNodeStates.computeIfAbsent(node, { new MutableWorkflowNodeStateImpl(node) })
                def nodeState = mutableNodeStates[node]
                if(!allNodes.contains(node)){
                    allNodes<<node
                }
                mutableStepStates.each { int ident, MutableWorkflowStepState wfStepState ->
                    if (wfStepState.nodeStep) {
                        def wfnodestate = wfStepState.getOrCreateMutableNodeState(node)
                        def newIdent = StateUtils.stepIdentifierAppend(identifier, StateUtils.stepIdentifier(ident + 1))
                        nodeState.mutableStepStateMap.putIfAbsent(newIdent, wfnodestate)
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

        String runnerNode = getRunnerNode(mutableWorkflowStepState)
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
        } else if (!mutableWorkflowStepState.nodeStep && runnerNode) {
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
                    def nodes = eachstep.nodeStepTargets?:eachstep.nodeStateMap?eachstep.nodeStateMap.keySet():[getRunnerNode(eachstep)]
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
        if (identifier.context.size() - index > 0) {
            //descend one step
            MutableWorkflowStepState nextStep = locateStepWithContext(identifier, index, false, nodeNames)
            MutableWorkflowState nextWorkflow = nextStep.hasSubWorkflow() ?
                nextStep.mutableSubWorkflowState :
                nextStep.getOrCreateMutableSubWorkflowState(null, 0)

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

    /**
     * Returns the runner node configured for the given workflow step. If the step's runner node is not set,
     * this method falls back to returning the server node.
     *
     * @param currentStep the current workflow step state
     * @return the name of the runner node for the step, or the server node if not specified
     */
    @Override
    String getRunnerNode(WorkflowStepState currentStep){
        return currentStep.runnerNode?:serverNode
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
