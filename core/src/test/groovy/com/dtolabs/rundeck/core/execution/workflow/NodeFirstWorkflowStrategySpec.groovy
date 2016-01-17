package com.dtolabs.rundeck.core.execution.workflow

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification

/**
 * Created by greg on 11/6/15.
 */
class NodeFirstWorkflowStrategySpec extends Specification {
    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject('NodeFirstWorkflowStrategySpec')
    }

    def teardown() {
        framework.getFrameworkProjectMgr().removeFrameworkProject('NodeFirstWorkflowStrategySpec')
    }

    def "node step fails then workflow step success should fail"() {
        given:
        def strategy = new NodeFirstWorkflowStrategy(framework)
        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)
        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Mock(ExecutionListener) {
                log(*_) >> { args ->
                    System.err.println(args[1])
                }
            }
            getNodes() >> nodeSet
        }
        def successExecutor = Mock(StepExecutor) {
            isNodeDispatchStep(_) >> false
            executeWorkflowStep(_, _) >> {
                System.err.println("success StepExecutor")
                new StepExecutionResultImpl()
            }
        }
        def failExecutor = Mock(StepExecutor) {
            isNodeDispatchStep(_) >> true
            executeWorkflowStep(_, _) >> {
                System.err.println("failure StepExecutor")
                NodeDispatchStepExecutor.wrapDispatcherException(new DispatcherException(
                        "bad node",
                        new NodeStepException("bad", NodeStepFailureReason.NonZeroResultCode, "node1"),
                        node1
                )
                )
            }
        }
        framework.getStepExecutionService().registerInstance('typeA', failExecutor)
        framework.getStepExecutionService().registerInstance('typeB', successExecutor)

        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                isKeepgoing() >> true
                getCommands() >> [
                        Mock(StepExecutionItem) {
                            getType() >> 'typeA'
                        },
                        Mock(StepExecutionItem) {
                            getType() >> 'typeB'
                        }
                ]
            }
        }


        when:
        def result = strategy.executeWorkflowImpl(context, item)


        then:
        !result.isSuccess()
        result.getNodeFailures()
        result.getNodeFailures()['node1']
        NodeStepFailureReason.NonZeroResultCode == result.nodeFailures['node1'][0].failureReason

    }
}
