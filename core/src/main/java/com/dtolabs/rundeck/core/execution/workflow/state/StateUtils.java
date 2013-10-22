package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.*;

/**
 * $INTERFACE is ... User: greg Date: 10/17/13 Time: 12:36 PM
 */
public class StateUtils {

    public static StepState stepState(ExecutionState state) {
        return stepState(state, null, null);
    }

    public static StepState stepState(ExecutionState state, Map metadata) {
        return stepState(state, metadata, null);
    }

    public static StepState stepState(ExecutionState state, Map metadata, String errorMessage) {
        StepStateImpl stepState = new StepStateImpl();
        stepState.setExecutionState(state);
        stepState.setMetadata(metadata);
        stepState.setErrorMessage(errorMessage);
        return stepState;
    }

    public static StepStateChange stepStateChange(StepState state) {
        StepStateChangeImpl stepStateChange = new StepStateChangeImpl();
        stepStateChange.setStepState(state);
        stepStateChange.setNodeState(false);
        return stepStateChange;
    }

    public static StepStateChange stepStateChange(StepState state, String nodeName) {
        StepStateChangeImpl stepStateChange = new StepStateChangeImpl();
        stepStateChange.setStepState(state);
        stepStateChange.setNodeState(true);
        stepStateChange.setNodeName(nodeName);
        return stepStateChange;
    }

    public static StepIdentifier stepIdentifier(List<Integer> context) {
        return new StepIdentifierImpl(context);
    }
    public static StepIdentifier stepIdentifierTail(StepIdentifier identifier) {
        return new StepIdentifierImpl(identifier.getContext().subList(1, identifier.getContext().size()));
    }

    public static StepIdentifier stepIdentifier(Integer... context) {
        return new StepIdentifierImpl(Arrays.asList(context));
    }

    public static WorkflowState workflowState(HashSet<String> nodeSet, long stepCount, ExecutionState executionState,
            Date timestamp,
            ArrayList<WorkflowStepState> stepStates) {
        return new WorkflowStateImpl(nodeSet, stepCount, executionState, timestamp, stepStates);
    }

    public static WorkflowStepState workflowStepState(StepState stepState, Map<String, StepState> nodeStateMap,
            StepIdentifier stepIdentifier, WorkflowState subWorkflowState, Set<String> nodeStepTargets) {
        WorkflowStepStateImpl workflowStepState = new WorkflowStepStateImpl();
        workflowStepState.setStepState(stepState);
        workflowStepState.setNodeStateMap(nodeStateMap);
        workflowStepState.setStepIdentifier(stepIdentifier);
        workflowStepState.setSubWorkflow(null != subWorkflowState);
        workflowStepState.setSubWorkflowState(subWorkflowState);
        workflowStepState.setNodeStepTargets(nodeStepTargets);
        return workflowStepState;
    }
}
