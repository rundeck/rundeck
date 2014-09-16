package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.MutableExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/16/13
 * Time: 10:41 AM
 */
public interface MutableStepState extends StepState, MutableExecutionState {
    /**
     * The execution state
     * @return
     */
    void setExecutionState(ExecutionState state)

    /**
     * Any metadata
     * @return
     */
    void setMetadata(Map metadata)

    /**
     * Potential error message if state is failed
     * @return
     */
    void setErrorMessage(String message)

    /**
     * Set the start time
     * @param startTime
     */
    void setStartTime(Date startTime)

    /**
     * Set the update time
     * @param startTime
     */
    void setUpdateTime(Date startTime)

    /**
     * Set the end time
     * @param startTime
     */
    void setEndTime(Date startTime)

}
