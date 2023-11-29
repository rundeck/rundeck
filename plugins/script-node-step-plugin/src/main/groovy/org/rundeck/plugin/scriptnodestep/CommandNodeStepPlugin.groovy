package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecCommand
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.proxy.ProxyRunnerPlugin
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.plugins.ExecutionEnvironmentConstants
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginMetadata
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.PluginStepContext

@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = EXEC_COMMAND_TYPE)
@PluginDescription(title = "Command", description = "Run a command on the remote node", isHighlighted = true, order = 0)
@PluginMetadata(key = ExecutionEnvironmentConstants.ENVIRONMENT_TYPE_KEY, value = ExecutionEnvironmentConstants.LOCAL_RUNNER)
class CommandNodeStepPlugin implements NodeStepPlugin, ExecCommand, ProxyRunnerPlugin {

    @PluginProperty(title = "Command",
            description = "",
            required = true)
    String adhocRemoteString;

    @Override
    void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) throws NodeStepException {
        boolean featureQuotingBackwardCompatible = Boolean.valueOf(context.getExecutionContext().getIFramework()
                .getPropertyRetriever().getProperty("rundeck.feature.quoting.backwardCompatible"));

        NodeExecutorResult nodeExecutorResult =  context.getFramework().getExecutionService().executeCommand(
                context.getExecutionContext(),
                ExecArgList.fromStrings(featureQuotingBackwardCompatible, DataContextUtils
                .stringContainsPropertyReferencePredicate, adhocRemoteString),
                entry
        );

        if(nodeExecutorResult.resultCode != 0){
            throw new NodeStepException( nodeExecutorResult.failureMessage, nodeExecutorResult.failureReason, entry.getNodename())
        }
    }

    @Override
    Map<String, String> getRuntimeProperties(ExecutionContext context) {
        return context.getFramework().getFrameworkProjectMgr().loadProjectConfig(context.frameworkProject).getProjectProperties()
    }

    @Override
    Map<String, String> getRuntimeFrameworkProperties(ExecutionContext context){
        return context.getIFramework().getPropertyLookup().getPropertiesMap()
    }
}
