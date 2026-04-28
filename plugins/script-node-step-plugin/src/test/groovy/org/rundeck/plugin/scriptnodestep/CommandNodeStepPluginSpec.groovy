package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
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
            def iFrameworkMock = Mock(IFramework) {
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
            }

            WFSharedContext sharedContext = SharedDataContextUtils.sharedContext()
            sharedContext.merge(ContextView.global(), DataContextUtils.context("data", [home_dir: '/workspace']))
            sharedContext.merge(ContextView.global(), DataContextUtils.context("job", [execid: 'exec123']))

            def execCtx = Mock(ExecutionContext) {
                getIFramework() >> iFrameworkMock
                getSharedDataContext() >> sharedContext
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
            def iFrameworkMock = Mock(IFramework) {
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
            }

            WFSharedContext sharedContext = SharedDataContextUtils.sharedContext()
            sharedContext.merge(ContextView.global(), DataContextUtils.context("option", [port: '80; whoami']))

            def execCtx = Mock(ExecutionContext) {
                getIFramework() >> iFrameworkMock
                getSharedDataContext() >> sharedContext
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

    def "per-value quoting does not quote unquoted refs"() {
        given:
            def fwkProps = [
                    'rundeck.feature.quoting.backwardCompatible': 'false',
                    'rundeck.feature.exec.quoting.enabled'      : 'true',
            ]
            def iFrameworkMock = Mock(IFramework) {
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
            }

            WFSharedContext sharedContext = SharedDataContextUtils.sharedContext()
            sharedContext.merge(ContextView.global(), DataContextUtils.context("option", [port: '80 && whoami']))

            def execCtx = Mock(ExecutionContext) {
                getIFramework() >> iFrameworkMock
                getSharedDataContext() >> sharedContext
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
}
