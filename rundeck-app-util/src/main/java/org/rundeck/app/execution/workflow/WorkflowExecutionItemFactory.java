package org.rundeck.app.execution.workflow;

import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData;
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData;

import java.util.List;

public interface WorkflowExecutionItemFactory {
    WorkflowExecutionItem createExecutionItemForWorkflow(WorkflowData workflow);
    List<StepExecutionItem> consolidateWorkflowSteps(List<WorkflowStepData> steps);
}
