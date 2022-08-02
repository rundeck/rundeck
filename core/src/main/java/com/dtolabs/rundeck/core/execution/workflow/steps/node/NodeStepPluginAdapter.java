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
* NodeStepPluginAdapter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/12/12 5:39 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.PluginStepContextImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import org.rundeck.app.spi.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;


/**
 * NodeStepPluginAdapter implements NodeStepExecutor, and makes use of a {@link NodeStepPlugin} instance to perform the
 * execution.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeStepPluginAdapter implements NodeStepExecutor, Describable, DynamicProperties {
    protected static Logger  log = LoggerFactory.getLogger(NodeStepPluginAdapter.class.getName());
    private          String  serviceName;
    private          boolean blankIfUnexpanded;

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
    public Map<String, Object> dynamicProperties(Map<String, Object> projectAndFrameworkValues, Services services){
        if(plugin instanceof DynamicProperties){
            return ((DynamicProperties)plugin).dynamicProperties(projectAndFrameworkValues, services);
        }

        return null;
    }

    private NodeStepPlugin plugin;

    public NodeStepPluginAdapter(final NodeStepPlugin plugin) {
        this(ServiceNameConstants.WorkflowNodeStep, plugin, true);
    }

    public NodeStepPluginAdapter(
            final String serviceName,
            final NodeStepPlugin plugin,
            final boolean blankIfUnexpanded
    )
    {
        this.serviceName = serviceName;
        this.plugin = plugin;
        this.blankIfUnexpanded = blankIfUnexpanded;
    }

    public static boolean canAdaptType(Class<?> testType){
        return NodeStepPlugin.class.isAssignableFrom(testType);
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }


    public static class ConvertToNodeStepExecutor
            implements Converter<NodeStepPlugin, NodeStepExecutor>
    {
        String serviceName;
        boolean blankIfUnexpanded ;

        public ConvertToNodeStepExecutor(final String serviceName, boolean blankIfUnexpanded) {
            this.serviceName = serviceName;
            this.blankIfUnexpanded = blankIfUnexpanded;
        }

        public ConvertToNodeStepExecutor() {
            this(ServiceNameConstants.WorkflowNodeStep, true);
        }

        public NodeStepExecutor convert(final NodeStepPlugin plugin) {
            return new NodeStepPluginAdapter(serviceName, plugin, blankIfUnexpanded);
        }
    }

    public static final ConvertToNodeStepExecutor CONVERT_TO_NODE_STEP_EXECUTOR = new ConvertToNodeStepExecutor();

    @Override
    public NodeStepResult executeNodeStep(final StepExecutionContext context,
                                          final NodeStepExecutionItem item,
                                          final INodeEntry node)
        throws NodeStepException {

        final String providerName = item.getNodeStepType();
        final PluginStepContext pluginContext = PluginStepContextImpl.from(context);
        final Map<String, Object> config = createConfig(context, item, node);

        try {
            plugin.executeNodeStep(pluginContext, config, node);
        } catch (NodeStepException e) {
            log.error("Error executing node step.", e);
            return new NodeStepResultImpl(e,
                    e.getFailureReason(),
                    e.getMessage(),
                    e.getFailureData(),
                    node);
        } catch (Throwable e) {
            log.error("Uncaught throwable executing node step.", e);
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            context.getExecutionListener().log(Constants.DEBUG_LEVEL,
                    "Failed executing node plugin ["+providerName+"] on node " + node.getNodename() + ": "
                            + stringWriter.toString());
            return new NodeStepResultImpl(e,
                                          StepFailureReason.PluginFailed,
                                          e.getMessage(),
                                          node);
        }
        return new NodeStepResultImpl(node);
    }

    public Map<String, Object> createConfig( StepExecutionContext context,
                                            NodeStepExecutionItem item,
                                            INodeEntry node){
        Map<String, Object> instanceConfiguration = getStepConfiguration(item);
        Description description = getDescription();
        Map<String,Boolean> blankIfUnexMap = new HashMap<>();
        if(description != null) {
            description.getProperties().forEach(p -> {
                if (!p.isBlankIfUnexpandable()) blankIfUnexMap.put(p.getName(), p.isBlankIfUnexpandable());
                else blankIfUnexMap.put(p.getName(), blankIfUnexpanded);
            });
        }
        if (null != instanceConfiguration) {
            instanceConfiguration = SharedDataContextUtils.replaceDataReferences(
                    instanceConfiguration,
                    ContextView.node(node.getNodename()),
                    ContextView::nodeStep,
                    null,
                    context.getSharedDataContext(),
                    false,
                    blankIfUnexMap
            );
        }
        final String providerName = item.getNodeStepType();

        final PropertyResolver resolver = PropertyResolverFactory.createStepPluginRuntimeResolver(context,
                instanceConfiguration,
                getServiceName(),
                providerName);
        return PluginAdapterUtility.configureProperties(resolver, description, plugin, PropertyScope.InstanceOnly);
    }

    public Map<String, Object> getStepConfiguration(StepExecutionItem item) {
        if (item instanceof ConfiguredStepExecutionItem) {
            return ((ConfiguredStepExecutionItem) item).getStepConfiguration();
        } else {
            return null;
        }
    }
}
