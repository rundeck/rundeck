package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.cli.CLIUtils
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import com.dtolabs.rundeck.core.utils.OptsUtil
import org.rundeck.core.execution.ExecCommand
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.PluginStepContext

@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = EXEC_COMMAND_TYPE)
@PluginDescription(title = "Command", description = "Execute a remote command", isHighlighted = true, order = 0)
class CommandNodeStepPlugin extends ScriptProxyRunner implements NodeStepPlugin, ExecCommand, PluginResourceLoader {

    @PluginProperty(title = "Command",
            description = "Enter the shell command, e.g.: echo this is a test",
            required = true)
    String adhocRemoteString;

    @Override
    void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) throws NodeStepException {
        boolean featureQuotingBackwardCompatible = Boolean.valueOf(context.getExecutionContext().getIFramework()
                .getPropertyRetriever().getProperty("rundeck.feature.quoting.backwardCompatible"));
        
        // Default true: quoting enabled (secure). Set to false to disable (not recommended).
        String execQuotingEnabledProp = context.getExecutionContext().getIFramework()
                .getPropertyRetriever().getProperty("rundeck.feature.exec.quoting.enabled")
        boolean execQuotingEnabled = (execQuotingEnabledProp == null || execQuotingEnabledProp.isEmpty())
                ? true
                : Boolean.parseBoolean(execQuotingEnabledProp)

        def arr = OptsUtil.burst(adhocRemoteString)

        // Derive the command interpreter from the node attribute first, then fall back to the
        // project-level property — matching the same resolution order used in ExecutionServiceImpl.
        // For Windows cmd.exe nodes the interpreter must be "cmd" to select WINDOWS_CMD_ESCAPE
        // (caret-prefix escaping) instead of single-quote wrapping.
        String commandInterpreter = entry.getAttributes()?.get("shell-escaping-interpreter") ?:
                context.getExecutionContext().getIFramework()
                        .getFrameworkProjectMgr()
                        .getFrameworkProject(context.getExecutionContext().getFrameworkProject())
                        .getProperty("project.plugin.Shell.Escaping.interpreter")

        // Per-value quoting: each ${...} expanded value is individually quoted using the
        // OS-aware converter. ${unquoted.*} refs are exempted from the converter during
        // SharedDataContextUtils.replaceDataReferences(...). Template-level shell operators
        // (;, &&, |) written by the job author stay outside any quoted value and are preserved.
        def valueConverter = execQuotingEnabled
                ? CLIUtils.argumentQuoteForOperatingSystem(entry.getOsFamily(), commandInterpreter)
                : null

        def result = SharedDataContextUtils.replaceDataReferences(
                arr,
                context.getExecutionContext().getSharedDataContext(),
                ContextView.node(entry.getNodename()),
                ContextView::nodeStep,
                valueConverter,
                false,
                true
        )

        // Quoting is already baked into each value — pass shouldQuote=false to avoid
        // double-quoting at the token level.
        def execArgListBuilder = ExecArgList.builder()
        for (int i = 0; i < result.length; i++) {
            execArgListBuilder.arg(result[i], false, featureQuotingBackwardCompatible)
        }

        NodeExecutorResult nodeExecutorResult =  context.getFramework().getExecutionService().executeCommand(
                context.getExecutionContext(),
                execArgListBuilder.build(),
                entry
        );

        Util.handleFailureResult(nodeExecutorResult, entry)
    }

    @Override
    List<String> listResources() throws PluginException, IOException {
        ['WorkflowNodeStep.exec-command.icon.png']
    }

    @Override
    InputStream openResourceStreamFor(String name) throws PluginException, IOException {
        return this.getClass().getResourceAsStream("/" + name)
    }
}
