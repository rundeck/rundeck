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
* NodeStepPluginAdapter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/12/12 5:39 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import com.dtolabs.rundeck.core.Constants;
import org.apache.log4j.Logger;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.PluginStepContextImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;


/**
 * NodeStepPluginAdapter implements NodeStepExecutor, and makes use of a {@link NodeStepPlugin} instance to perform the
 * execution.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class NodeStepPluginAdapter implements NodeStepExecutor, Describable {
    protected static Logger log = Logger.getLogger(NodeStepPluginAdapter.class.getName());

    @Override
    public Description getDescription() {
        if (plugin instanceof Describable) {
            final Describable desc = (Describable) plugin;
            return desc.getDescription();
        } else {
            return PluginAdapterUtility.buildDescription(plugin, DescriptionBuilder.builder());
        }
    }

    private NodeStepPlugin plugin;

    public NodeStepPluginAdapter(final NodeStepPlugin plugin) {
        this.plugin = plugin;
    }

    static class Convert implements Converter<NodeStepPlugin, NodeStepExecutor> {
        public NodeStepExecutor convert(final NodeStepPlugin plugin) {
            return new NodeStepPluginAdapter(plugin);
        }
    }

    public static final Convert CONVERTER = new Convert();

    @Override
    public NodeStepResult executeNodeStep(final StepExecutionContext context,
                                          final NodeStepExecutionItem item,
                                          final INodeEntry node)
        throws NodeStepException {
        Map<String, Object> instanceConfiguration = getStepConfiguration(item);
        if (null != instanceConfiguration) {
            instanceConfiguration = DataContextUtils.replaceDataReferences(instanceConfiguration,
                                                                           context.getDataContext());
        }
        final String providerName = item.getNodeStepType();

        final PropertyResolver resolver = PropertyResolverFactory.createStepPluginRuntimeResolver(context,
                                                                                                  instanceConfiguration,
                                                                                                  ServiceNameConstants.WorkflowNodeStep,
                                                                                                  providerName);
        final PluginStepContext pluginContext = PluginStepContextImpl.from(context);
        final Map<String, Object> config = PluginAdapterUtility.configureProperties(resolver, getDescription(), plugin, PropertyScope.InstanceOnly);
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

    private Map<String, Object> getStepConfiguration(StepExecutionItem item) {
        if (item instanceof ConfiguredStepExecutionItem) {
            return ((ConfiguredStepExecutionItem) item).getStepConfiguration();
        } else {
            return null;
        }
    }
}
