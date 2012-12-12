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
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.ChainedProviderService;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.util.ArrayList;
import java.util.List;


/**
 * StepExecutionService can provide executors for workflow steps.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class StepExecutionService extends ChainedProviderService<StepExecutor> implements DescribableService {
    public static final String SERVICE_NAME = ServiceNameConstants.WorkflowStep;

    private List<ProviderService<StepExecutor>> serviceList;
    private BuiltinStepExecutionService builtinStepExecutionService;

    public String getName() {
        return SERVICE_NAME;
    }

    StepExecutionService(final Framework framework) {
        this.serviceList = new ArrayList<ProviderService<StepExecutor>>();
        builtinStepExecutionService = new BuiltinStepExecutionService(SERVICE_NAME, framework);
        final ProviderService<StepExecutor> pluginStepExecutionService
            = new PluginStepExecutionService(SERVICE_NAME, framework)
            .adapter(StepPluginAdapter.CONVERTER);

        serviceList.add(builtinStepExecutionService);
        serviceList.add(pluginStepExecutionService);
    }

    @Override
    protected List<ProviderService<StepExecutor>> getServiceList() {
        return serviceList;
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
        return providerOfType(item.getType());
    }

    @Override
    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }

    @Override
    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this);
    }

    public void registerClass(String name, Class<? extends StepExecutor> clazz) {
        builtinStepExecutionService.registerClass(name, clazz);
    }

    public void registerInstance(String name, StepExecutor object) {
        builtinStepExecutionService.registerInstance(name, object);
    }
}
