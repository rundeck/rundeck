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
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateChange
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStateListener

/**
 * Listener implementation which invokes a closure when an event is received.
 */
class WorkflowStateListenerAction implements WorkflowStateListener{
    Closure onStepStateChanged
    Closure onWorkflowExecutionStateChanged
    Closure onSubWorkflowExecutionStateChanged
    @Override
    void stepStateChanged(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
        if(null!=onStepStateChanged){
            onStepStateChanged(identifier,stepStateChange,timestamp)
        }
    }

    @Override
    void workflowExecutionStateChanged(ExecutionState executionState, Date timestamp, List<String> nodeSet) {

        if (null != onWorkflowExecutionStateChanged) {
            onWorkflowExecutionStateChanged(executionState, timestamp, nodeSet)
        }
    }

    @Override
    void subWorkflowExecutionStateChanged(StepIdentifier identifier, ExecutionState executionState, Date timestamp, List<String> nodeSet) {
        if(null!=onSubWorkflowExecutionStateChanged){
            onSubWorkflowExecutionStateChanged(identifier,executionState,timestamp,nodeSet)
        }
    }
}
