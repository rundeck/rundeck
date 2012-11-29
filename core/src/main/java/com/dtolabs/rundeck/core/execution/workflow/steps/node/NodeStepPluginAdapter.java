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

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.PluginStepContextImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.PluginStepItemImpl;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.PluginStepItem;

import java.util.*;


/**
 * NodeStepPluginAdapter implements NodeStepExecutor, and makes use of a {@link NodeStepPlugin}
 * instance to perform the execution.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class NodeStepPluginAdapter implements NodeStepExecutor, Describable {
    @Override
    public Description getDescription() {
        if (plugin instanceof Describable) {
            final Describable desc = (Describable) plugin;
            return desc.getDescription();
        }
        return null;
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
        final PluginStepItem step = toPluginStepItem(item, context);
        final PluginStepContext pluginContext = PluginStepContextImpl.from(context);
        final boolean success = plugin.executeNodeStep(pluginContext, step, node);
        return new NodeStepResultImpl(success, node);
    }

    static PluginStepItem toPluginStepItem(final NodeStepExecutionItem item, final ExecutionContext executionContext) {
        if (!(item instanceof PluginStepItem)) {
            return new PluginStepItemImpl(item.getNodeStepType(), null);
        }

        final PluginStepItem step = (PluginStepItem) item;
        if (step.getStepConfiguration() == null) {
            return step;
        }
        final Map<String, Object> map = DataContextUtils.replaceDataReferences(step.getStepConfiguration(),
                                                                               executionContext.getDataContext());
        return new PluginStepItemImpl(item.getNodeStepType(), map);
    }
}
