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
* StepExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/2/12 10:42 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.IServicesRegistration;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.StepPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * StepExecutionService can provide executors for workflow steps.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class StepExecutionService
    extends ChainedProviderService<StepExecutor>
    implements DescribableService,
               PluggableProviderService<StepExecutor>
{
    public static final String SERVICE_NAME = ServiceNameConstants.WorkflowStep;
    private final PluggableProviderService<StepExecutor> stepPluginAdaptedStepExecutorService;
    private final PluginStepExecutionService pluginStepExecutionService;

    private List<ProviderService<StepExecutor>>             serviceList;
    @Deprecated()
    private PresetBaseProviderRegistryService<StepExecutor> builtinStepExecutionService;
    private PresetBaseProviderRegistryService<StepExecutor> dynamicRegistryService;

    public String getName() {
        return SERVICE_NAME;
    }

    public StepExecutionService(final Framework framework) {
        this.serviceList = new ArrayList<>();
        HashMap<String, Class<? extends StepExecutor>> presets = new HashMap<>();
        presets.put(NodeDispatchStepExecutor.STEP_EXECUTION_TYPE, NodeDispatchStepExecutor.class);
        builtinStepExecutionService = new PresetBaseProviderRegistryService<>(presets, framework, false, SERVICE_NAME);
        dynamicRegistryService =
            new PresetBaseProviderRegistryService<>(new HashMap<>(), framework, true, SERVICE_NAME);
        pluginStepExecutionService = new PluginStepExecutionService(SERVICE_NAME, framework);
        stepPluginAdaptedStepExecutorService = getPluginStepExecutionService().adapter(getStepAdapter());

        serviceList.add(builtinStepExecutionService);
        serviceList.add(dynamicRegistryService);
        serviceList.add(stepPluginAdaptedStepExecutorService);
    }
    public static boolean isRegistered(String name){
        return name.equals(NodeDispatchStepExecutor.STEP_EXECUTION_TYPE);
    }

    @Override
    public boolean canLoadWithLoader(final ProviderLoader loader) {
        return stepPluginAdaptedStepExecutorService.canLoadWithLoader(loader);
    }

    @Override
    public StepExecutor loadWithLoader(final String providerName, final ProviderLoader loader)
        throws ProviderLoaderException
    {
        return stepPluginAdaptedStepExecutorService.loadWithLoader(providerName, loader);
    }

    @Override
    public CloseableProvider<StepExecutor> loadCloseableWithLoader(
        final String providerName, final ProviderLoader loader
    ) throws ProviderLoaderException
    {
        return stepPluginAdaptedStepExecutorService.loadCloseableWithLoader(providerName, loader);
    }

    @Override
    protected List<ProviderService<StepExecutor>> getServiceList() {
        return serviceList;
    }

    public static StepExecutionService getInstanceForFramework(final Framework framework,
                                                               final IServicesRegistration registration) {
        if (null == registration.getService(SERVICE_NAME)) {
            final StepExecutionService service = new StepExecutionService(framework);
            registration.setService(SERVICE_NAME, service);
            return service;
        }
        return (StepExecutionService) registration.getService(SERVICE_NAME);
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

    @Deprecated
    public void registerClass(String name, Class<? extends StepExecutor> clazz) {
        builtinStepExecutionService.registerClass(name, clazz);
    }
    @Deprecated
    public void registerInstance(String name, StepExecutor object) {
        builtinStepExecutionService.registerInstance(name, object);
    }

    /**
     * @return dynamic registry for providers
     */
    @Deprecated
    public ProviderRegistryService<StepExecutor> getProviderRegistryService() {
        return dynamicRegistryService;
    }

    public PluginStepExecutionService getPluginStepExecutionService() {
        return pluginStepExecutionService;
    }

    public Converter<StepPlugin, StepExecutor> getStepAdapter(){
        return StepPluginAdapter.CONVERTER;
    }

}
