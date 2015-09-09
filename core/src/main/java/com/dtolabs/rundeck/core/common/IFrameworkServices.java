package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;

/**
 * Created by greg on 2/20/15.
 */
public interface IFrameworkServices {
    /**
     * @return a service by name
     * @param name service name
     */
    FrameworkSupportService getService(String name);

    /**
     * Set a service by name
     * @param name name
     * @param service service
     */
    void setService(String name, FrameworkSupportService service);

    ExecutionService getExecutionService();
    OrchestratorService getOrchestratorService();

    WorkflowExecutionService getWorkflowExecutionService();

    StepExecutionService getStepExecutionService();

    FileCopier getFileCopierForNodeAndProject(INodeEntry node, String project) throws ExecutionServiceException;

    FileCopierService getFileCopierService();

    NodeExecutor getNodeExecutorForNodeAndProject(INodeEntry node, String project) throws ExecutionServiceException;

    NodeExecutorService getNodeExecutorService() throws ExecutionServiceException;

    NodeStepExecutionService getNodeStepExecutorService() throws ExecutionServiceException;

    NodeStepExecutor getNodeStepExecutorForItem(NodeStepExecutionItem item) throws ExecutionServiceException;

    NodeDispatcher getNodeDispatcherForContext(ExecutionContext context) throws ExecutionServiceException;

    ResourceModelSourceService getResourceModelSourceService();

    ResourceFormatParserService getResourceFormatParserService();

    ResourceFormatGeneratorService getResourceFormatGeneratorService();

    ServiceProviderLoader getPluginManager();
}
