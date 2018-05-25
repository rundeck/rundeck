/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution.workflow.engine;

import com.dtolabs.rundeck.core.execution.workflow.BaseWorkflowExecutor;
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext;
import com.dtolabs.rundeck.core.rules.StateObj;
import com.dtolabs.rundeck.core.rules.WorkflowSystem;

/**
 * Successful result of a workflow step operation
 */
public class OperationCompleted implements WorkflowSystem.OperationCompleted<WFSharedContext> {
    private int stepNum;
    private BaseWorkflowExecutor.StepResultCapture stepResultCapture;
    private StateObj newState;

    OperationCompleted(
            final int stepNum,
            final StateObj newState, BaseWorkflowExecutor.StepResultCapture stepResultCapture
    )
    {

        this.stepNum = stepNum;
        this.newState = newState;
        this.stepResultCapture = stepResultCapture;
    }

    @Override
    public WFSharedContext getResult() {
        return stepResultCapture.getResultData();
    }
    public BaseWorkflowExecutor.StepResultCapture getStepResultCapture() {
        return stepResultCapture;
    }

    @Override
    public StateObj getNewState() {
        return newState;
    }

    @Override
    public String toString() {
        return "StepSuccess{" +
               "stepNum=" + stepNum +
               ", stepResultCapture=" + stepResultCapture +
               ", newState=" + newState +
               '}';
    }

    public int getStepNum() {
        return stepNum;
    }
}
