/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
