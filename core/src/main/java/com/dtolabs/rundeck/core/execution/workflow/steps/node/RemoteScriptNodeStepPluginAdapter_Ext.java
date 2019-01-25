/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.DefaultScriptFileNodeStepUtils;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepUtils;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.step.*;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Adapts a RemoteScriptNodeStepPlugin into a NodeStepPlugin
 */
public class RemoteScriptNodeStepPluginAdapter_Ext
        implements NodeStepPlugin, Configurable, Describable
{

    private ScriptFileNodeStepUtils scriptUtils = new DefaultScriptFileNodeStepUtils();

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

    public RemoteScriptNodeStepPluginAdapter_Ext(final RemoteScriptNodeStepPlugin plugin) {
        this.plugin = plugin;
    }

    public ScriptFileNodeStepUtils getScriptUtils() {
        return scriptUtils;
    }

    public void setScriptUtils(ScriptFileNodeStepUtils scriptUtils) {
        this.scriptUtils = scriptUtils;
    }

    static class NodeStepPluginConverter
            implements Converter<RemoteScriptNodeStepPlugin, NodeStepPlugin>
    {
        @Override
        public NodeStepPlugin convert(final RemoteScriptNodeStepPlugin plugin) {
            return new RemoteScriptNodeStepPluginAdapter_Ext(plugin);
        }
    }

    //    public static final Convert CONVERTER = new Convert();
    public static final RemoteScriptNodeStepPluginAdapter_Ext.NodeStepPluginConverter
            CONVERT_TO_NODE_STEP_PLUGIN = new RemoteScriptNodeStepPluginAdapter_Ext.NodeStepPluginConverter();

    @Override
    public void configure(final Properties configuration) throws ConfigurationException {

        final Map<String, Object>
                config =
                PluginAdapterUtility.configureProperties(
                        PropertyResolverFactory.createInstanceResolver(configuration),
                        getDescription(),
                        plugin,
                        PropertyScope.InstanceOnly
                );
    }

    @Override
    public void executeNodeStep(
            final PluginStepContext pluginContext, final Map<String, Object> config, final INodeEntry node
    ) throws NodeStepException
    {
        final GeneratedScript script;
        try {
            script = plugin.generateScript(pluginContext, config, node);
        } catch (RuntimeException e) {
            throw new NodeStepException(e.getMessage(), StepFailureReason.PluginFailed, node.getNodename());
        }

        //get all plugin config properties, and add to the data context used when executing the remote script
        final Map<String, String> stringconfig = new HashMap<>();
        for (final Map.Entry<String, Object> objectEntry : config.entrySet()) {
            stringconfig.put(objectEntry.getKey(), objectEntry.getValue().toString());
        }

        ExecutionContextImpl.Builder builder = ExecutionContextImpl
                .builder(pluginContext.getExecutionContext())
                .setContext("config", stringconfig);
        if (plugin.hasAdditionalConfigVarGroupName()) {
            //new context variable name
            builder.setContext("nodestep", stringconfig);
        }
        ExecutionContextImpl newContext = builder.build();


        NodeStepResult nodeStepResult = executeRemoteScript(
                newContext,
                node,
                script,
                pluginContext.getExecutionContext().getDataContextObject().resolve("job", "execid"),
                getDescription().getName(), scriptUtils
        );
        if (!nodeStepResult.isSuccess()) {
            throw new NodeStepException(
                    nodeStepResult.getFailureMessage() != null
                    ? nodeStepResult.getFailureMessage()
                    : "Remote script execution failed",
                    nodeStepResult.getFailureReason(),
                    nodeStepResult.getFailureData(),
                    node.getNodename()
            );
        }

    }

    public static NodeStepResult executeRemoteScript(
            final StepExecutionContext context,
            final INodeEntry node,
            final GeneratedScript script,
            String ident,
            String providerName,
            final ScriptFileNodeStepUtils scriptUtils
    )
            throws NodeStepException
    {
        final ExecutionService executionService = context.getFramework().getExecutionService();
        boolean expandTokens = true;
        if (context.getFramework().hasProperty("execution.script.tokenexpansion.enabled")) {
            expandTokens = "true".equals(context.getFramework().getProperty("execution.script.tokenexpansion.enabled"));
        }


        if (null != script.getCommand()) {
            //execute the command
            return executionService.executeCommand(
                    context,
                    ExecArgList.fromStrings(
                            DataContextUtils.stringContainsPropertyReferencePredicate,
                            script.getCommand()
                    ),
                    node
            );
        } else if (null != script.getScript()) {

            String destpath = BaseFileCopier.generateRemoteFilepathForNode(
                    node,
                    context.getFramework().getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()),
                    context.getFramework(),
                    providerName + "-script",
                    getFileExtension(script),
                    ident
            );
            final String filepath; //result file path
            try {
                filepath = executionService.fileCopyScriptContent(context, script.getScript(), node, destpath);
            } catch (FileCopierException e) {
                throw new NodeStepException(e.getMessage(), e, e.getFailureReason(), node.getNodename());
            }
            return scriptUtils.executeRemoteScript(
                    context,
                    context.getFramework(),
                    node,
                    script.getArgs(),
                    filepath
            );
        } else if (script instanceof FileBasedGeneratedScript) {
            final FileBasedGeneratedScript fileScript = (FileBasedGeneratedScript) script;
            //merge any config context data
            StepExecutionContext newcontext =
                    fileScript.getConfigData() != null
                    ? ExecutionContextImpl.builder(context).mergeContext("config", fileScript.getConfigData()).build()
                    : context;
            return scriptUtils.executeScriptFile(
                    newcontext,
                    node,
                    null,
                    fileScript.getScriptFile().getAbsolutePath(),
                    null,
                    fileScript.getFileExtension(),
                    fileScript.getArgs(),
                    fileScript.getScriptInterpreter(),
                    fileScript.isInterpreterArgsQuoted(),
                    executionService,
                    expandTokens
            );
        } else {
            return new NodeStepResultImpl(
                    null,
                    StepFailureReason.ConfigurationFailure,
                    "Generated script must have a command or script defined",
                    node
            );
        }
    }

    private static String getFileExtension(final GeneratedScript script) {
        if (script instanceof FileExtensionGeneratedScript) {
            return ((FileExtensionGeneratedScript) script).getFileExtension();
        }
        return null;
    }

    private Map<String, Object> getStepConfiguration(StepExecutionItem item) {
        if (item instanceof ConfiguredStepExecutionItem) {
            return ((ConfiguredStepExecutionItem) item).getStepConfiguration();
        } else {
            return null;
        }
    }
}
