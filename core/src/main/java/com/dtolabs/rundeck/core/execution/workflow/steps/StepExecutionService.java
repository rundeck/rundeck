/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* StepExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/2/12 10:42 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.MissingProviderException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.BaseProviderRegistryService;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.Description;

import java.util.ArrayList;
import java.util.List;


/**
 * StepExecutionService can provide executors for workflow steps.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class StepExecutionService extends BaseProviderRegistryService<StepExecutor> implements FrameworkSupportService,DescribableService {
    private static final String SERVICE_NAME = "Internal_StepExecution";

    private PluginStepExecutionService pluginStepExecutionService;
    public String getName() {
        return SERVICE_NAME;
    }

    StepExecutionService(final Framework framework) {
        super(framework);
        pluginStepExecutionService = new PluginStepExecutionService(framework);
        registry.put(NodeDispatchStepExecutor.STEP_EXECUTION_TYPE, NodeDispatchStepExecutor.class);
    }

    @Override
    public StepExecutor providerOfType(String providerName)
        throws ExecutionServiceException {
        StepExecutor t = null;
        MissingProviderException caught = null;
        try {
            t = super.providerOfType(providerName);
        } catch (MissingProviderException e) {
            //ignore and attempt to load from the plugin manager
            caught = e;
        }
        if (null != t) {
            return t;
        }
        if (null != pluginStepExecutionService) {
            return pluginStepExecutionService.providerOfType(providerName);
        } else if (null != caught) {
            throw caught;
        } else {
            throw new MissingProviderException("Provider not found", getName(), providerName);
        }
    }

    public static StepExecutionService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final StepExecutionService service = new StepExecutionService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (StepExecutionService) framework.getService(SERVICE_NAME);
    }

    public StepExecutor getExecutorForItem(final StepExecutionItem item) throws ExecutionServiceException {
        String type = item.getType();
        return providerOfType(type);
    }

    public List<Description> listDescriptions() {
        final ArrayList<Description> list = new ArrayList<Description>();
        for (final ProviderIdent providerIdent : listProviders()) {
            try {
                final StepExecutor providerForType = providerOfType(providerIdent.getProviderName());
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
                final StepExecutor providerForType = providerOfType(providerIdent.getProviderName());
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
}
