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
* StepPluginAdapter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/13/12 6:30 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.storage.StorageTree;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import org.rundeck.app.spi.Services;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * StepPluginAdapter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class StepPluginAdapter implements StepExecutor, Describable, DynamicProperties{
    public static final Convert CONVERTER = new Convert();
    private StepPlugin plugin;

    public StepPluginAdapter(final StepPlugin plugin) {
        this.plugin = plugin;
    }

    public static class Convert implements Converter<StepPlugin, StepExecutor> {
        @Override
        public StepExecutor convert(final StepPlugin plugin) {
            return new StepPluginAdapter(plugin);
        }
    }

    @Override
    public Map<String, Object> dynamicProperties(Map<String, Object> projectAndFrameworkValues, Services services){
        if(plugin instanceof DynamicProperties){
            return ((DynamicProperties)plugin).dynamicProperties(projectAndFrameworkValues, services);
        }

        return null;
    }

    @Override
    public Map<String, Object> dynamicDefaults(
            final Map<String, Object> projectAndFrameworkValues,
            final Services services
    )
    {
        if(plugin instanceof DynamicProperties){
            return ((DynamicProperties)plugin).dynamicDefaults(projectAndFrameworkValues, services);
        }

        return null;
    }

    @Override
    public Description getDescription() {
        if (plugin instanceof Describable) {
            final Describable desc = (Describable) plugin;
            return desc.getDescription();
        } else {
            return PluginAdapterUtility.buildDescription(plugin, DescriptionBuilder.builder());
        }
    }

    @Override
    public boolean isNodeDispatchStep(StepExecutionItem item) {
        return false;
    }

    @Override
    public StepExecutionResult executeWorkflowStep(final StepExecutionContext executionContext,
                                                   final StepExecutionItem item) throws StepException
        {

        final String providerName = item.getType();
        final PluginStepContext stepContext = PluginStepContextImpl.from(executionContext);
        final Map<String, Object> instanceConfiguration = createConfig(executionContext, item);

        final PropertyResolver resolver = PropertyResolverFactory.createStepPluginRuntimeResolver(executionContext,
                instanceConfiguration,
                ServiceNameConstants.WorkflowStep,
                providerName
        );
        Map<String, Object>  config = PluginAdapterUtility.configureProperties(resolver, getDescription(),plugin, PropertyScope.InstanceOnly);

        try {
            plugin.executeStep(stepContext, config);
        } catch (StepException e) {
            executionContext.getExecutionListener().log(
                    Constants.ERR_LEVEL,
                    e.getMessage()
            );
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            executionContext.getExecutionListener().log(
                    Constants.DEBUG_LEVEL,
                    "Failed executing step plugin [" + providerName + "]: "
                    + stringWriter.toString()
            );

            return new StepExecutionResultImpl(e, e.getFailureReason(), e.getMessage());
        } catch (Throwable e) {
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                    "Failed executing step plugin [" + providerName + "]: "
                            + stringWriter.toString());
            return new StepExecutionResultImpl(e, StepFailureReason.PluginFailed, e.getMessage());
        }
        return new StepExecutionResultImpl();
    }

    public Map<String, Object> createConfig(StepExecutionContext executionContext,
                                  StepExecutionItem item){
        Map<String, Object> instanceConfiguration = getStepConfiguration(item);
        Description description = getDescription();
        Map<String,Boolean> blankIfUnexMap = new HashMap<>();
        if(description != null) {
            description.getProperties().forEach(p -> {
                blankIfUnexMap.put(p.getName(), p.isBlankIfUnexpandable());
            });
        }
        if (null != instanceConfiguration) {
            instanceConfiguration = SharedDataContextUtils.replaceDataReferences(
                    instanceConfiguration,
                    ContextView.global(),
                    ContextView::nodeStep,
                    null,
                    executionContext.getSharedDataContext(),
                    false,
                    blankIfUnexMap
            );
        }

        return instanceConfiguration;

    }

    private Map<String, Object> getStepConfiguration(StepExecutionItem item) {
        if (item instanceof ConfiguredStepExecutionItem) {
            return ((ConfiguredStepExecutionItem) item).getStepConfiguration();
        } else {
            return null;
        }
    }

    public StepPlugin getPlugin() {
        return plugin;
    }
}
