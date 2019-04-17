/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutor;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategy;
import com.dtolabs.rundeck.core.plugins.PluggableProviderService;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser;
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.StepPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines core plugin types that require Frameowrk constructor, and can provide the pluggin provider services for them
 */
public class CorePluginProviderServices {
    private static final Set<Class<?>> FRAMEWORK_PLUGIN_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            NodeExecutor.class,
            NodeStepPlugin.class,
            RemoteScriptNodeStepPlugin.class,
            StepPlugin.class,
            FileCopier.class,
            ResourceModelSourceFactory.class,
            ResourceFormatParser.class,
            ResourceFormatGenerator.class,
            OrchestratorPlugin.class,
            WorkflowExecutor.class,
            WorkflowStrategy.class
    )));

    /**
     * @param p   class
     * @param <T> type
     * @return true if the type requires Framework argument for construction
     */
    public static <T> boolean isFrameworkDependentPluginType(final Class<T> p) {
        return FRAMEWORK_PLUGIN_TYPES.contains(p);
    }

    /**
     * @param type      class
     * @param framework framework
     * @param <T>       type
     * @return plugin provider service for the type, or null if it is not a framework plugin type
     */
    @SuppressWarnings("unchecked")
    public static <T> PluggableProviderService<T> getPluggableProviderServiceForType(
            final Class<T> type,
            final Framework framework
    )
    {
        if (type.equals(NodeStepPlugin.class)) {
            //this returns a chained service with both NodeStepPlugin and RemoteScriptNodeStepPlugin loading
            PluggableProviderService<NodeStepPlugin>
                    svc = framework.getNodeStepExecutorService().getChainedNodeStepPluginService();
            return (PluggableProviderService<T>) svc;
        } else if (type.equals(RemoteScriptNodeStepPlugin.class)) {
            PluggableProviderService<RemoteScriptNodeStepPlugin>
                    svc = framework.getNodeStepExecutorService().getRemoteScriptNodeStepPluginService();
            return (PluggableProviderService<T>) svc;
        } else if (type.equals(StepPlugin.class)) {
            PluggableProviderService<StepPlugin>
                    svc = framework.getStepExecutionService().getPluginStepExecutionService();
            return (PluggableProviderService<T>) svc;
        } else if (type.equals(NodeExecutor.class)) {
            PluggableProviderService<NodeExecutor> svc = framework.getNodeExecutorService();
            return (PluggableProviderService<T>) svc;
        } else if (type.equals(FileCopier.class)) {
            PluggableProviderService<FileCopier> svc = framework.getFileCopierService();
            return (PluggableProviderService<T>) svc;
        } else if (type.equals(ResourceModelSourceFactory.class)) {
            PluggableProviderService<ResourceModelSourceFactory> svc = framework.getResourceModelSourceService();
            return (PluggableProviderService<T>) svc;
        } else if (type.equals(ResourceFormatParser.class)) {
            PluggableProviderService<ResourceFormatParser> svc = framework.getResourceFormatParserService();
            return (PluggableProviderService<T>) svc;
        } else if (type.equals(ResourceFormatGenerator.class)) {
            PluggableProviderService<ResourceFormatGenerator> svc = framework.getResourceFormatGeneratorService();
            return (PluggableProviderService<T>) svc;
        } else if (type.equals(OrchestratorPlugin.class)) {
            PluggableProviderService<OrchestratorPlugin> svc = framework.getOrchestratorService();
            return (PluggableProviderService<T>) svc;
        } else if (type.equals(WorkflowStrategy.class)) {
            PluggableProviderService<WorkflowStrategy> svc = framework.getWorkflowStrategyService();
            return (PluggableProviderService<T>) svc;
        } else if (type.equals(WorkflowExecutor.class)) {
            PluggableProviderService<WorkflowExecutor> svc = framework.getWorkflowExecutionService();
            return (PluggableProviderService<T>) svc;
        }
        return null;

    }
}
