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
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcherService;
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategyService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.PluginManagerService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

/**
 * Created by greg on 2/20/15.
 */
public class ServiceSupport implements IFrameworkServices, IExecutionProviders, IExecutionServices {

    final HashMap<String,FrameworkSupportService> services = new HashMap<String, FrameworkSupportService>();

    private Framework framework;
    @Getter @Setter private ExecutionService executionService;
    @Getter @Setter private IExecutionProviders executionProviders;

    public ServiceSupport() {

    }

    /**
     * Initialize children, the various resource management objects
     */
    public void initialize(Framework framework) {
        if (null == this.framework) {
            setFramework(framework);
            NodeStepExecutionService.getInstanceForFramework(getFramework());
            NodeExecutorService.getInstanceForFramework(getFramework());
            FileCopierService.getInstanceForFramework(getFramework());
            NodeDispatcherService.getInstanceForFramework(getFramework());
            WorkflowExecutionService.getInstanceForFramework(getFramework());
            StepExecutionService.getInstanceForFramework(getFramework());
            ResourceModelSourceService.getInstanceForFramework(getFramework());
            ResourceFormatParserService.getInstanceForFramework(getFramework());
            ResourceFormatGeneratorService.getInstanceForFramework(getFramework());
        }
    }

    /**
     * @return a service by name
     * @param name service name
     */
    @Override
    public FrameworkSupportService getService(String name) {
        return services.get(name);
    }
    /**
     * Set a service by name
     * @param name name
     * @param service service
     */
    @Override
    public void setService(final String name, final FrameworkSupportService service){
        synchronized (services){
            if(null==services.get(name) && null!=service) {
                services.put(name, service);
            }else if(null==service) {
                services.remove(name);
            }
        }
    }

    @Override
    public void overrideService(final String name, final FrameworkSupportService service) {
        synchronized (services) {
            services.put(name, service);
        }
    }

    @Override
    public OrchestratorService getOrchestratorService() {
        return OrchestratorService.getInstanceForFramework(getFramework());
    }

    @Override
    public WorkflowExecutionService getWorkflowExecutionService() {
        return WorkflowExecutionService.getInstanceForFramework(getFramework());
    }

    @Override
    public WorkflowStrategyService getWorkflowStrategyService() {
        return WorkflowStrategyService.getInstanceForFramework(getFramework());
    }

    @Override
    public StepExecutionService getStepExecutionService() {
        return StepExecutionService.getInstanceForFramework(getFramework());
    }

    @Override
    public StepExecutor getStepExecutorForItem(final StepExecutionItem item, final String project) throws ExecutionServiceException {
        return executionProviders.getStepExecutorForItem(item, project);
    }

    @Override
    public FileCopier getFileCopierForNodeAndProject(INodeEntry node, final String project) throws
            ExecutionServiceException
    {
        return executionProviders.getFileCopierForNodeAndProject(node, project);
    }

    @Override
    public FileCopierService getFileCopierService() {
        return FileCopierService.getInstanceForFramework(getFramework());
    }

    @Override
    public NodeExecutor getNodeExecutorForNodeAndProject(INodeEntry node, final String project) throws ExecutionServiceException {
        return executionProviders.getNodeExecutorForNodeAndProject(node, project);
    }
    @Override
    public NodeExecutorService getNodeExecutorService() {
        return NodeExecutorService.getInstanceForFramework(getFramework());
    }
    @Override
    public NodeStepExecutionService getNodeStepExecutorService() {
        return NodeStepExecutionService.getInstanceForFramework(getFramework());
    }
    @Override
    public NodeStepExecutor getNodeStepExecutorForItem(NodeStepExecutionItem item, final String project) throws ExecutionServiceException {
        return executionProviders.getNodeStepExecutorForItem(item, project);
    }
    @Override
    public NodeDispatcher getNodeDispatcherForContext(ExecutionContext context) throws ExecutionServiceException {
        return executionProviders.getNodeDispatcherForContext(context);
    }

    @Override
    public NodeDispatcherService getNodeDispatcherService() {
        return NodeDispatcherService.getInstanceForFramework(getFramework());
    }

    @Override
    public ResourceModelSourceService getResourceModelSourceService() {
        return ResourceModelSourceService.getInstanceForFramework(getFramework());
    }

    @Override
    public ResourceFormatParserService getResourceFormatParserService() {
        return ResourceFormatParserService.getInstanceForFramework(getFramework());
    }

    @Override
    public ResourceFormatGeneratorService getResourceFormatGeneratorService() {
        return ResourceFormatGeneratorService.getInstanceForFramework(getFramework());
    }

    @Override
    public ServiceProviderLoader getPluginManager(){
        if(null!=getService(PluginManagerService.SERVICE_NAME)) {
            return PluginManagerService.getInstanceForFramework(getFramework());
        }
        return null;
    }


    public Framework getFramework() {
        return framework;
    }

    public void setFramework(final Framework framework) {
        this.framework = framework;
    }
}
