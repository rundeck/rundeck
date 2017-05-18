/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
import com.dtolabs.rundeck.core.execution.workflow.ControlBehavior;
import com.dtolabs.rundeck.core.execution.workflow.EngineWorkflowExecutor;
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.rules.*;

import java.util.Set;

/**
 * operation for running a step
 */
public class StepOperation implements WorkflowSystem.Operation<WFSharedContext,OperationCompleted> {
    private int stepNum;
    private String label;
    private Set<Condition> startTriggerConditions;
    private Set<Condition> skipTriggerConditions;
    private StepCallable callable;
    private StateObj startTriggerState;
    private StateObj skipTriggerState;
    private boolean didRun = false;

    public StepOperation(
            final int stepNum,
            final String label,
            final StepCallable callable,
            final StateObj startTriggerState,
            final StateObj skipTriggerState,
            final Set<Condition> startTriggerConditions,
            final Set<Condition> skipTriggerConditions
    )
    {
        this.stepNum = stepNum;
        this.label = label;
        this.callable = callable;
        this.startTriggerState = startTriggerState;
        this.startTriggerConditions = startTriggerConditions;
        this.skipTriggerConditions = skipTriggerConditions;
        this.skipTriggerState = skipTriggerState;
    }

    @Override
    public boolean shouldRun(final StateObj state) {
        return state.hasState(startTriggerState);
    }

    @Override
    public boolean shouldSkip(final StateObj state) {
        return null != skipTriggerState && state.hasState(skipTriggerState);
    }

    @Override
    public OperationCompleted apply(WFSharedContext context)  {
        didRun = true;
        BaseWorkflowExecutor.StepResultCapture stepResultCapture = callable.apply(context);
        StepExecutionResult result = stepResultCapture.getStepResult();
        ControlBehavior controlBehavior = stepResultCapture.getControlBehavior();
        String statusString = stepResultCapture.getStatusString();


        MutableStateObj stateChanges = States.mutable();
        boolean success = null != result && result.isSuccess();
        if (result != null) {
            EngineWorkflowExecutor.updateStateWithStepResultData(
                    stateChanges,
                    stepNum,
                    result.getFailureData()
            );
        }
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_COMPLETED_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_TRUE
        );
        String stepResultValue = success
                                 ? EngineWorkflowExecutor.STEP_STATE_RESULT_SUCCESS
                                 : EngineWorkflowExecutor.STEP_STATE_RESULT_FAILURE;
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_STATE_KEY, stepNum),
                stepResultValue
        );
        if (label != null) {
            stateChanges.updateState(
                    EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_STATE_KEY, "label." + label),
                    stepResultValue
            );
            stateChanges.updateState(
                    EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_COMPLETED_KEY, "label." + label),
                    EngineWorkflowExecutor.VALUE_TRUE
            );
            if (result != null) {
                EngineWorkflowExecutor.updateStateWithStepResultData(
                        stateChanges,
                        "label." + label,
                        result.getFailureData()
                );
            }
        }
        if (success) {
            stateChanges.updateState(
                    EngineWorkflowExecutor.STEP_ANY_STATE_SUCCESS_KEY,
                    EngineWorkflowExecutor.VALUE_TRUE
            );
        } else {
            stateChanges.updateState(
                    EngineWorkflowExecutor.STEP_ANY_STATE_FAILED_KEY,
                    EngineWorkflowExecutor.VALUE_TRUE
            );
        }
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_BEFORE_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_FALSE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_AFTER_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_TRUE
        );

        if (controlBehavior != null) {
            stateChanges.updateState(EngineWorkflowExecutor.stepKey(
                    EngineWorkflowExecutor.STEP_FLOW_CONTROL_KEY,
                    stepNum
            ), controlBehavior.toString());
            if (controlBehavior == ControlBehavior.Halt) {
                stateChanges.updateState(
                        EngineWorkflowExecutor.STEP_ANY_FLOW_CONTROL_HALT_KEY,
                        EngineWorkflowExecutor.VALUE_TRUE
                );
            }
            if (null != statusString) {
                stateChanges.updateState(EngineWorkflowExecutor.stepKey(
                        EngineWorkflowExecutor.STEP_FLOW_CONTROL_STATUS_KEY,
                        stepNum
                ), statusString);
            }
        }

        return new OperationCompleted(stepNum, stateChanges, stepResultCapture);
    }

    @Override
    public StateObj getSkipState(final StateObj state) {
        MutableStateObj stateChanges = States.mutable();
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_COMPLETED_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_FALSE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_STATE_KEY, stepNum),
                EngineWorkflowExecutor.STEP_STATE_RESULT_SKIPPED
        );
        stateChanges.updateState(EngineWorkflowExecutor.STEP_ANY_STATE_SKIPPED_KEY, EngineWorkflowExecutor.VALUE_TRUE);
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_BEFORE_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_FALSE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_AFTER_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_TRUE
        );
        return stateChanges;
    }

    @Override
    public StateObj getFailureState(final Throwable t) {
        MutableStateObj stateChanges = States.mutable();
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_COMPLETED_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_TRUE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_STATE_KEY, stepNum),
                EngineWorkflowExecutor.STEP_STATE_RESULT_FAILURE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_BEFORE_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_FALSE
        );
        stateChanges.updateState(
                EngineWorkflowExecutor.stepKey(EngineWorkflowExecutor.STEP_AFTER_KEY, stepNum),
                EngineWorkflowExecutor.VALUE_TRUE
        );
        return stateChanges;
    }

    public boolean isDidRun() {
        return didRun;
    }

    @Override
    public String toString() {
        return "EngineWorkflowStepOperation{" +
               "stepNum=" + stepNum +
               ", label='" + label + '\'' +
               '}';
    }

    public int getStepNum() {
        return stepNum;
    }

    public Set<Condition> getStartTriggerConditions() {
        return startTriggerConditions;
    }

    public Set<Condition> getSkipTriggerConditions() {
        return skipTriggerConditions;
    }
}
