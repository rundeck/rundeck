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
import com.dtolabs.rundeck.core.data.BaseDataContext;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MultiDataContext;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;
import com.dtolabs.rundeck.core.data.UnexpandableBehavior;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import org.rundeck.app.spi.Services;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
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
        final Description description = getDescription();
        Map<String, Object>  config = PluginAdapterUtility.configureProperties(resolver, description,plugin, PropertyScope.InstanceOnly);

        captureOutputMetadataValues(executionContext, description, config);

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

    /**
     * Write the resolved value of any property carrying {@code @PluginOutput} metadata into the
     * step's shared data context, so it can be referenced by conditional-logic steps later in the
     * workflow (as {@code "<Step Label> - <Property Name>"}, resolved to {@code ${N:group.name}}).
     *
     * @param executionContext current step execution context
     * @param description      the plugin's description, providing property output metadata
     * @param config           the resolved/expanded instance configuration for this step
     */
    private void captureOutputMetadataValues(
            final StepExecutionContext executionContext,
            final Description description,
            final Map<String, Object> config
    )
    {
        if (description == null || description.getProperties() == null) {
            return;
        }
        final MultiDataContext<ContextView, DataContext> sharedContext = executionContext.getSharedDataContext();
        if (sharedContext == null) {
            return;
        }
        final ContextView stepView = ContextView.step(executionContext.getStepNumber());
        for (final Property property : description.getProperties()) {
            final List<PluginOutputMetadata> outputMetadata = property.getOutputMetadata();
            if (outputMetadata == null || outputMetadata.isEmpty()) {
                continue;
            }
            final Object value = resolvePropertyValue(property, config);
            if (value == null) {
                continue;
            }
            for (final PluginOutputMetadata metadata : outputMetadata) {
                final Map<String, String> data = new HashMap<>();
                data.put(metadata.getName(), value.toString());
                sharedContext.merge(stepView, new BaseDataContext(metadata.getGroup(), data));
            }
        }
    }

    /**
     * Resolve a property's configured value. Properties that map to an actual field on the plugin
     * instance are set directly on that field (and removed from the returned instance configuration
     * map) by {@link PluginAdapterUtility#configureProperties}, so the value is read back off the
     * plugin instance's field in that (common) case; otherwise it's read from the leftover config map.
     */
    private Object resolvePropertyValue(final Property property, final Map<String, Object> config) {
        if (config != null && config.containsKey(property.getName())) {
            return config.get(property.getName());
        }
        final Field field = new PluginAdapterImpl().fieldForPropertyName(property.getName(), plugin);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(plugin);
        } catch (IllegalAccessException e) {
            return null;
        }
    }


    public Map<String, Object> createConfig(StepExecutionContext executionContext,
                                  StepExecutionItem item){
        Map<String, Object> instanceConfiguration = getStepConfiguration(item);
        Description description = getDescription();
        CustomFieldsAdapter customFieldsAdapter = CustomFieldsAdapter.create(description);
        Map<String, UnexpandableBehavior> behaviorMap =
                UnexpandableBehaviorSupport.buildBehaviorMap(
                        description, true, instanceConfiguration);
        if (null != instanceConfiguration) {
            instanceConfiguration = SharedDataContextUtils.replaceDataReferencesWithBehavior(
                    instanceConfiguration,
                    ContextView.global(),
                    ContextView::nodeStep,
                    null,
                    executionContext.getSharedDataContext(),
                    false,
                    behaviorMap,
                    customFieldsAdapter::convertInput,
                    customFieldsAdapter::convertOutput
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
