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
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecNodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptURLNodeStepExecutor;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.PluggableProviderRegistryService;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.Description;

import java.util.ArrayList;
import java.util.List;


/**
 * NodeStepExecutorService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeStepExecutorService extends PluggableProviderRegistryService<NodeStepExecutor> implements
                                                                                                DescribableService {
    public static final String SERVICE_NAME = "NodeStepExecutor";

    public NodeStepExecutorService(final Framework framework) {
        super(framework);

        resetDefaultProviders();
    }

    public void resetDefaultProviders() {
        registry.put(ExecNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME, ExecNodeStepExecutor.class);
        registry.put(ScriptFileNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME, ScriptFileNodeStepExecutor.class);
        registry.put(ScriptURLNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME, ScriptURLNodeStepExecutor.class);
        instanceregistry.remove(ExecNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME);
        instanceregistry.remove(ScriptFileNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME);
        instanceregistry.remove(ScriptURLNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME);
    }

    public NodeStepExecutor getExecutorForExecutionItem(final NodeStepExecutionItem item) throws
                                                                                  ExecutionServiceException {
        return providerOfType(item.getNodeStepType());
    }

    public static NodeStepExecutorService getInstanceForFramework(Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final NodeStepExecutorService service = new NodeStepExecutorService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (NodeStepExecutorService) framework.getService(SERVICE_NAME);
    }

    public boolean isValidProviderClass(Class clazz) {

        return NodeStepExecutor.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    public NodeStepExecutor createProviderInstance(Class<NodeStepExecutor> clazz, String name) throws
                                                                                               PluginException,
                                                                                               ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    public boolean isScriptPluggable() {
        //TODO: add script plugins
        return false;
    }

    public NodeStepExecutor createScriptProviderInstance(ScriptPluginProvider provider) throws PluginException {
        return null;
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
                    list.add(providerIdent);
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
