/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateChange

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 2/6/14
 * Time: 4:25 PM
 */
class LogMutableWorkflowState extends DelegateMutableWorkflowState {
    def List<Map> stateChanges = Collections.<Map>synchronizedList([])

    LogMutableWorkflowState(MutableWorkflowState delegate) {
        super(delegate)
    }

    private void addStateChange(Date timestamp, String type, Map data) {
        this.stateChanges << [type: type, timestamp: timestamp] + data
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
                executionState: stepState.executionState,
                meta: stepState.metadata,
        ]
    }

    @Override
    void updateSubWorkflowState(StepIdentifier identifier, int index, boolean quellFinalState, ExecutionState executionState, Date timestamp,
                                List<String> nodeNames, MutableWorkflowState parent) {
        addStateChange(timestamp, "updateSubWorkflowState", [identifier: identifier, index: index,
                executionState: executionState, nodeNames: nodeNames, quellFinalState:quellFinalState])
        super.updateSubWorkflowState(identifier, index, quellFinalState, executionState, timestamp, nodeNames, parent)
    }

    @Override
    void updateStateForStep(StepIdentifier identifier, int index, StepStateChange stepStateChange, Date timestamp) {
        addStateChange(timestamp, "updateStateForStep", asChangeMap(stepStateChange) + [identifier: identifier])
        super.updateStateForStep(identifier, index, stepStateChange, timestamp)
    }

    @Override
    void updateWorkflowState(ExecutionState executionState, Date timestamp, List<String> nodeNames) {
        addStateChange(timestamp, "updateWorkflowState", [executionState: executionState, nodeNames: nodeNames])
        super.updateWorkflowState(executionState, timestamp, nodeNames)
    }
}
