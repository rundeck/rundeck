package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.cli.CLIUtils
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification

class CommandNodeStepPluginSpec extends Specification {

    def "per-value quoting preserves template semicolon for safe values"() {
        given:
            def fwkProps = [
                    'rundeck.feature.quoting.backwardCompatible': 'false',
                    'rundeck.feature.exec.quoting.enabled'      : 'true',
            ]
            def projectMock = Mock(IRundeckProject) {
                getProperty("project.plugin.Shell.Escaping.interpreter") >> null
            }
            def projectMgrMock = Mock(ProjectManager) {
                getFrameworkProject(_) >> projectMock
            }
            def iFrameworkMock = Mock(IFramework) {
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
                getFrameworkProjectMgr() >> projectMgrMock
            }

            WFSharedContext sharedContext = SharedDataContextUtils.sharedContext()
            sharedContext.merge(ContextView.global(), DataContextUtils.context("data", [home_dir: '/workspace']))
            sharedContext.merge(ContextView.global(), DataContextUtils.context("job", [execid: 'exec123']))

            def execCtx = Mock(ExecutionContext) {
                getIFramework() >> iFrameworkMock
                getSharedDataContext() >> sharedContext
                getFrameworkProject() >> 'testProject'
            }
            def execService = Mock(ExecutionService)
            // PluginStepContext.getFramework() returns Framework (concrete) — mock the concrete class
            def framework = Mock(Framework) {
                getExecutionService() >> execService
            }
            def context = Mock(PluginStepContext) {
                getExecutionContext() >> execCtx
                getFramework() >> framework
            }
            def node = new NodeEntryImpl('node')
            node.setOsFamily('unix')

            def plugin = new CommandNodeStepPlugin()
            plugin.adhocRemoteString = 'cd ${data.home_dir}/${job.execid};'

        when:
            List<String> captured = null
            execService.executeCommand(_, _, node) >> { args ->
                captured = ((ExecArgList) args[1]).buildCommandForNode([:], 'unix')
                Mock(NodeExecutorResult) { isSuccess() >> true }
            }
            plugin.executeNodeStep(context, [:], node)

        then:
            captured != null
            captured[0] == 'cd'
            // Safe values pass through quoteUnixShellArg unchanged; semicolon is a template-level separator
            captured[1] == '/workspace/exec123;'
    }

    def "per-value quoting blocks injection via expanded option value"() {
        given:
            def fwkProps = [
                    'rundeck.feature.quoting.backwardCompatible': 'false',
                    'rundeck.feature.exec.quoting.enabled'      : 'true',
            ]
            def projectMock = Mock(IRundeckProject) {
                getProperty("project.plugin.Shell.Escaping.interpreter") >> null
            }
            def projectMgrMock = Mock(ProjectManager) {
                getFrameworkProject(_) >> projectMock
            }
            def iFrameworkMock = Mock(IFramework) {
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
                getFrameworkProjectMgr() >> projectMgrMock
            }

            WFSharedContext sharedContext = SharedDataContextUtils.sharedContext()
            sharedContext.merge(ContextView.global(), DataContextUtils.context("option", [port: '80; whoami']))

            def execCtx = Mock(ExecutionContext) {
                getIFramework() >> iFrameworkMock
                getSharedDataContext() >> sharedContext
                getFrameworkProject() >> 'testProject'
            }
            def execService = Mock(ExecutionService)
            def framework = Mock(Framework) {
                getExecutionService() >> execService
            }
            def context = Mock(PluginStepContext) {
                getExecutionContext() >> execCtx
                getFramework() >> framework
            }
            def node = new NodeEntryImpl('node')
            node.setOsFamily('unix')

            def plugin = new CommandNodeStepPlugin()
            plugin.adhocRemoteString = 'echo ${option.port}'

        when:
            List<String> captured = null
            execService.executeCommand(_, _, node) >> { args ->
                captured = ((ExecArgList) args[1]).buildCommandForNode([:], 'unix')
                Mock(NodeExecutorResult) { isSuccess() >> true }
            }
            plugin.executeNodeStep(context, [:], node)

        then:
            captured != null
            // Injected shell operator is trapped inside single quotes
            captured[1] == "'80; whoami'"
    }

    def "full multi-command string: semicolon separates commands and option value with special chars is quoted"() {
        given:
            def fwkProps = [
                    'rundeck.feature.quoting.backwardCompatible': 'false',
                    'rundeck.feature.exec.quoting.enabled'      : 'true',
            ]
            def projectMock = Mock(IRundeckProject) {
                getProperty("project.plugin.Shell.Escaping.interpreter") >> null
            }
            def projectMgrMock = Mock(ProjectManager) {
                getFrameworkProject(_) >> projectMock
            }
            def iFrameworkMock = Mock(IFramework) {
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
                getFrameworkProjectMgr() >> projectMgrMock
            }

            WFSharedContext sharedContext = SharedDataContextUtils.sharedContext()
            sharedContext.merge(ContextView.global(), DataContextUtils.context("data", [home_dir: '/workspace']))
            sharedContext.merge(ContextView.global(), DataContextUtils.context("job", [execid: 'exec123']))
            // option.json value contains a semicolon — a common injection attempt
            sharedContext.merge(ContextView.global(), DataContextUtils.context("option", [json: '{"key": "val; whoami"}']))

            def execCtx = Mock(ExecutionContext) {
                getIFramework() >> iFrameworkMock
                getSharedDataContext() >> sharedContext
                getFrameworkProject() >> 'testProject'
            }
            def execService = Mock(ExecutionService)
            def framework = Mock(Framework) {
                getExecutionService() >> execService
            }
            def context = Mock(PluginStepContext) {
                getExecutionContext() >> execCtx
                getFramework() >> framework
            }
            def node = new NodeEntryImpl('node')
            node.setOsFamily('unix')

            def plugin = new CommandNodeStepPlugin()
            // Full two-command string as a job author would write it.
            // OptsUtil.burst() splits on whitespace only, so the semicolon stays glued
            // to the end of the second token: ['cd', '${data.home_dir}/${job.execid};', 'echo', '${option.json}']
            plugin.adhocRemoteString = 'cd ${data.home_dir}/${job.execid}; echo ${option.json}'

        when:
            List<String> captured = null
            execService.executeCommand(_, _, node) >> { args ->
                captured = ((ExecArgList) args[1]).buildCommandForNode([:], 'unix')
                Mock(NodeExecutorResult) { isSuccess() >> true }
            }
            plugin.executeNodeStep(context, [:], node)

        then:
            captured != null
            // burst splits into 4 tokens
            captured.size() == 4
            // First command: 'cd' + safe path, semicolon stays on the path token
            captured[0] == 'cd'
            captured[1] == '/workspace/exec123;'
            // Second command is captured as its own elements — not swallowed by 'cd'
            captured[2] == 'echo'
            // option.json contains shell-special chars → must be single-quoted (injection blocked)
            captured[3].startsWith("'") && captured[3].endsWith("'")
            captured[3].contains('val; whoami')
    }

    def "per-value quoting does not quote unquoted refs"() {
        given:
            def fwkProps = [
                    'rundeck.feature.quoting.backwardCompatible': 'false',
                    'rundeck.feature.exec.quoting.enabled'      : 'true',
            ]
            def projectMock = Mock(IRundeckProject) {
                getProperty("project.plugin.Shell.Escaping.interpreter") >> null
            }
            def projectMgrMock = Mock(ProjectManager) {
                getFrameworkProject(_) >> projectMock
            }
            def iFrameworkMock = Mock(IFramework) {
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
                getFrameworkProjectMgr() >> projectMgrMock
            }

            WFSharedContext sharedContext = SharedDataContextUtils.sharedContext()
            sharedContext.merge(ContextView.global(), DataContextUtils.context("option", [port: '80 && whoami']))

            def execCtx = Mock(ExecutionContext) {
                getIFramework() >> iFrameworkMock
                getSharedDataContext() >> sharedContext
                getFrameworkProject() >> 'testProject'
            }
            def execService = Mock(ExecutionService)
            def framework = Mock(Framework) {
                getExecutionService() >> execService
            }
            def context = Mock(PluginStepContext) {
                getExecutionContext() >> execCtx
                getFramework() >> framework
            }
            def node = new NodeEntryImpl('node')
            node.setOsFamily('unix')

            def plugin = new CommandNodeStepPlugin()
            plugin.adhocRemoteString = 'echo ${unquotedoption.port}'

        when:
            List<String> captured = null
            execService.executeCommand(_, _, node) >> { args ->
                captured = ((ExecArgList) args[1]).buildCommandForNode([:], 'unix')
                Mock(NodeExecutorResult) { isSuccess() >> true }
            }
            plugin.executeNodeStep(context, [:], node)

        then:
            captured != null
            // ${unquoted.*} refs must NOT be quoted by the converter
            captured[1] == '80 && whoami'
    }

    def "Windows cmd interpreter: values with shell-special chars use cmd escaping not single-quote wrapping"() {
        given:
            def fwkProps = [
                    'rundeck.feature.quoting.backwardCompatible': 'false',
                    'rundeck.feature.exec.quoting.enabled'      : 'true',
            ]
            // No project-level property needed — node attribute takes precedence
            def projectMock = Mock(IRundeckProject) {
                getProperty("project.plugin.Shell.Escaping.interpreter") >> null
            }
            def projectMgrMock = Mock(ProjectManager) {
                getFrameworkProject(_) >> projectMock
            }
            def iFrameworkMock = Mock(IFramework) {
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
                getFrameworkProjectMgr() >> projectMgrMock
            }

            WFSharedContext sharedContext = SharedDataContextUtils.sharedContext()
            sharedContext.merge(ContextView.global(), DataContextUtils.context("option", [val: 'foo & whoami']))

            def execCtx = Mock(ExecutionContext) {
                getIFramework() >> iFrameworkMock
                getSharedDataContext() >> sharedContext
                getFrameworkProject() >> 'testProject'
            }
            def execService = Mock(ExecutionService)
            def framework = Mock(Framework) {
                getExecutionService() >> execService
            }
            def context = Mock(PluginStepContext) {
                getExecutionContext() >> execCtx
                getFramework() >> framework
            }
            def node = new NodeEntryImpl('node')
            node.setOsFamily('windows')
            // Signal that this node uses cmd.exe as the command interpreter
            node.setAttribute('shell-escaping-interpreter', 'cmd')

            def plugin = new CommandNodeStepPlugin()
            plugin.adhocRemoteString = 'echo ${option.val}'

        when:
            List<String> captured = null
            execService.executeCommand(_, _, node) >> { args ->
                captured = ((ExecArgList) args[1]).buildCommandForNode([:], 'windows')
                Mock(NodeExecutorResult) { isSuccess() >> true }
            }
            plugin.executeNodeStep(context, [:], node)

        then:
            captured != null
            // WINDOWS_CMD_ESCAPE prefixes each cmd-special character with '^'.
            // The '&' in 'foo & whoami' must become '^&', giving 'foo ^& whoami'.
            // The result must NOT be single-quote wrapped (that would indicate
            // WINDOWS_ARGUMENT_QUOTE was used instead of WINDOWS_CMD_ESCAPE).
            !captured[1].startsWith("'")
            captured[1] == CLIUtils.escapeWindowsCMDChars('foo & whoami')
    }
}
