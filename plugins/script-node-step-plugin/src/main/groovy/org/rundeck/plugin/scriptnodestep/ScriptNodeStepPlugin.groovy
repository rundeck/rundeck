package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.INodeEntry
import org.rundeck.core.execution.ScriptCommand
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.PluginStepContext

@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = SCRIPT_COMMAND_TYPE)
@PluginDescription(title = "Script", description = "Run a script on the remote node", isHighlighted = true, order = 1)
class ScriptNodeStepPlugin extends ScriptProxyRunner implements NodeStepPlugin, ScriptCommand  {
    public static final String PROVIDER_NAME = "script-node-step-plugin";

    @PluginProperty(
            title = "Enter the entire script to execute",
            description = "",
            required = true
    )
    @RenderingOptions(
            [
                    @RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = 'CODE'),
                    @RenderingOption(key = 'codeSyntaxMode', value = 'sh')
            ]
    )
    String adhocLocalString

    @PluginProperty(title = "Arguments",
            description = "Arguments",
            required = false)
    String argString;

    @PluginProperty(title = "Invocation String",
            description = "",
            required = false)
    String scriptInterpreter;

    @PluginProperty(title = "Quote arguments to script invocation string?",
            description = "",
            required = false)
    Boolean interpreterArgsQuoted;

    @PluginProperty(title = "File Extension",
            description = "",
            required = false)
    String fileExtension;

    @Override
    void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) throws NodeStepException {
        ScriptFileNodeStepExecutor scriptFileNodeStepExecutor = new ScriptFileNodeStepExecutor(
                scriptInterpreter,
                interpreterArgsQuoted,
                fileExtension,
                argString,
                null,
                adhocLocalString,
                true
        );

        scriptFileNodeStepExecutor.executeScriptFile(context, configuration, entry)
    }
}
