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
* RemoteScriptNodeStepPluginAdapter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/19/12 6:09 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.PluginStepContextImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.PropertyResolverFactory;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.GeneratedScript;
import com.dtolabs.rundeck.plugins.step.PluginStepItem;
import com.dtolabs.rundeck.plugins.step.PropertyResolver;
import com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin;


/**
 * RemoteScriptNodeStepPluginAdapter is a NodeStepExecutor that makes use of a RemoteScriptNodeStepPlugin to
 * provide the remote script to execute.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class RemoteScriptNodeStepPluginAdapter implements NodeStepExecutor, Describable {
    @Override
    public Description getDescription() {
        if (plugin instanceof Describable) {
            final Describable desc = (Describable) plugin;
            return desc.getDescription();
        }
        return null;
    }

    private RemoteScriptNodeStepPlugin plugin;

    public RemoteScriptNodeStepPluginAdapter(final RemoteScriptNodeStepPlugin plugin) {
        this.plugin = plugin;
    }

    static class Convert implements Converter<RemoteScriptNodeStepPlugin, NodeStepExecutor> {
        @Override
        public NodeStepExecutor convert(final RemoteScriptNodeStepPlugin plugin) {
            return new RemoteScriptNodeStepPluginAdapter(plugin);
        }
    }

    public static final Convert CONVERTER = new Convert();

    @Override
    public NodeStepResult executeNodeStep(final StepExecutionContext context,
                                          final NodeStepExecutionItem item,
                                          final INodeEntry node)
        throws NodeStepException {

        final PluginStepItem item1 = NodeStepPluginAdapter.toPluginStepItem(item, context);
        final PropertyResolver resolver = PropertyResolverFactory.createStepPluginRuntimeResolver(context,
                                                                                                  ServiceNameConstants.RemoteScriptNodeStep,
                                                                                                  item1
        );
        final PluginStepContextImpl pluginContext = PluginStepContextImpl.from(context, resolver);
        final GeneratedScript script = plugin.generateScript(pluginContext, item1, node);
        final ExecutionService executionService = context.getFramework().getExecutionService();
        if (null != script.getCommand()) {
            //execute the command
            try {
                return executionService.executeCommand(context, script.getCommand(), node);
            } catch (ExecutionException e) {
                throw new NodeStepException(e, node.getNodename());
            }
        } else if (null != script.getScript()) {
            final String filepath; //result file path
            try {
                filepath = executionService.fileCopyScriptContent(context, script.getScript(), node);
            } catch (FileCopierException e) {
                throw new NodeStepException(e, node.getNodename());
            }
            return ScriptFileNodeStepExecutor.executeRemoteScript(context,
                                                                  context.getFramework(),
                                                                  node,
                                                                  script.getArgs(),
                                                                  filepath);

        } else {
            return new NodeStepResultImpl(false, node);
        }
    }

}
