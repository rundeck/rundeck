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

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStepState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/16/13
 * Time: 11:05 AM
 */
class MutableWorkflowStepStateImpl implements MutableWorkflowStepState {
    MutableStepState mutableStepState
    MutableWorkflowState mutableSubWorkflowState
    MutableWorkflowStepState ownerStepState
    StepIdentifier stepIdentifier;
    Map<String, MutableStepState> mutableNodeStateMap;
    Map<String, MutableWorkflowStepState> parameterizedStepStates;
    List<String> nodeStepTargets;
    boolean nodeStep;

    MutableWorkflowStepStateImpl(StepIdentifier stepIdentifier) {
        this(stepIdentifier,null)
    }
    MutableWorkflowStepStateImpl(StepIdentifier stepIdentifier,MutableWorkflowState subflow) {
        this.stepIdentifier = stepIdentifier
        this.mutableStepState=new MutableStepStateImpl()
        this.mutableNodeStateMap = Collections.synchronizedMap(new HashMap<String, MutableStepState>())
        this.parameterizedStepStates = Collections.synchronizedMap(new HashMap<String, MutableWorkflowStepState>())
        this.mutableSubWorkflowState=subflow
        this.nodeStep=false
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
        mutableSubWorkflowState!=null
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
    MutableWorkflowState createMutableSubWorkflowState(List<String> nodeSet,long count) {
        mutableSubWorkflowState = new MutableWorkflowStateImpl(nodeSet, count)
    }

    @Override
    MutableWorkflowStepState getParameterizedStepState(StepIdentifier ident,Map<String, String> params) {
        def string = StateUtils.parameterString(params)
        if(null==parameterizedStepStates[string]){
            if (null == mutableSubWorkflowState) {
                createMutableSubWorkflowState([], 1)
            }
            MutableWorkflowStepStateImpl newState = new MutableWorkflowStepStateImpl(ident,
                    new MutableWorkflowStateImpl(mutableSubWorkflowState.nodeSet, mutableSubWorkflowState.stepCount))
            newState.ownerStepState=this
            parameterizedStepStates[string]= newState
        }
        return parameterizedStepStates[string]
    }

    @Override
    Map<String, ? extends WorkflowStepState> getParameterizedStateMap() {
        return parameterizedStepStates
    }

    @Override
    Map<String, MutableWorkflowStepState> getMutableParameterizedStateMap() {
        return parameterizedStepStates
    }

    @Override
    void setNodeStepTargets(List<String> nodeset) {
        nodeStepTargets = new ArrayList<String>(nodeset)
        nodeStep=true
    }

    @Override
    public java.lang.String toString() {
        return "WFStep{" +
                "step=" + mutableStepState +
                (hasSubWorkflow()? ", sub=" + mutableSubWorkflowState :'') +
                ", id=" + stepIdentifier +
                (nodeStepTargets!=null?", targetNodes=" + nodeStepTargets :'')+
                ", nodes=" + mutableNodeStateMap +
                '}';
    }
}
