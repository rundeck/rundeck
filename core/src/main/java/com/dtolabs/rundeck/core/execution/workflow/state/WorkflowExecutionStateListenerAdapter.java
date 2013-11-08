package com.dtolabs.rundeck.core.execution.workflow.state;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.*;
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.util.*;

/**
 * Adapts events from a {@link WorkflowExecutionListener} and sends changes to a list of {@link WorkflowStateListener}s.
 */
public class WorkflowExecutionStateListenerAdapter implements WorkflowExecutionListener {
    List<WorkflowStateListener> listeners;
    StepContextWorkflowExecutionListener<INodeEntry, StepContextId> stepContext;


    public WorkflowExecutionStateListenerAdapter() {
        this(new ArrayList<WorkflowStateListener>());
    }

    public WorkflowExecutionStateListenerAdapter(List<WorkflowStateListener> listeners) {
        this.listeners = listeners;
        stepContext = new StepContextWorkflowExecutionListener<INodeEntry, StepContextId>();
    }

    public void addWorkflowStateListener(WorkflowStateListener listener) {
        listeners.add(listener);
    }

    private void notifyAllWorkflowState(ExecutionState executionState, Date timestamp, Collection<String> nodenames) {
        HashSet<String> nodes=null;
        if(null!=nodenames){
            nodes= new HashSet<String>(nodenames);
        }
        for (WorkflowStateListener listener : listeners) {
            listener.workflowExecutionStateChanged(executionState, timestamp, nodes);
        }
    }

    private void notifyAllSubWorkflowState(StepIdentifier identifier, ExecutionState executionState, Date timestamp,
            Collection<String> nodenames) {

        HashSet<String> nodes = null;
        if (null != nodenames) {
            nodes = new HashSet<String>(nodenames);
        }
        for (WorkflowStateListener listener : listeners) {
            listener.subWorkflowExecutionStateChanged(identifier,executionState, timestamp, nodes);
        }
    }


    private void notifyAllStepState(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
        for (WorkflowStateListener listener : listeners) {
            listener.stepStateChanged(identifier, stepStateChange, timestamp);
        }
    }

    public void beginWorkflowExecution(StepExecutionContext executionContext, WorkflowExecutionItem item) {
        stepContext.beginContext();
        List<StepContextId> currentContext = stepContext.getCurrentContext();
        if(null==currentContext ){
            notifyAllWorkflowState(ExecutionState.RUNNING, new Date(), executionContext.getNodes().getNodeNames());
        }else{
            notifyAllSubWorkflowState(createIdentifier(), ExecutionState.RUNNING, new Date(),
                    executionContext.getNodes().getNodeNames());
        }
    }

    public void finishWorkflowExecution(WorkflowExecutionResult result, StepExecutionContext executionContext,
            WorkflowExecutionItem item) {
        List<StepContextId> currentContext = stepContext.getCurrentContext();
        if (null == currentContext || currentContext.size() < 1) {
            notifyAllWorkflowState(
                    null != result && result.isSuccess() ? ExecutionState.SUCCEEDED : ExecutionState.FAILED,
                    new Date(), null);
        }else{
            notifyAllSubWorkflowState(createIdentifier(),
                    null != result && result.isSuccess() ? ExecutionState.SUCCEEDED : ExecutionState.FAILED,
                    new Date(), null);
        }
        stepContext.finishContext();
    }
    private StepIdentifier createIdentifier() {
        return StateUtils.stepIdentifier(stepContext.getCurrentContext());
    }

    private StepStateChange createStepStateChange(ExecutionState executionState) {
        return createStepStateChange(executionState, null);
    }
    private StepStateChange createStepStateChange(ExecutionState executionState, Map metadata) {
        INodeEntry currentNode = stepContext.getCurrentNode();

        return StateUtils.stepStateChange(StateUtils.stepState(executionState,metadata), null != currentNode ? currentNode
                .getNodename() : null);
    }
    private StepStateChange createStepStateChange(StepExecutionResult result){
        INodeEntry currentNode = stepContext.getCurrentNode();
        return createStepStateChange(result, currentNode);
    }

    private StepStateChange createStepStateChange(StepExecutionResult result, INodeEntry currentNode) {
        return StateUtils.stepStateChange(
                StateUtils.stepState(
                        resultState(result),
                        resultMetadata(result),
                        resultMessage(result)
                ),
                null != currentNode ? currentNode.getNodename() : null);
    }

    private String resultMessage(StepExecutionResult result) {
        return result.getFailureMessage();
    }

    private ExecutionState resultState(StepExecutionResult result) {
        return result.isSuccess() ? ExecutionState.SUCCEEDED :
                ExecutionState.FAILED;
    }

    private Map<String, Object> resultMetadata(StepExecutionResult result) {
        if(result.isSuccess()){
            return null;
        }
        HashMap<String, Object> map = new HashMap<String, Object>();
        if(null!=result.getFailureData()) {
            map.putAll(result.getFailureData());
        }
        map.put("failureReason", result.getFailureReason().toString());
        return map;
    }

    public void beginWorkflowItem(int step, StepExecutionItem item) {
        stepContext.beginStepContext(StateUtils.stepContextId(step, false));
        notifyAllStepState(createIdentifier(), createStepStateChange(ExecutionState.RUNNING), new Date());
    }

    @Override
    public void beginWorkflowItemErrorHandler(int step, StepExecutionItem item) {
        stepContext.beginStepContext(StateUtils.stepContextId(step, true));
        HashMap<String,String> ehMap=new HashMap<String, String>();
        ehMap.put("handlerTriggered", "true");
        notifyAllStepState(createIdentifier(), createStepStateChange(ExecutionState.RUNNING_HANDLER, ehMap),
                new Date());
    }

    public void finishWorkflowItem(int step, StepExecutionItem item, StepExecutionResult result) {
        if (NodeDispatchStepExecutor.STEP_EXECUTION_TYPE.equals(item.getType())) {
            //dont notify
        } else {
            notifyAllStepState(createIdentifier(), createStepStateChange(result), new Date());
        }
        stepContext.finishStepContext();
    }

    @Override
    public void finishWorkflowItemErrorHandler(int step, StepExecutionItem item, StepExecutionResult result) {
        //dont notify
    }

    public void beginExecuteNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node) {
        stepContext.beginNodeContext(node);
        StepIdentifier identifier = createIdentifier();
        notifyAllStepState(identifier, createStepStateChange(identifier.getContext().get(0).getAspect() == StepAspect
                .Main ?
                ExecutionState.RUNNING : ExecutionState.RUNNING_HANDLER), new Date());

    }

    public void beginStepExecution(StepExecutor executor,StepExecutionContext context, StepExecutionItem item) {
    }

    public void finishStepExecution(StepExecutor executor, StatusResult result, StepExecutionContext context, StepExecutionItem item) {
    }

    public void finishExecuteNodeStep(NodeStepResult result, ExecutionContext context, StepExecutionItem item,
            INodeEntry node) {
        notifyAllStepState(createIdentifier(), createStepStateChange(result), new Date());
        stepContext.finishNodeContext();
    }

}
