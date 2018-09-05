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

/*
* NodeStepExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:06 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecNodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptURLNodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * NodeStepExecutionService is a provider of NodeStepExecutors, which aggregates a few services together
 * that provide builtin NodeStepExecutors, and several forms of plugins.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeStepExecutionService
    extends ChainedProviderService<NodeStepExecutor>
    implements PluggableProviderService<NodeStepExecutor>, DescribableService
{
    public static final String                                     SERVICE_NAME = ServiceNameConstants.WorkflowNodeStep;
    private final       PluggableProviderService<NodeStepExecutor> nodeStepPluginAdapterService;
    private final       PluggableProviderService<NodeStepExecutor> remoteScriptNodeStepPluginAdapterService;

    private List<ProviderService<NodeStepExecutor>> serviceList;

    private PresetBaseProviderRegistryService<NodeStepExecutor> primaryService;
    private PresetBaseProviderRegistryService<NodeStepExecutor> dynamicRegistryService;

    public NodeStepExecutionService(final Framework framework) {
        this.serviceList = new ArrayList<>();
        /*
         * NodeStepExecutionService chains several other services:
         * 1. builtin providers
         * 2. NodeStepPlugin providers
         * 3. RemoteScriptNodeStepPlugin providers
         */
        Map<String, Class<? extends NodeStepExecutor>> presetProviders = new HashMap<>();

        presetProviders.put(ExecNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME, ExecNodeStepExecutor.class);
        presetProviders.put(ScriptFileNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME, ScriptFileNodeStepExecutor.class);
        presetProviders.put(ScriptURLNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME, ScriptURLNodeStepExecutor.class);

        this.primaryService = new PresetBaseProviderRegistryService<>(presetProviders, framework, false, SERVICE_NAME);
        this.dynamicRegistryService =
            new PresetBaseProviderRegistryService<>(new HashMap<>(), framework, true, SERVICE_NAME);

        NodeStepPluginService
            nodeStepPluginService =
            new NodeStepPluginService(ServiceNameConstants.WorkflowNodeStep, framework);
        nodeStepPluginAdapterService = nodeStepPluginService.adapter(NodeStepPluginAdapter.CONVERTER);

        RemoteScriptNodeStepPluginService
            remoteScriptNodeStepPluginService =
            new RemoteScriptNodeStepPluginService(ServiceNameConstants.RemoteScriptNodeStep, framework);
        remoteScriptNodeStepPluginAdapterService =
            remoteScriptNodeStepPluginService.adapter(RemoteScriptNodeStepPluginAdapter.CONVERTER);

        serviceList.add(primaryService);
        serviceList.add(dynamicRegistryService);
        serviceList.add(nodeStepPluginAdapterService);
        serviceList.add(remoteScriptNodeStepPluginAdapterService);
    }

    @Override
    public boolean canLoadWithLoader(final ProviderLoader loader) {
        return nodeStepPluginAdapterService.canLoadWithLoader(loader) ||
               remoteScriptNodeStepPluginAdapterService.canLoadWithLoader(loader);
    }

    @Override
    public NodeStepExecutor loadWithLoader(final String providerName, final ProviderLoader loader)
        throws ProviderLoaderException
    {
        if (nodeStepPluginAdapterService.canLoadWithLoader(loader)) {
            return nodeStepPluginAdapterService.loadWithLoader(providerName, loader);
        } else if (remoteScriptNodeStepPluginAdapterService.canLoadWithLoader(loader)) {
            return remoteScriptNodeStepPluginAdapterService.loadWithLoader(providerName, loader);
        } else {
            return null;
        }
    }

    @Override
    public CloseableProvider<NodeStepExecutor> loadCloseableWithLoader(
        final String providerName, final ProviderLoader loader
    ) throws ProviderLoaderException
    {
        if (nodeStepPluginAdapterService.canLoadWithLoader(loader)) {
            return nodeStepPluginAdapterService.loadCloseableWithLoader(providerName, loader);
        } else if (remoteScriptNodeStepPluginAdapterService.canLoadWithLoader(loader)) {
            return remoteScriptNodeStepPluginAdapterService.loadCloseableWithLoader(providerName, loader);
        } else {
            return null;
        }
    }

    @Override
    protected List<ProviderService<NodeStepExecutor>> getServiceList() {
        return serviceList;
    }

    public void registerInstance(final String name, final NodeStepExecutor object) {
        primaryService.registerInstance(name, object);
    }

    public void registerClass(final String name, final Class<? extends NodeStepExecutor> clazz) {
        primaryService.registerClass(name, clazz);
    }


    public NodeStepExecutor getExecutorForExecutionItem(final NodeStepExecutionItem item) throws
                                                                                          ExecutionServiceException {
        return providerOfType(item.getNodeStepType());
    }

    public static NodeStepExecutionService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final NodeStepExecutionService service = new NodeStepExecutionService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (NodeStepExecutionService) framework.getService(SERVICE_NAME);
    }


    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }


    public String getName() {
        return SERVICE_NAME;
    }

    /**
     * @return dynamic registry for providers
     */
    public ProviderRegistryService<NodeStepExecutor> getProviderRegistryService() {
        return dynamicRegistryService;
    }
}
