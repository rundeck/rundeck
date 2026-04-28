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

import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
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

    @Override
    public boolean blankIfUnexpanded() {
        return this.blankIfUnexpanded;
    }

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
    private boolean blankIfUnexpanded;

    public RemoteScriptNodeStepPluginAdapter_Ext(final RemoteScriptNodeStepPlugin plugin, final boolean blankIfUnexpanded) {
        this.blankIfUnexpanded = blankIfUnexpanded;
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
            return new RemoteScriptNodeStepPluginAdapter_Ext(plugin, false);
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
            boolean featureQuotingBackwardCompatible = Boolean.valueOf(context.getIFramework().getPropertyRetriever().getProperty("rundeck.feature.quoting.backwardCompatible"));
            // Default true: quoting enabled (secure). Set to false to disable (not recommended).
            String execQuotingEnabledProp = context.getIFramework().getPropertyRetriever().getProperty("rundeck.feature.exec.quoting.enabled");
            boolean execQuotingEnabled = (execQuotingEnabledProp == null || execQuotingEnabledProp.isEmpty())
                    ? true
                    : Boolean.parseBoolean(execQuotingEnabledProp);

            String[] command = script.getCommand();
            // Per-value quoting: expand each template arg with the OS-aware converter so
            // individual ${...} values are quoted while template-level shell operators stay free.
            // ${unquoted.*} refs are exempted from the converter inside replaceDataReferences.
            // Derive the command interpreter from the node attribute first, then fall back to the
            // project property — this ensures Windows cmd.exe nodes use WINDOWS_CMD_ESCAPE instead
            // of single-quote wrapping.
            String commandInterpreter;
            if (node.getAttributes().get("shell-escaping-interpreter") != null) {
                commandInterpreter = node.getAttributes().get("shell-escaping-interpreter");
            } else if (context.getFrameworkProject() != null) {
                commandInterpreter = context.getIFramework().getFrameworkProjectMgr()
                        .getFrameworkProject(context.getFrameworkProject())
                        .getProperty("project.plugin.Shell.Escaping.interpreter");
            } else {
                commandInterpreter = null;
            }
            Converter<String, String> valueConverter = execQuotingEnabled
                    ? CLIUtils.argumentQuoteForOperatingSystem(node.getOsFamily(), commandInterpreter)
                    : null;
            String[] expanded = SharedDataContextUtils.replaceDataReferences(
                    command,
                    context.getSharedDataContext(),
                    ContextView.node(node.getNodename()),
                    ContextView::nodeStep,
                    valueConverter,
                    false,
                    true
            );
            ExecArgList.Builder builder = ExecArgList.builder();
            for (String arg : expanded) {
                // Quoting is already baked into each value — pass shouldQuote=false
                builder.arg(arg, false, featureQuotingBackwardCompatible);
            }

            return executionService.executeCommand(
                    context,
                    builder.build(),
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
