package com.dtolabs.rundeck.plugin.script

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import spock.lang.IgnoreIf
import spock.lang.Specification

/**
 * Tests for {@link ScriptNodeExecutor}, covering configuration of the command via the
 * node "script-exec" attribute when no project/framework default command is set (issue #9910).
 */
class ScriptNodeExecutorSpec extends Specification {

    def "description validates with no command configured"() {
        when:
            def report = Validator.validate(new Properties(), ScriptNodeExecutor.DESC)
            def resolverReport = Validator.validate(
                PropertyResolverFactory.createInstanceResolver(new Properties()),
                ScriptNodeExecutor.DESC,
                PropertyScope.Instance
            )
        then:
            report.valid
            resolverReport.valid
    }

    @IgnoreIf({ os.windows })
    def "node script-exec attribute is used as the command when no project default is set"() {
        given:
            def listener = Mock(ExecutionListener)
            def context = mockContext(listener)
            def node = new NodeEntryImpl('node1')
            node.setAttribute(ScriptNodeExecutor.SCRIPT_ATTRIBUTE, 'echo wrapped ${exec.command}')
            def executor = new ScriptNodeExecutor()
        when:
            def result = executor.executeCommand(context, ['hello'] as String[], node)
        then:
            result.success
            1 * listener.log(3, '[script-exec] executing: echo wrapped hello')
    }

    def "fails with a configuration error when neither project default nor node attribute is set"() {
        given:
            def context = mockContext(Mock(ExecutionListener))
            def node = new NodeEntryImpl('node1')
            def executor = new ScriptNodeExecutor()
        when:
            def result = executor.executeCommand(context, ['hello'] as String[], node)
        then:
            !result.success
            result.failureReason == StepFailureReason.ConfigurationFailure
            result.failureMessage.contains(ScriptNodeExecutor.SCRIPT_ATTRIBUTE)
    }

    /**
     * Create an execution context mock whose framework has no project properties set.
     */
    private ExecutionContext mockContext(ExecutionListener listener) {
        def framework = Mock(Framework) {
            getProjectProperty('proj', _) >> null
        }
        return Mock(ExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> 'proj'
            getDataContext() >> [:]
            getExecutionListener() >> listener
        }
    }
}
