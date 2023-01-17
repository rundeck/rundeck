package org.rundeck.app.execution.workflow;

import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData;

public interface WorkflowExecutionItemFactory {
    WorkflowExecutionItem createExecutionItemForWorkflow(WorkflowData workflow);
}
