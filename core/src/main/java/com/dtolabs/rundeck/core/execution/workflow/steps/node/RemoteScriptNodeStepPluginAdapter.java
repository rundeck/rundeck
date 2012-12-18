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
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.PluginAdapterUtility;
import com.dtolabs.rundeck.core.execution.workflow.steps.PluginStepContextImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.PropertyResolver;
import com.dtolabs.rundeck.core.execution.workflow.steps.PropertyResolverFactory;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.GeneratedScript;
import com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.util.HashMap;
import java.util.Map;


/**
 * RemoteScriptNodeStepPluginAdapter is a NodeStepExecutor that makes use of a RemoteScriptNodeStepPlugin to provide the
 * remote script to execute.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class RemoteScriptNodeStepPluginAdapter implements NodeStepExecutor, Describable {

    @Override
    public Description getDescription() {
        if (plugin instanceof Describable) {
            final Describable desc = (Describable) plugin;
            return desc.getDescription();
        } else {
            return PluginAdapterUtility.buildDescription(plugin, DescriptionBuilder.builder());
        }
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

        Map<String, Object> instanceConfiguration = getStepConfiguration(item);
        if (null != instanceConfiguration) {
            instanceConfiguration = DataContextUtils.replaceDataReferences(instanceConfiguration,
                                                                           context.getDataContext());
        }
        final String providerName = item.getNodeStepType();
        final PropertyResolver resolver = PropertyResolverFactory.createStepPluginRuntimeResolver(context,
                                                                                                  instanceConfiguration,
                                                                                                  ServiceNameConstants.RemoteScriptNodeStep,
                                                                                                  providerName
        );
        final PluginStepContextImpl pluginContext = PluginStepContextImpl.from(context);
        Description description = getDescription();
        final Map<String, Object> config = PluginAdapterUtility.configureProperties(resolver, description, plugin);

        final GeneratedScript script = plugin.generateScript(pluginContext, config, node);

        //get all plugin config properties, and add to the data context used when executing the remote script
        final Map<String, Object> allconfig = PluginAdapterUtility.mapDescribedProperties(resolver, description);
        final Map<String, String> stringconfig = new HashMap<String, String>();
        for (final Map.Entry<String, Object> objectEntry : allconfig.entrySet()) {
            stringconfig.put(objectEntry.getKey(), objectEntry.getValue().toString());
        }
        final Map<String, Map<String, String>> configDataContext =
            DataContextUtils.addContext("config", stringconfig, context.getDataContext());

        return executeRemoteScript(ExecutionContextImpl
                                       .builder(context)
                                       .dataContext(configDataContext)
                                       .build(),
                                   node,
                                   script);
    }

    static NodeStepResult executeRemoteScript(final StepExecutionContext context,
                                              final INodeEntry node,
                                              final GeneratedScript script)
        throws NodeStepException {
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
        } else if (script instanceof FileBasedGeneratedScript) {
            final FileBasedGeneratedScript fileScript = (FileBasedGeneratedScript) script;
            final String filepath; //result file path
            try {
                filepath = executionService.fileCopyFile(context, fileScript.getScriptFile(), node);
            } catch (FileCopierException e) {
                throw new NodeStepException(e, node.getNodename());
            }
            return ScriptFileNodeStepExecutor.executeRemoteScript(context,
                                                                  context.getFramework(),
                                                                  node,
                                                                  script.getArgs(),
                                                                  filepath,
                                                                  fileScript.getScriptInterpreter(),
                                                                  fileScript.isInterpreterArgsQuoted());

        } else {
            return new NodeStepResultImpl(false, node);
        }
    }

    private Map<String, Object> getStepConfiguration(StepExecutionItem item) {
        if (item instanceof ConfiguredStepExecutionItem) {
            return ((ConfiguredStepExecutionItem) item).getStepConfiguration();
        } else {
            return null;
        }
    }

}
