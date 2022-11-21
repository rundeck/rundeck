package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcherService;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import lombok.Getter;
import lombok.Setter;

/**
 * base (legacy) implementation to provide Execution service providers via the IExecutionServices
 */
public class BaseFrameworkExecutionProviders
        implements IExecutionProviders
{
    @Getter @Setter private IExecutionServices executionServices;

    public BaseFrameworkExecutionProviders() {

    }

    public static BaseFrameworkExecutionProviders create(final IExecutionServices executionServices) {
        BaseFrameworkExecutionProviders baseFrameworkExecutionProviders = new BaseFrameworkExecutionProviders();
        baseFrameworkExecutionProviders.setExecutionServices(executionServices);
        return baseFrameworkExecutionProviders;
    }

    @Override
    public StepExecutor getStepExecutorForItem(final StepExecutionItem item, final String project)
            throws ExecutionServiceException
    {
        return executionServices.getStepExecutionService().getExecutorForItem(item);
    }

    @Override
    public FileCopier getFileCopierForNodeAndProject(final INodeEntry node, final ExecutionContext context)
            throws ExecutionServiceException
    {
        return executionServices.getFileCopierService().getProviderForNodeAndProject(node, context);
    }

    @Override
    public NodeExecutor getNodeExecutorForNodeAndProject(final INodeEntry node, final ExecutionContext context)
            throws ExecutionServiceException
    {
        return executionServices.getNodeExecutorService().getProviderForNodeAndProject(node, context);
    }

    @Override
    public NodeStepExecutor getNodeStepExecutorForItem(final NodeStepExecutionItem item, final String project)
            throws ExecutionServiceException
    {
        return executionServices.getNodeStepExecutorService().getExecutorForExecutionItem(item);
    }

    @Override
    public NodeDispatcher getNodeDispatcherForContext(final ExecutionContext context) throws ExecutionServiceException {
        return executionServices.getNodeDispatcherService().getNodeDispatcher(context);
    }
}
