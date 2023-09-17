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
package com.dtolabs.rundeck.core.execution.dispatch;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.FailedNodesListener;
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator;
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

/**
 * OrchestratorNodeDispatcher invokes the orchestrator plugin that will control the order of node execution
 *
 * @author Ashley Taylor
 */
public class OrchestratorNodeDispatcher implements NodeDispatcher {
    private IFramework framework;

    public OrchestratorNodeDispatcher(IFramework framework) {
        this.framework = framework;
    }

    public DispatcherResult dispatch(final StepExecutionContext context,
                                     final NodeStepExecutionItem item) throws
        DispatcherException {
        return dispatch(context, item, null);
    }

    public DispatcherResult dispatch(final StepExecutionContext context,
                                     final Dispatchable item) throws
        DispatcherException {
        return dispatch(context, null, item);
    }

    public DispatcherResult dispatch(final StepExecutionContext context,
                                     final NodeStepExecutionItem item, final Dispatchable toDispatch) throws
        DispatcherException {

        
        OrchestratorService orchestratorService = framework.getOrchestratorService(); 
        ServiceProviderLoader loader = orchestratorService.getPluginManager();
        OrchestratorConfig config = context.getOrchestrator();
        OrchestratorPlugin plugin;
        try {
            
            plugin = loader.loadProvider(orchestratorService, config.getType());
        } catch (ProviderLoaderException e) {
            throw new DispatcherException(e);
        }
        
        
        Description description = PluginAdapterUtility.buildDescription(plugin, DescriptionBuilder.builder());

        //replace embedded properties

        Map<String, Object> instanceProperties = null;
        if (config.getConfig() != null) {
            instanceProperties = DataContextUtils.replaceDataReferences(config.getConfig(), context.getDataContext());
        }
        final PropertyResolver resolver = PropertyResolverFactory.createFrameworkProjectRuntimeResolver(
                framework,
                context.getFrameworkProject(),
                instanceProperties,
                ServiceNameConstants.Orchestrator,
                config.getType()
        );
        PluginAdapterUtility.configureProperties(resolver, description, plugin, PropertyScope.InstanceOnly);

        INodeSet nodes = context.filteredNodes();
        boolean keepgoing = context.isKeepgoing();

        final HashSet<String> nodeNames = new HashSet<>();
        FailedNodesListener failedListener = context.getExecutionListener().getFailedNodesListener();

        context.getExecutionListener().log(3,
            "preparing for orchestrator execution...(keepgoing? " + keepgoing + ", threads: "
            + context.getThreadCount()
            + ")");
        //to not have 2 orchestrator within one run when it processed the inner node

        boolean success = false;
        final HashMap<String, NodeStepResult> resultMap = new HashMap<>();
        final HashMap<String, NodeStepResult> failureMap = new HashMap<>();
        final Collection<INodeEntry> nodes1 = nodes.getNodes();
        //reorder based on configured rank property and order
        final String rankProperty = null != context.getNodeRankAttribute() ? context.getNodeRankAttribute() : "nodename";
        final boolean rankAscending = context.isNodeRankOrderAscending();
        final INodeEntryComparator comparator = new INodeEntryComparator(rankProperty);
        final TreeSet<INodeEntry> orderedNodes = new TreeSet<>(
                rankAscending ? comparator : Collections.reverseOrder(comparator));

        orderedNodes.addAll(nodes1);
        
        Map<INodeEntry, Callable<NodeStepResult>> executions = new HashMap<>();
        for (final INodeEntry node: orderedNodes) {
            final Callable<NodeStepResult> tocall;
            if (null != item) {
                tocall = execItemCallable(context, item, resultMap, node, failureMap);
            } else {
                tocall = dispatchableCallable(context, toDispatch, resultMap, node, failureMap);
            }
            nodeNames.add(node.getNodename());
            executions.put(node, tocall);
        }
        if (null != failedListener) {
            failedListener.matchedNodes(nodeNames);
        }
        context.getExecutionListener().log(3, "orchestrator dispatch to nodes: " + nodeNames);
        
        
        Orchestrator orchestrator = plugin.createOrchestrator(context, orderedNodes);
        OrchestratorNodeProcessor
            processor =
            OrchestratorNodeProcessor
                .builder()
                .threadCount(context.getThreadCount())
                .keepgoing(keepgoing)
                .orchestrator(orchestrator)
                .executions(executions)
                .cancelOnInterrupt(true)
                .build();
        
        try {
            success = processor.execute();
        } catch (OrchestratorNodeProcessor.NodeProcessorException e) {
            context.getExecutionListener().log(0, e.getMessage());
            if (!keepgoing) {
                throw new DispatcherException(e);
            }
        }
        if (processor.isInterrupted()) {
            if (!keepgoing) {
                throw new DispatcherException("Node dispatcher cancelled on interrupt");
            }
            context.getExecutionListener().log(0, "Node dispatcher cancelled on interrupt");
        }
        //evaluate the failed nodes
        if (failureMap.size() > 0) {
            if (null != failedListener) {
                //tell listener of failed node list
                //extract status results
                failedListener.nodesFailed(failureMap);
            }
            return new DispatcherResultImpl(failureMap, false);
        } else if (null != failedListener && nodeNames.isEmpty()) {
            failedListener.nodesSucceeded();
        }

        final boolean status = success;

        return new DispatcherResultImpl(resultMap, status, "Orchestrator dispatch: (" + status + ") " + resultMap);
    }

    private Callable<NodeStepResult> dispatchableCallable(final ExecutionContext context, final Dispatchable toDispatch,
                                          final HashMap<String, NodeStepResult> resultMap, final INodeEntry node,
                                          final Map<String, NodeStepResult> failureMap) {
        return new Callable<NodeStepResult>() {
            public NodeStepResult call() throws Exception {
                final NodeStepResult dispatch = toDispatch.dispatch(context, node);
                if (!dispatch.isSuccess()) {
                    failureMap.put(node.getNodename(), dispatch);
                }
                resultMap.put(node.getNodename(), dispatch);
                return dispatch;
            }
        };
    }

   
    private ParallelNodeDispatcher.ExecNodeStepCallable execItemCallable(final StepExecutionContext context, final NodeStepExecutionItem item,
                                      final HashMap<String, NodeStepResult> resultMap, final INodeEntry node,
                                      final Map<String, NodeStepResult> failureMap) {
        return new ParallelNodeDispatcher.ExecNodeStepCallable(context, item, resultMap, node, failureMap, framework);
    }



}
