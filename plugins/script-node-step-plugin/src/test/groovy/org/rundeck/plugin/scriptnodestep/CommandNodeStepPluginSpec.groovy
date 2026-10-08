package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.PropertyRetriever
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * CommandNodeStepPlugin only decides which arguments need quoting (whether they contain a
 * property/data reference) and builds an ExecArgList accordingly; actual substitution and
 * per-reference quoting happen later, in ExecArgList.buildCommandForNode() (see
 * ExecCommandInjectionTest for that behavior, including the regression fix for
 * https://github.com/rundeck/rundeck/issues/10293 and
 * https://github.com/rundeck/rundeck/issues/10027).
 */
class CommandNodeStepPluginSpec extends Specification {

    PluginStepContext mockContext(Map<String, String> frameworkProps, ExecutionService executionService) {
        def propertyRetriever = Mock(PropertyRetriever) {
            getProperty(_ as String) >> { String key -> frameworkProps[key] }
        }
        def framework = Mock(IFramework) {
            getPropertyRetriever() >> propertyRetriever
        }
        def executionContext = Mock(ExecutionContext) {
            getIFramework() >> framework
        }
        // Deprecated PluginStepContext#getFramework() returns the concrete Framework class
        // (not IFramework); only getExecutionService() is needed on it for these tests.
        def concreteFramework = Mock(Framework) {
            getExecutionService() >> executionService
        }
        Mock(PluginStepContext) {
            getExecutionContext() >> executionContext
            getFramework() >> concreteFramework
        }
    }

    @Unroll
    def "flags only arguments containing a property reference for quoting"() {
        given:
        ExecArgList captured = null
        def executionService = Mock(ExecutionService) {
            executeCommand(_, _, _) >> { args ->
                captured = args[1]
                Mock(NodeExecutorResult) { isSuccess() >> true }
            }
        }
        def plugin = new CommandNodeStepPlugin(adhocRemoteString: adhocRemoteString)
        def context = mockContext([:], executionService)
        def node = Mock(INodeEntry)

        when:
        plugin.executeNodeStep(context, [:], node)

        then:
        def args = captured.getList()
        args.collect { [it.getString(), it.isQuoted()] } == expected

        where:
        adhocRemoteString                                        | expected
        'echo ${option.port}'                                    | [['echo', false], ['${option.port}', true]]
        // #10293: the option reference is embedded inside a single, larger job-authored argument
        // (grouped by the author's own quotes); the whole argument is flagged quoted, but it is up
        // to ExecArgList.buildCommandForNode() to quote only the reference's value in place.
        'sudo "sh script.sh ${option.parm1}"'                    | [['sudo', false], ['sh script.sh ${option.parm1}', true]]
        'echo literal text'                                      | [['echo', false], ['literal', false], ['text', false]]
    }

    def "quoting can be disabled via the feature flag"() {
        given:
        ExecArgList captured = null
        def executionService = Mock(ExecutionService) {
            executeCommand(_, _, _) >> { args ->
                captured = args[1]
                Mock(NodeExecutorResult) { isSuccess() >> true }
            }
        }
        def plugin = new CommandNodeStepPlugin(adhocRemoteString: 'echo ${option.port}')
        def context = mockContext(['rundeck.feature.exec.quoting.enabled': 'false'], executionService)
        def node = Mock(INodeEntry)

        when:
        plugin.executeNodeStep(context, [:], node)

        then:
        def args = captured.getList()
        args.collect { it.isQuoted() } == [false, false]
    }
}
