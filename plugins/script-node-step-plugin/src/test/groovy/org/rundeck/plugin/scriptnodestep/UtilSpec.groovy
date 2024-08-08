package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.PropertyRetriever
import com.dtolabs.rundeck.core.data.MultiDataContext
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.DefaultScriptFileNodeStepUtils
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification

class UtilSpec extends Specification {

    def "handleFailureResult with success"() {

        given:
        NodeExecutorResult nodeExecutorResult = Mock(NodeExecutorResult) {
            isSuccess() >> true
        }
        INodeEntry entry = new NodeEntryImpl()

        when:

        Util.handleFailureResult(nodeExecutorResult, entry)

        then:
        noExceptionThrown()
    }

    def "handleFailureResult with failure message"() {

        given:
        NodeExecutorResult nodeExecutorResult = Mock(NodeExecutorResult) {
            isSuccess() >> false
            failureMessage >> "Test Failure Message"
        }
        INodeEntry entry = new NodeEntryImpl()

        when:
        Util.handleFailureResult(nodeExecutorResult, entry)

        then:
        def ex = thrown(NodeStepException)
        ex.message == "Test Failure Message"
    }

    def "handleFailreResult with null message"() {
        given:
        NodeExecutorResult nodeExecutorResult = Mock(NodeExecutorResult) {
            isSuccess() >> false
            failureMessage >> null
            failureReason >> Mock(FailureReason) {
                toString() >> "Test Failure Reason"
            }
        }
        INodeEntry entry = new NodeEntryImpl()

        when:
        Util.handleFailureResult(nodeExecutorResult, entry)

        then:
        def ex = thrown(NodeStepException)
        ex.message == "Step failed: Test Failure Reason"
    }

    def "CommandNodeStepPlugin handles null error message"() {

        given:
        CommandNodeStepPlugin plugin = new CommandNodeStepPlugin()
        plugin.adhocRemoteString = "echo hello"
        def context = Mock(PluginStepContext) {
            getExecutionContext() >> Mock(ExecutionContext) {
                getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    getPropertyRetriever() >> Mock(PropertyRetriever) {
                        getProperty(_) >> null
                    }
                }
            }

            getFramework() >> Mock(Framework) {
                getExecutionService() >> Mock(ExecutionService) {
                    executeCommand(_ as ExecutionContext, _ as ExecArgList, _ as INodeEntry) >> Mock(NodeExecutorResult) {
                        isSuccess() >> false
                        failureMessage >> null
                        failureReason >> Mock(FailureReason) {
                            toString() >> "Test Failure Reason"
                        }
                    }
                }
            }

        }

        Map<String, Object> config = [:]
        def nodeEntry = new NodeEntryImpl()

        when:

        plugin.executeNodeStep(context, config, nodeEntry)

        then:
        def ex = thrown(NodeStepException)
        ex.message == "Step failed: Test Failure Reason"

    }


    def "ScriptFileNodeStepExecutor handles null error message correctly"() {

        given:
        ScriptFileNodeStepExecutor plugin = new ScriptFileNodeStepExecutor(
                null,
                null,
                null,
                null,
                null,
                null,
                false)
        plugin.scriptUtils = Mock(DefaultScriptFileNodeStepUtils) {
            executeScriptFile(*_) >> Mock(NodeExecutorResult) {
                isSuccess() >> false
                failureMessage >> null
                failureReason >> Mock(FailureReason) {
                    toString() >> "Test Failure Reason"
                }
            }
        }


        def context = Mock(PluginStepContext) {
            getDataContext() >> Mock(Map)
            getExecutionContext() >> Mock(StepExecutionContext) {
                getSharedDataContext() >> Mock(MultiDataContext)
            }

            getFramework() >> Mock(Framework) {
                hasProperty(_) >> false

                getExecutionService() >> Mock(ExecutionService) {
                }
            }

        }

        Map<String, Object> config = [:]
        def nodeEntry = new NodeEntryImpl()

        when:

        plugin.executeScriptFile(context, nodeEntry, null)

        then:
        def ex = thrown(NodeStepException)
        ex.message == "Step failed: Test Failure Reason"

    }


}
