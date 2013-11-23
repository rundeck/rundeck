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
