/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
 */
package org.rundeck.plugin.util;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategyService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;

import java.util.Set;

public class ScriptFileFramework extends Framework {

    private Framework framework;

    private ExecutionService executionService;


    public ScriptFileFramework(Framework framework) {
        super(framework.getFilesystemFramework(), framework.getFrameworkProjectMgr(), framework.getPropertyLookup(), null, null);
        this.framework = framework;
    }

    @Override
    public ProjectManager getFrameworkProjectMgr() {
        return framework.getFrameworkProjectMgr();
    }

    @Override
    public IPropertyLookup getPropertyLookup() {
        return framework.getPropertyLookup();
    }

    @Override
    public String getFrameworkNodeHostname() {
        return framework.getFrameworkNodeHostname();
    }

    @Override
    public String getFrameworkNodeName() {
        return framework.getFrameworkNodeName();
    }

    @Override
    public ExecutionService getExecutionService() {
        return this.executionService;
    }

    public void setExecutionService(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public OrchestratorService getOrchestratorService() {
        return null;
    }

    @Override
    public WorkflowExecutionService getWorkflowExecutionService() {
        return null;
    }

    @Override
    public WorkflowStrategyService getWorkflowStrategyService() {
        return null;
    }

    @Override
    public StepExecutionService getStepExecutionService() {
        return framework.getStepExecutionService();
    }

    @Override
    public FileCopierService getFileCopierService() {
        return framework.getFileCopierService();
    }

    @Override
    public NodeExecutorService getNodeExecutorService() {
        return framework.getNodeExecutorService();
    }

    @Override
    public NodeStepExecutionService getNodeStepExecutorService() {
        return framework.getNodeStepExecutorService();
    }

    @Override
    public NodeEntryImpl createFrameworkNode() {
        return framework.createFrameworkNode();
    }

    @Override
    public ResourceFormatParserService getResourceFormatParserService() {
        return framework.getResourceFormatParserService();
    }

    @Override
    public ResourceFormatGeneratorService getResourceFormatGeneratorService() {
        return new ResourceFormatGeneratorService(this.framework);

    }

    @Override
    public ServiceProviderLoader getPluginManager() {
        return framework.getPluginManager();
    }

    @Override
    public FileCopier getFileCopierForNodeAndProject(INodeEntry node, ExecutionContext context) throws ExecutionServiceException {
        FileCopierService fileCopierService = getFileCopierService();
        String fileCopier = FileCopierService.getProviderNameForNode(
                getPropertyLookup().getProperty("framework.server.name").equals(node.getNodename()),
                getProjectManager().loadProjectConfig(context.getFrameworkProject())
        );

        if (null != node.getAttributes() && null != node.getAttributes().get(fileCopierService.getServiceProviderNodeAttributeForNode(
                node))) {
            fileCopier = node.getAttributes().get(fileCopierService.getServiceProviderNodeAttributeForNode(node));
        }

        return fileCopierService.providerOfType(fileCopier);
    }

    @Override
    public NodeExecutor getNodeExecutorForNodeAndProject(INodeEntry node, ExecutionContext context) throws ExecutionServiceException {
        NodeExecutorService nodeExecutorService = getNodeExecutorService();
        String nodeExecutor = NodeExecutorService.getProviderNameForNode(
                getPropertyLookup().getProperty("framework.server.name").equals(node.getNodename()),
                getProjectManager().loadProjectConfig(context.getFrameworkProject())
        );

        if (null != node.getAttributes() && null != node.getAttributes().get(nodeExecutorService.getServiceProviderNodeAttributeForNode(
                node))) {
            nodeExecutor = node.getAttributes().get(nodeExecutorService.getServiceProviderNodeAttributeForNode(node));
        }

        return nodeExecutorService.providerOfType(nodeExecutor);
    }
}
