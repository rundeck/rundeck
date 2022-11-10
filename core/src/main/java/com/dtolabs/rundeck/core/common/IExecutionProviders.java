package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;

/**
 * Retrieve execution providers based on context criteria
 */
public interface IExecutionProviders {
    /**
     * provide step executor for the item and the project
     * @param item
     * @param project
     * @return
     * @throws ExecutionServiceException
     */
    StepExecutor getStepExecutorForItem(StepExecutionItem item, final String project) throws ExecutionServiceException;

    /**
     * provide file copier for the node and project
     * @param node
     * @param project
     * @return
     * @throws ExecutionServiceException
     */
    FileCopier getFileCopierForNodeAndProject(INodeEntry node, String project) throws ExecutionServiceException;

    /**
     * provide node executor for the node and project
     * @param node
     * @param project
     * @return
     * @throws ExecutionServiceException
     */
    NodeExecutor getNodeExecutorForNodeAndProject(INodeEntry node, String project) throws ExecutionServiceException;

    /**
     * provide node step executor for item and project
     * @param item
     * @param project
     * @return
     * @throws ExecutionServiceException
     */
    NodeStepExecutor getNodeStepExecutorForItem(NodeStepExecutionItem item, final String project) throws ExecutionServiceException;

    /**
     * provide node dispatcher for context
     * @param context
     * @return
     * @throws ExecutionServiceException
     */
    NodeDispatcher getNodeDispatcherForContext(ExecutionContext context) throws ExecutionServiceException;
}
