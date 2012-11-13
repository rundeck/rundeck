/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* NodeStepExecutorService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:06 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.ChainedProviderService;
import com.dtolabs.rundeck.core.plugins.ConverterService;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;

import java.util.ArrayList;
import java.util.List;


/**
 * NodeStepExecutorService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeStepExecutorService extends ChainedProviderService<NodeStepExecutor> implements DescribableService {
    public static final String SERVICE_NAME = "NodeStepExecutor";

    private BuiltinNodeStepExecutorService primaryService;
    private ConverterService<NodeStepPlugin, NodeStepExecutor> secondaryService;
    private NodeStepPluginService pluginService;
    public NodeStepExecutorService(final Framework framework) {
        this.primaryService=new BuiltinNodeStepExecutorService(framework);
        this.pluginService = new NodeStepPluginService(framework);
        this.secondaryService
            = new ConverterService<NodeStepPlugin, NodeStepExecutor>(pluginService,
                                                                     new NodeStepPluginConverter());
    }

    @Override
    protected ProviderService<NodeStepExecutor> getPrimaryService() {
        return primaryService;
    }

    @Override
    protected ProviderService<NodeStepExecutor> getSecondaryService() {
        return secondaryService;
    }


    public void registerInstance(String name, NodeStepExecutor object) {
        primaryService.registerInstance(name, object);
    }

    public void registerClass(String name, Class<? extends NodeStepExecutor> clazz) {
        primaryService.registerClass(name, clazz);
    }
    public void resetDefaultProviders() {
        primaryService.resetDefaultProviders();
    }

    public NodeStepExecutor getExecutorForExecutionItem(final NodeStepExecutionItem item) throws
                                                                                          ExecutionServiceException {
        return providerOfType(item.getNodeStepType());
    }

    public static NodeStepExecutorService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final NodeStepExecutorService service = new NodeStepExecutorService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (NodeStepExecutorService) framework.getService(SERVICE_NAME);
    }


    public List<Description> listDescriptions() {
        final ArrayList<Description> list = new ArrayList<Description>();
        for (final ProviderIdent providerIdent : listProviders()) {
            try {
                final NodeStepExecutor providerForType = providerOfType(providerIdent.getProviderName());
                if (providerForType instanceof Describable) {
                    final Describable desc = (Describable) providerForType;
                    final Description description = desc.getDescription();
                    if (null != description) {
                        list.add(description);
                    }
                }
            } catch (ExecutionServiceException e) {
                e.printStackTrace();
            }

        }
        return list;
    }

    public List<ProviderIdent> listDescribableProviders() {
        final ArrayList<ProviderIdent> list = new ArrayList<ProviderIdent>();
        for (final ProviderIdent providerIdent : listProviders()) {
            try {
                final NodeStepExecutor providerForType = providerOfType(providerIdent.getProviderName());
                if (providerForType instanceof Describable) {
                    final Describable desc = (Describable) providerForType;
                    final Description description = desc.getDescription();
                    if (null != description) {
                        list.add(providerIdent);
                    }
                }
            } catch (ExecutionServiceException e) {
                e.printStackTrace();
            }

        }
        return list;
    }


    public String getName() {
        return SERVICE_NAME;
    }
}
