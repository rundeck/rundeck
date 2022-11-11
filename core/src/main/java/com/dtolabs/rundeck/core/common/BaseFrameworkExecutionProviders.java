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
 * base (legacy) implementation to provide Execution service providers using the Framework service registration
 * mechanism
 */
public class BaseFrameworkExecutionProviders
        implements IExecutionProviders
{
    @Getter @Setter private Framework framework;

    public BaseFrameworkExecutionProviders() {

    }

    public static BaseFrameworkExecutionProviders create(final Framework testFramework) {
        BaseFrameworkExecutionProviders baseFrameworkExecutionProviders = new BaseFrameworkExecutionProviders();
        baseFrameworkExecutionProviders.setFramework(testFramework);
        return baseFrameworkExecutionProviders;
    }

    @Override
    public StepExecutor getStepExecutorForItem(final StepExecutionItem item, final String project)
            throws ExecutionServiceException
    {
        return StepExecutionService.getInstanceForFramework(framework).getExecutorForItem(item);
    }

    @Override
    public FileCopier getFileCopierForNodeAndProject(final INodeEntry node, final String project)
            throws ExecutionServiceException
    {
        return FileCopierService.getInstanceForFramework(framework).getProviderForNodeAndProject(node, project);
    }

    @Override
    public NodeExecutor getNodeExecutorForNodeAndProject(final INodeEntry node, final String project)
            throws ExecutionServiceException
    {
        return NodeExecutorService.getInstanceForFramework(framework).getProviderForNodeAndProject(node, project);
    }

    @Override
    public NodeStepExecutor getNodeStepExecutorForItem(final NodeStepExecutionItem item, final String project)
            throws ExecutionServiceException
    {
        return NodeStepExecutionService.getInstanceForFramework(framework).getExecutorForExecutionItem(item);
    }

    @Override
    public NodeDispatcher getNodeDispatcherForContext(final ExecutionContext context) throws ExecutionServiceException {
        return NodeDispatcherService.getInstanceForFramework(context.getIFramework()).getNodeDispatcher(context);
    }
}
