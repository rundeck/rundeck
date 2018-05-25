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

package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * $INTERFACE is ... User: greg Date: 10/17/13 Time: 10:32 AM
 */
public class EchoWFStateListener implements WorkflowStateListener{
    public void stepStateChanged(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
        System.err.println(String.format("stepStateChanged(%s,%s,%s)", identifier, stepStateChange, timestamp));
    }

    public void workflowExecutionStateChanged(ExecutionState executionState, Date timestamp, List<String> nodeSet) {
        System.err.println(String.format("workflowExecutionStateChanged(%s,%s,%s)", executionState, timestamp, nodeSet));
    }

    @Override
    public void subWorkflowExecutionStateChanged(StepIdentifier identifier, ExecutionState executionState, Date
            timestamp, List<String> nodeSet) {
        System.err.println(String.format("subWorkflowExecutionStateChanged(%s,%s,%s,%s)", identifier, executionState,
                timestamp,
                nodeSet));
    }
}
