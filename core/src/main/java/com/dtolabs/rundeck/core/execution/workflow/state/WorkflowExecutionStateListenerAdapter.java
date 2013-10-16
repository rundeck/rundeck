package com.dtolabs.rundeck.core.execution.workflow.state;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.workflow.*;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Adapts events from a {@link WorkflowExecutionListener} and sends changes to a list of {@link WorkflowStateListener}s.
 */
public class WorkflowExecutionStateListenerAdapter implements WorkflowExecutionListener {
    List<WorkflowStateListener> listeners;
    StepContextWorkflowExecutionListener<INodeEntry, Integer> stepContext;

    public WorkflowExecutionStateListenerAdapter() {
        this(new ArrayList<WorkflowStateListener>());
    }

    public WorkflowExecutionStateListenerAdapter(List<WorkflowStateListener> listeners) {
        this.listeners = listeners;
        stepContext = new StepContextWorkflowExecutionListener<INodeEntry, Integer>();
    }

    public void addWorkflowStateListener(WorkflowStateListener listener) {
        listeners.add(listener);
    }

    private void notifyAllWorkflowState(ExecutionState executionState, Date timestamp) {
        for (WorkflowStateListener listener : listeners) {
            listener.workflowExecutionStateChanged(executionState, timestamp);
        }
    }

    private void notifyAllStepState(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
        for (WorkflowStateListener listener : listeners) {
            listener.stepStateChanged(identifier, stepStateChange, timestamp);
        }
    }

    public void beginWorkflowExecution(StepExecutionContext executionContext, WorkflowExecutionItem item) {
        notifyAllWorkflowState(ExecutionState.RUNNING, new Date());
    }

    public void finishWorkflowExecution(WorkflowExecutionResult result, StepExecutionContext executionContext,
            WorkflowExecutionItem item) {
        notifyAllWorkflowState(result.isSuccess() ? ExecutionState.SUCCEEDED : ExecutionState.FAILED, new Date());
    }

    private StepIdentifier createIdentifier() {
        return new StepIdentifierImpl(stepContext.getCurrentContext());
    }

    private StepStateChange createState(ExecutionState executionState) {
        StepStateChangeImpl stepStateChange = new StepStateChangeImpl();

        INodeEntry currentNode = stepContext.getCurrentNode();

        stepStateChange.setNodeState(null != currentNode);
        if (null != currentNode) {
            stepStateChange.setNodeName(currentNode.getNodename());
        }

        StepStateImpl stepState = new StepStateImpl();
        stepState.setExecutionState(executionState);
//        stepState.setMetadata();
        stepStateChange.setStepState(stepState);
        return stepStateChange;
    }

    public void beginWorkflowItem(int step, StepExecutionItem node) {
    }

    public void finishWorkflowItem(int step, StepExecutionItem node, boolean success) {
    }

    public void beginExecuteNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node) {
        stepContext.beginNodeContext(node);
        notifyAllStepState(createIdentifier(), createState(ExecutionState.RUNNING), new Date());

    }

    public void beginStepExecution(StepExecutionContext context, StepExecutionItem item) {
        stepContext.beginStepContext(context.getStepNumber());
        notifyAllStepState(createIdentifier(), createState(ExecutionState.RUNNING), new Date());
    }

    public void finishStepExecution(StatusResult result, StepExecutionContext context, StepExecutionItem item) {
        stepContext.finishStepContext();
        notifyAllStepState(createIdentifier(), createState(stateForResult(result)), new Date());
    }

    public void finishExecuteNodeStep(NodeStepResult result, ExecutionContext context, StepExecutionItem item,
            INodeEntry node) {
        stepContext.finishNodeContext();
        notifyAllStepState(createIdentifier(), createState(stateForResult(result)), new Date());
    }

    private ExecutionState stateForResult(StatusResult result) {
        if (result.isSuccess()) {
            return ExecutionState.SUCCEEDED;
        } else {
            return ExecutionState.FAILED;
        }
    }
}
