/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 11/13/13
 * Time: 11:43 AM
 */
class MutableWorkflowNodeStateImpl implements MutableWorkflowNodeState {
    String nodeName
    MutableStepState mutableNodeState
    StepIdentifier lastIdentifier;
    Map<StepIdentifier, MutableStepState> mutableStepStateMap;

    MutableWorkflowNodeStateImpl(String nodeName) {
        this.nodeName = nodeName
        mutableNodeState=new MutableStepStateImpl()
        mutableStepStateMap=new HashMap<StepIdentifier,MutableStepState>()
    }

    public StepState getNodeState() {
        return mutableNodeState
    }

    @Override
    Map<StepIdentifier, ? extends StepState> getStepStateMap() {
        return mutableStepStateMap
    }
}
