package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Emits events to the logger for workflow step/node start and finish.
 */
public class WorkflowEventLoggerListener implements WorkflowExecutionListener{
    private static final String STEP_START = "stepbegin";
    private static final String STEP_HANDLER = "handlerbegin";
    private static final String HANDLER_FINISH = "handlerend";
    private static final String STEP_FINISH = "stepend";
    private static final String NODE_START = "nodebegin";
    private static final String NODE_FINISH = "nodeend";
    ExecutionListener logger;

    public WorkflowEventLoggerListener(ExecutionListener logger) {
        this.logger = logger;
    }

    @Override
    public void beginWorkflowExecution(StepExecutionContext executionContext, WorkflowExecutionItem item) {
    }

    @Override
    public void finishWorkflowExecution(WorkflowExecutionResult result, StepExecutionContext executionContext,
            WorkflowExecutionItem item) {
    }

    @Override
    public void beginWorkflowItem(int step, StepExecutionItem item) {
        logger.event(STEP_START, null, null);
    }

    @Override
    public void beginWorkflowItemErrorHandler(int step, StepExecutionItem item) {
        logger.event(STEP_HANDLER, null, null);
    }

    private static Map resultMap(StepExecutionResult result) {
        if (null != result && result.isSuccess()) {
            return null;
        }
        HashMap hashMap = new HashMap();
        if (null != result && null != result.getFailureData()) {
            hashMap.putAll(result.getFailureData());
        }
        hashMap.put("failureReason", null != result ? result.getFailureReason().toString() : "Unknown");
        hashMap.put("executionState", null != result && result.isSuccess() ? ExecutionState.SUCCEEDED.toString() :
                ExecutionState.FAILED.toString());
        return hashMap;
    }
    @Override
    public void finishWorkflowItem(int step, StepExecutionItem item, StepExecutionResult result) {
        logger.event(STEP_FINISH,
                null != result && null != result.getFailureMessage()
                        ? result.getFailureMessage()
                        : null,
                resultMap(result));
    }

    @Override
    public void finishWorkflowItemErrorHandler(int step, StepExecutionItem item, StepExecutionResult result) {
        logger.event(HANDLER_FINISH,
                null != result && null != result.getFailureMessage()
                ? result.getFailureMessage()
                : null, resultMap(result));
    }

    @Override
    public void beginStepExecution(StepExecutor executor, StepExecutionContext context, StepExecutionItem item) {
    }

    @Override
    public void finishStepExecution(StepExecutor executor, StatusResult result, StepExecutionContext context,
            StepExecutionItem item) {
    }

    @Override
    public void beginExecuteNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node) {
        logger.event(NODE_START, null, null);
    }

    @Override
    public void finishExecuteNodeStep(NodeStepResult result, ExecutionContext context, StepExecutionItem item, INodeEntry node) {
        logger.event(NODE_FINISH, null != result && null != result.getFailureMessage() ? result.getFailureMessage() :
                null,
                resultMap(result));
    }
}
