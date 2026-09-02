package com.dtolabs.rundeck.core.execution.workflow

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.StatusResult
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import spock.lang.Specification

/**
 * Note on timing: {@link StepNodeSecondsStore} only exposes a whole-seconds total, so these
 * tests use sleeps long enough (~1.2s) that "counted once" (rounds to 1s), "not counted"
 * (rounds to 0s), and "double-counted" (rounds to 2s+) are unambiguous outcomes, rather than
 * asserting exact millisecond durations.
 */
class StepNodeSecondsWorkflowListenerSpec extends Specification {

    def executor = Mock(StepExecutor)
    def stepContext = Mock(StepExecutionContext)
    def execContext = Mock(ExecutionContext)
    def statusResult = Mock(StatusResult)
    def nodeStepResult = Mock(NodeStepResult)
    def workflowResult = Mock(WorkflowExecutionResult)

    def node(String name) {
        Mock(INodeEntry) {
            getNodename() >> name
        }
    }

    def "zero-node step contributes exactly one step-level unit"() {
        given:
        def executionId = 111L
        def listener = new StepNodeSecondsWorkflowListener(executionId)
        def item = Mock(StepExecutionItem)

        when:
        listener.beginStepExecution(executor, stepContext, item)
        Thread.sleep(1200)
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        StepNodeSecondsStore.getInstance().takeFinishedTotal(executionId) == 1L
    }

    def "single-node step contributes exactly one node-level unit, not double-counted at the step level"() {
        given:
        def executionId = 112L
        def listener = new StepNodeSecondsWorkflowListener(executionId)
        def item = Mock(NodeStepExecutionItem)
        def n = node("node1")

        when:
        listener.beginStepExecution(executor, stepContext, item)
        listener.beginExecuteNodeStep(execContext, item, n)
        Thread.sleep(1200)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, item, n)
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        // if the step-level path had also added its own ~1.2s span on top of the
        // node-level span, this would round to 2s -- assert it stays at 1s.
        StepNodeSecondsStore.getInstance().takeFinishedTotal(executionId) == 1L
    }

    def "multi-node dispatch of one step sums per-node durations, no step-level addition"() {
        given:
        def executionId = 113L
        def listener = new StepNodeSecondsWorkflowListener(executionId)
        def item = Mock(NodeStepExecutionItem)
        def nodeA = node("nodeA")
        def nodeB = node("nodeB")

        when:
        listener.beginStepExecution(executor, stepContext, item)
        listener.beginExecuteNodeStep(execContext, item, nodeA)
        listener.beginExecuteNodeStep(execContext, item, nodeB)
        Thread.sleep(1200)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, item, nodeA)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, item, nodeB)
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        // two node-level spans of ~1.2s each sum to ~2.4s -> rounds to 2s; would be ~0s if
        // not counted, or ~3s+ if the step level also added its own span on top.
        StepNodeSecondsStore.getInstance().takeFinishedTotal(executionId) == 2L
    }

    def "concurrent dispatch across nodes of the same step is thread-safe and sums correctly"() {
        given:
        def executionId = 114L
        def listener = new StepNodeSecondsWorkflowListener(executionId)
        def item = Mock(NodeStepExecutionItem)
        def nodeCount = 5
        def sleepMillis = 1000

        when:
        listener.beginStepExecution(executor, stepContext, item)
        def threads = (1..nodeCount).collect { i ->
            Thread.start {
                def n = node("node${i}")
                listener.beginExecuteNodeStep(execContext, item, n)
                Thread.sleep(sleepMillis)
                listener.finishExecuteNodeStep(nodeStepResult, execContext, item, n)
            }
        }
        threads*.join()
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        // 5 concurrent ~1s node spans should sum to ~5s if no updates were lost to a race.
        def total = StepNodeSecondsStore.getInstance().takeFinishedTotal(executionId)
        total >= 4L
        total <= 6L
    }

    def "nested job-reference execution accumulates into the same top-level total, with last-write-wins on finishWorkflowExecution"() {
        given:
        def executionId = 115L
        def listener = new StepNodeSecondsWorkflowListener(executionId)
        def outerItem = Mock(NodeStepExecutionItem)
        def innerItem = Mock(NodeStepExecutionItem)
        def sharedNodeName = "shared-node"
        def outerNode = node(sharedNodeName)
        def innerNode = node(sharedNodeName)

        when: "outer step begins dispatch to a node"
        listener.beginStepExecution(executor, stepContext, outerItem)
        listener.beginExecuteNodeStep(execContext, outerItem, outerNode)

        and: "while that outer node dispatch is open, a nested job-reference runs its own workflow on the same listener instance, using the same node name but a distinct item"
        listener.beginStepExecution(executor, stepContext, innerItem)
        listener.beginExecuteNodeStep(execContext, innerItem, innerNode)
        Thread.sleep(1200)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, innerItem, innerNode)
        listener.finishStepExecution(executor, statusResult, stepContext, innerItem)
        listener.finishWorkflowExecution(workflowResult, stepContext, null) // nested job's own finish -- writes an intermediate ~1s total

        and: "the outer node dispatch then finishes, after more time has passed"
        Thread.sleep(1200)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, outerItem, outerNode)
        listener.finishStepExecution(executor, statusResult, stepContext, outerItem)
        listener.finishWorkflowExecution(workflowResult, stepContext, null) // outer's own finish -- overwrites with the final total

        then: "the final value reflects both levels' durations (inner ~1.2s + outer ~2.4s), not just the nested one"
        def total = StepNodeSecondsStore.getInstance().takeFinishedTotal(executionId)
        total >= 3L
    }

    def "finishWorkflowExecution writes under the closure-captured execution id, and the store is read-and-remove"() {
        given:
        def executionId = 116L
        def listener = new StepNodeSecondsWorkflowListener(executionId)
        def item = Mock(StepExecutionItem)

        when:
        listener.beginStepExecution(executor, stepContext, item)
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        StepNodeSecondsStore.getInstance().takeFinishedTotal(executionId) != null
        StepNodeSecondsStore.getInstance().takeFinishedTotal(executionId) == null
    }

    def "unmatched finish calls do not throw and do not corrupt the total"() {
        given:
        def executionId = 117L
        def listener = new StepNodeSecondsWorkflowListener(executionId)
        def item = Mock(StepExecutionItem)
        def nodeItem = Mock(NodeStepExecutionItem)
        def n = node("orphan-node")

        when:
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, nodeItem, n)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        noExceptionThrown()
        StepNodeSecondsStore.getInstance().takeFinishedTotal(executionId) == 0L
    }
}
