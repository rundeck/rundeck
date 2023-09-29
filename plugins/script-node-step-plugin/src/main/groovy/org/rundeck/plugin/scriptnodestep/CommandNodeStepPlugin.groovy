package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecCommand
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.PluginStepContext

@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = CommandNodeStepPlugin.EXEC_COMMAND_TYPE)
@PluginDescription(title = "Command", description = "Run a command on the remote node", isHighlighted = true, order = 0)
class CommandNodeStepPlugin implements NodeStepPlugin, ExecCommand {

    @PluginProperty(title = "Command",
            description = "",
            required = true)
    String adhocRemoteString;

    @Override
    void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) throws NodeStepException {
        context.getFramework().getExecutionService().executeCommand(context, adhocRemoteString, entry);
    }
}
