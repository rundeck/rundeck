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
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateChange
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowNodeState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStepState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 2/6/14
 * Time: 1:08 PM
 */
class DelegateMutableWorkflowState implements MutableWorkflowState {

    MutableWorkflowState delegate

    DelegateMutableWorkflowState(MutableWorkflowState delegate) {
        this.delegate = delegate
    }

    @Override
    void setExecutionState(ExecutionState state) {
        delegate.setExecutionState(state)
    }

    @Override
    void updateStateForStep(StepIdentifier identifier, int index, StepStateChange stepStateChange, Date timestamp) {
        delegate.updateStateForStep(identifier, index, stepStateChange, timestamp)
    }

    @Override
    void updateWorkflowState(ExecutionState executionState, Date timestamp, List<String> nodeNames) {
        delegate.updateWorkflowState(executionState, timestamp, nodeNames)
    }

    @Override
    void updateSubWorkflowState(StepIdentifier identifier, int index, boolean quellFinalState, ExecutionState executionState, Date timestamp,
                                List<String> nodeNames, MutableWorkflowState parent) {
        delegate.updateSubWorkflowState(identifier, index, quellFinalState,executionState, timestamp, nodeNames, parent)
    }

    @Override
    void touchStateForStep(StepIdentifier identifier, int index, StepStateChange stepStateChange, Date timestamp) {
        delegate.touchStateForStep(identifier, index, stepStateChange, timestamp)
    }

    @Override
    Map<String, ? extends MutableWorkflowNodeState> getMutableNodeStates() {
        return delegate.getMutableNodeStates()
    }

    @Override
    List<String> getNodeSet() {
        delegate.getNodeSet()
    }

    @Override
    List<String> getAllNodes() {
        delegate.getAllNodes()
    }

    @Override
    String getServerNode() {
        delegate.getServerNode()
    }

    @Override
    long getStepCount() {
        delegate.getStepCount()
    }

    @Override
    ExecutionState getExecutionState() {
        delegate.getExecutionState()
    }

    @Override
    Date getUpdateTime() {
        delegate.updateTime
    }

    @Override
    Date getStartTime() {
        delegate.getStartTime()
    }

    @Override
    Date getEndTime() {
        delegate.endTime
    }

    @Override
    List<WorkflowStepState> getStepStates() {
        delegate.stepStates
    }

    @Override
    Map<String, ? extends WorkflowNodeState> getNodeStates() {
        delegate.nodeStates
    }
}
