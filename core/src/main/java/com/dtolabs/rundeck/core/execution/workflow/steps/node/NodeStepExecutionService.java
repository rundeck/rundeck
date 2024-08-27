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
import com.dtolabs.rundeck.core.common.IServicesRegistration;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;

import java.util.*;


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
    private final PluggableProviderService<NodeStepExecutor> nodeStepPluginAdaptedNodeStepExecutorService;
    private final PluggableProviderService<NodeStepExecutor> remoteScriptAdaptedNodeStepExecutorService;

    private List<ProviderService<NodeStepExecutor>> serviceList;

    private PresetBaseProviderRegistryService<NodeStepExecutor> primaryService;
    private PresetBaseProviderRegistryService<NodeStepExecutor> dynamicRegistryService;
    private final NodeStepPluginService nodeStepPluginService;
    private final RemoteScriptNodeStepPluginService remoteScriptNodeStepPluginService;
    private final ChainedNodeStepPluginService chainedNodeStepPluginService;
    private static final Map<String, Class<? extends NodeStepExecutor>> PRESET_PROVIDERS ;

    static {
        Map<String, Class<? extends NodeStepExecutor>> map = new HashMap<>();
        PRESET_PROVIDERS = Collections.unmodifiableMap(map);
    }

    public NodeStepExecutionService(final Framework framework) {
        this.serviceList = new ArrayList<>();
        /*
         * NodeStepExecutionService chains several other services:
         * 1. builtin providers
         * 2. NodeStepPlugin providers
         * 3. RemoteScriptNodeStepPlugin providers
         */
        this.primaryService = new PresetBaseProviderRegistryService<>(PRESET_PROVIDERS, framework, false, SERVICE_NAME);
        this.dynamicRegistryService =
            new PresetBaseProviderRegistryService<>(new HashMap<>(), framework, true, SERVICE_NAME);

        nodeStepPluginService = new NodeStepPluginService(ServiceNameConstants.WorkflowNodeStep, framework);

        remoteScriptNodeStepPluginService =
                new RemoteScriptNodeStepPluginService(ServiceNameConstants.RemoteScriptNodeStep, framework);


        List<ProviderService<NodeStepPlugin>>
                serviceList =
                new ArrayList<>(Collections.singletonList(nodeStepPluginService));


        /*
         * this new behavior converts RemoteScriptNodeStepPlugin to a NodeStepPlugin
         * which we then convert using the NodeStepPluginAdapter to NodeStepExecutor
         * and allows us to add the remote script plugin service to the NodeStepPlugin service chain
         */

        //convert (original)RemoteScriptNodeStepPlugin -> NodeStepPlugin (adapted sevice)
        final PluggableProviderService<NodeStepPlugin>
                remoteScriptAdaptedNodeStepPluginService =
                getRemoteScriptNodeStepPluginService().adapter(RemoteScriptNodeStepPluginAdapter_Ext.CONVERT_TO_NODE_STEP_PLUGIN);

        //convert (adapted)NodeStepPlugin -> NodeStepExecutor
        remoteScriptAdaptedNodeStepExecutorService =
                remoteScriptAdaptedNodeStepPluginService.adapter(
                        new NodeStepPluginAdapter.ConvertToNodeStepExecutor(
                                ServiceNameConstants.RemoteScriptNodeStep,
                                false
                        )
                );

        //chain both (original)NodeStepPlugin and (adapted)RemoteScriptNodestepPlugin services as single service with
        // NodeStepPlugin type
        serviceList.add(remoteScriptAdaptedNodeStepPluginService);

        //convert (original)NodeStepPlugin -> NodeStepExecutor
        nodeStepPluginAdaptedNodeStepExecutorService =
                getNodeStepPluginService().adapter(NodeStepPluginAdapter.CONVERT_TO_NODE_STEP_EXECUTOR);


        chainedNodeStepPluginService = new ChainedNodeStepPluginService(
                ServiceNameConstants.WorkflowNodeStep,
                serviceList
        );

        //add list of services to the chain
        this.serviceList.add(primaryService);
        this.serviceList.add(dynamicRegistryService);
        this.serviceList.add(nodeStepPluginAdaptedNodeStepExecutorService);
        this.serviceList.add(remoteScriptAdaptedNodeStepExecutorService);
    }

    public ChainedNodeStepPluginService getChainedNodeStepPluginService() {
        return chainedNodeStepPluginService;
    }

    static class ChainedNodeStepPluginService
            extends ChainedProviderService<NodeStepPlugin>
            implements PluggableProviderService<NodeStepPlugin>
    {
        private String name;
        private List<ProviderService<NodeStepPlugin>> serviceList;

        public ChainedNodeStepPluginService(
                final String name,
                final List<ProviderService<NodeStepPlugin>> serviceList
        )
        {
            this.name = name;
            this.serviceList = serviceList;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<ProviderService<NodeStepPlugin>> getServiceList() {
            return serviceList;
        }

        @Override
        public List<ProviderIdent> listDescribableProviders() {
            return listProviders();
        }

        @Override
        public List<Description> listDescriptions() {
            return DescribableServiceUtil.listDescriptions(this);
        }
    }

    @Override
    public boolean canLoadWithLoader(final ProviderLoader loader) {
        return nodeStepPluginAdaptedNodeStepExecutorService.canLoadWithLoader(loader) ||
               remoteScriptAdaptedNodeStepExecutorService.canLoadWithLoader(loader);
    }

    @Override
    public NodeStepExecutor loadWithLoader(final String providerName, final ProviderLoader loader)
        throws ProviderLoaderException
    {
        if (nodeStepPluginAdaptedNodeStepExecutorService.canLoadWithLoader(loader)) {
            return nodeStepPluginAdaptedNodeStepExecutorService.loadWithLoader(providerName, loader);
        } else if (remoteScriptAdaptedNodeStepExecutorService.canLoadWithLoader(loader)) {
            return remoteScriptAdaptedNodeStepExecutorService.loadWithLoader(providerName, loader);
        } else {
            return null;
        }
    }

    @Override
    public CloseableProvider<NodeStepExecutor> loadCloseableWithLoader(
        final String providerName, final ProviderLoader loader
    ) throws ProviderLoaderException
    {
        if (nodeStepPluginAdaptedNodeStepExecutorService.canLoadWithLoader(loader)) {
            return nodeStepPluginAdaptedNodeStepExecutorService.loadCloseableWithLoader(providerName, loader);
        } else if (remoteScriptAdaptedNodeStepExecutorService.canLoadWithLoader(loader)) {
            return remoteScriptAdaptedNodeStepExecutorService.loadCloseableWithLoader(providerName, loader);
        } else {
            return null;
        }
    }

    @Override
    protected List<ProviderService<NodeStepExecutor>> getServiceList() {
        return serviceList;
    }

    public NodeStepExecutor getExecutorForExecutionItem(final NodeStepExecutionItem item) throws
                                                                                          ExecutionServiceException {
        return providerOfType(item.getNodeStepType());
    }

    public static NodeStepExecutionService getInstanceForFramework(
            final Framework framework,
            final IServicesRegistration registration
    )
    {
        if (null == registration.getService(SERVICE_NAME)) {
            final NodeStepExecutionService service = new NodeStepExecutionService(framework);
            registration.setService(SERVICE_NAME, service);
            return service;
        }
        return (NodeStepExecutionService) registration.getService(SERVICE_NAME);
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

    public NodeStepPluginService getNodeStepPluginService() {
        return nodeStepPluginService;
    }

    public RemoteScriptNodeStepPluginService getRemoteScriptNodeStepPluginService() {
        return remoteScriptNodeStepPluginService;
    }
}
