package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcherService;
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService;
import com.dtolabs.rundeck.core.execution.service.FileCopierService;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategyService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;

/**
 * Interface for Provider Services access for the execution service types
 */
public interface IExecutionServices {

    OrchestratorService getOrchestratorService();

    WorkflowExecutionService getWorkflowExecutionService();

    WorkflowStrategyService getWorkflowStrategyService();

    StepExecutionService getStepExecutionService();

    FileCopierService getFileCopierService();

    NodeExecutorService getNodeExecutorService();

    NodeStepExecutionService getNodeStepExecutorService();

    ResourceModelSourceService getResourceModelSourceService();

    ResourceFormatParserService getResourceFormatParserService();

    ResourceFormatGeneratorService getResourceFormatGeneratorService();

    NodeDispatcherService getNodeDispatcherService();
}
