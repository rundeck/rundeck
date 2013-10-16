package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/15/13
 * Time: 3:42 PM
 */
public interface MutableWorkflowState extends WorkflowState {

    void updateStateForStep(StepState stepState, Date timestamp);

    void updateWorkflowState(ExecutionState executionState, Date timestamp);
}
