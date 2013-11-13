package com.dtolabs.rundeck.core.execution.workflow.state;

import com.dtolabs.rundeck.core.utils.PairImpl;

import java.util.*;

/**
 * $INTERFACE is ... User: greg Date: 10/17/13 Time: 12:36 PM
 */
public class StateUtils {

    public static StepState stepState(ExecutionState state) {
        return stepState(state, null, null, null, null, null);
    }

    public static StepState stepState(ExecutionState state, Map metadata) {
        return stepState(state, metadata, null,null,null,null);
    }

    public static StepState stepState(ExecutionState state, Map metadata, String errorMessage) {
        return stepState(state, metadata, errorMessage, null, null, null);
    }
    public static StepState stepState(ExecutionState state, Map metadata, String errorMessage,
            Date startTime,
            Date updateTime, Date endTime) {

        StepStateImpl stepState = new StepStateImpl();
        stepState.setExecutionState(state);
        stepState.setMetadata(metadata);
        stepState.setErrorMessage(errorMessage);
        stepState.setStartTime(startTime);
        stepState.setUpdateTime(updateTime);
        stepState.setEndTime(endTime);
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
        stepStateChange.setNodeState(null!=nodeName);
        stepStateChange.setNodeName(nodeName);
        return stepStateChange;
    }

    public static class CtxItem extends PairImpl<Integer, Boolean> implements StepContextId {
        public CtxItem(Integer first, Boolean second) {
            super(first, second);
        }

        public int getStep() {
            return getFirst();
        }

        @Override
        public StepAspect getAspect() {
            return getSecond() ? StepAspect.ErrorHandler : StepAspect.Main;
        }
    }
    public static StepContextId stepContextId(int step, boolean errorhandler) {
        return new CtxItem(step, errorhandler);
    }
    public static StepIdentifier stepIdentifier(List<StepContextId> context) {
        return new StepIdentifierImpl(context);
    }
    public static StepIdentifier stepIdentifier(StepContextId... context) {
        return new StepIdentifierImpl(Arrays.asList(context));
    }
    public static StepIdentifier stepIdentifier(int id) {
        return stepIdentifier(stepContextId(id,false));
    }
    public static StepIdentifier stepIdentifier(int... ids) {
        return stepIdentifier(asStepContextIds(ids));
    }
    public static StepContextId last(StepIdentifier stepIdentifier) {
        return stepIdentifier.getContext().get(stepIdentifier.getContext().size() - 1);
    }
    public static StepContextId first(StepIdentifier stepIdentifier) {
        return stepIdentifier.getContext().get(0);
    }

    private static List<StepContextId> asStepContextIds(int[] ids) {
        ArrayList<StepContextId> stepContextIds = new ArrayList<StepContextId>(ids.length);
        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
            stepContextIds.add(stepContextId(id,false));
        }
        return stepContextIds;
    }


    public static StepIdentifier stepIdentifierTail(StepIdentifier identifier) {
        return new StepIdentifierImpl(identifier.getContext().subList(1, identifier.getContext().size()));
    }


    public static WorkflowState workflowState(List<String> nodeSet, long stepCount, ExecutionState executionState,
            Date timestamp,
            Date startTime,
            Date endTime,
            ArrayList<WorkflowStepState> stepStates) {
        return new WorkflowStateImpl(nodeSet, stepCount, executionState, timestamp, startTime, endTime, stepStates);
    }

    public static WorkflowStepState workflowStepState(StepState stepState, Map<String, StepState> nodeStateMap,
            StepIdentifier stepIdentifier, WorkflowState subWorkflowState, List<String> nodeStepTargets, boolean nodeStep) {
        WorkflowStepStateImpl workflowStepState = new WorkflowStepStateImpl();
        workflowStepState.setStepState(stepState);
        workflowStepState.setNodeStateMap(nodeStateMap);
        workflowStepState.setStepIdentifier(stepIdentifier);
        workflowStepState.setSubWorkflow(null != subWorkflowState);
        workflowStepState.setSubWorkflowState(subWorkflowState);
        if(null!=nodeStepTargets){
            workflowStepState.setNodeStepTargets(nodeStepTargets);
        }
        workflowStepState.setNodeStep(nodeStep);
        return workflowStepState;
    }
}
