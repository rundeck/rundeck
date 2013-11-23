package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateImpl

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/16/13
 * Time: 10:46 AM
 */
class MutableStepStateImpl implements MutableStepState {
    ExecutionState executionState;
    Map metadata;
    String errorMessage;
    Date startTime
    Date updateTime
    Date endTime

    MutableStepStateImpl() {
        executionState=ExecutionState.WAITING
    }

    @Override
    public java.lang.String toString() {
        return "step{" +
                "state=" + executionState +
                (metadata?
                ", metadata=" + metadata :'' ) +
                (errorMessage?
                ", errorMessage='" + errorMessage + '\'' :'')+
                '}';
    }
}
