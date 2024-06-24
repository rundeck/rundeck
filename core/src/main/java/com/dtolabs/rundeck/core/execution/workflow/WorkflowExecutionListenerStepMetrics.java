package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowExecutionListenerStepMetrics implements WorkflowExecutionListener {
    private final WorkflowMetricsWriter workflowMetricsWriter;
    static Logger workflowLogger = LoggerFactory.getLogger("org.rundeck.api.workflowstatus");

    public WorkflowExecutionListenerStepMetrics(WorkflowMetricsWriter workflowMetricsWriter) {
        this.workflowMetricsWriter = workflowMetricsWriter;
    }

    @Override
    public void beginWorkflowExecution(StepExecutionContext executionContext, WorkflowExecutionItem item) {

    }

    @Override
    public void finishWorkflowExecution(WorkflowExecutionResult result, StepExecutionContext executionContext, WorkflowExecutionItem item) {

    }

    @Override
    public void beginWorkflowItem(int step, StepExecutionItem item) {
    }

    @Override
    public void beginWorkflowItemErrorHandler(int step, StepExecutionItem item) {
    }

    @Override
    public void finishWorkflowItem(int step, StepExecutionItem item, StepExecutionResult result) {
    }

    @Override
    public void finishWorkflowItemErrorHandler(int step, StepExecutionItem item, StepExecutionResult success) {
    }

    @Override
    public void beginStepExecution(StepExecutor executor, StepExecutionContext context, StepExecutionItem item) {
        workflowMetricsWriter.markMeterStepMetric(this.getClass().getName(), "startWorkflowStepMeter");
    }

    @Override
    public void finishStepExecution(StepExecutor executor, StatusResult result, StepExecutionContext context, StepExecutionItem item) {
        if(result.isSuccess()) {
            workflowMetricsWriter.markMeterStepMetric(this.getClass().getName(), "finishWorkflowStepSucceededMeter");
        } else {
            workflowMetricsWriter.markMeterStepMetric(this.getClass().getName(), "finishWorkflowStepFailedMeter");
            if (item instanceof NodeStepExecutionItem) {
                NodeStepExecutionItem nodeStepExecutionItem = (NodeStepExecutionItem) item;
                logError(nodeStepExecutionItem.getNodeStepType(), context.getStepNumber());
            } else {
                logError(item.getType(), context.getStepNumber());
            }
        }
    }

    @Override
    public void beginExecuteNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node) {

    }

    @Override
    public void finishExecuteNodeStep(NodeStepResult result, ExecutionContext context, StepExecutionItem item, INodeEntry node) {

    }

    private void logError(String pluginName, Integer step) {
        workflowLogger.error("Step {} failed: {}", step, pluginName);
    }
}
