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
 * Note on timing: {@link StepNodeUsageStore} only exposes whole-seconds durations, so these
 * tests use sleeps long enough (~1.2s) that "counted once" (rounds to 1s), "not counted"
 * (rounds to 0s), and "double-counted" (rounds to 2s+) are unambiguous outcomes, rather than
 * asserting exact millisecond durations.
 * <p>
 * Most tests below share one unstubbed {@code stepContext} mock across every step, so every
 * step resolves to the same path and their durations collapse into a single breakdown entry
 * -- fine for these tests, which only assert the total (summed across the breakdown's
 * entries). The per-step breakdown itself -- distinct steps producing distinct, correctly
 * keyed entries, each with its own plugin type -- is exercised separately, below.
 */
class StepNodeUsageWorkflowListenerSpec extends Specification {

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

    def totalOf(Long executionId) {
        StepNodeUsageStore.getInstance().takeFinishedBreakdown(executionId)?.values()?.sum { it.seconds }
    }

    def "zero-node step contributes exactly one step-level unit"() {
        given:
        def executionId = 111L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def item = Mock(StepExecutionItem) {
            getType() >> "some-workflow-step"
        }

        when:
        listener.beginStepExecution(executor, stepContext, item)
        Thread.sleep(1200)
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        totalOf(executionId) == 1L
    }

    def "single-node step contributes exactly one node-level unit, not double-counted at the step level"() {
        given:
        def executionId = 112L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def item = Mock(NodeStepExecutionItem) {
            getNodeStepType() >> "exec-command"
        }
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
        totalOf(executionId) == 1L
    }

    def "multi-node dispatch of one step sums per-node durations, no step-level addition"() {
        given:
        def executionId = 113L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def item = Mock(NodeStepExecutionItem) {
            getNodeStepType() >> "exec-command"
        }
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
        // not counted, or ~3s+ if the step level also added its own span on top. Both nodes
        // dispatch from the same step, so they land in the same breakdown entry.
        totalOf(executionId) == 2L
        StepNodeUsageStore.getInstance().takeFinishedBreakdown(executionId) == null // already taken by totalOf() above
    }

    def "concurrent dispatch across nodes of the same step is thread-safe and sums correctly"() {
        given:
        def executionId = 114L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def item = Mock(NodeStepExecutionItem) {
            getNodeStepType() >> "exec-command"
        }
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
        def total = totalOf(executionId)
        total >= 4L
        total <= 6L
    }

    def "nested job-reference execution accumulates into the same top-level total, with last-write-wins on finishWorkflowExecution"() {
        given:
        def executionId = 115L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def outerItem = Mock(NodeStepExecutionItem) {
            getNodeStepType() >> "job-ref"
        }
        def innerItem = Mock(NodeStepExecutionItem) {
            getNodeStepType() >> "exec-command"
        }
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
        listener.finishWorkflowExecution(workflowResult, stepContext, null) // nested job's own finish -- writes an intermediate ~1s breakdown

        and: "the outer node dispatch then finishes, after more time has passed"
        Thread.sleep(1200)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, outerItem, outerNode)
        listener.finishStepExecution(executor, statusResult, stepContext, outerItem)
        listener.finishWorkflowExecution(workflowResult, stepContext, null) // outer's own finish -- overwrites with the final breakdown

        then: "the final value reflects both levels' durations (inner ~1.2s + outer ~2.4s), not just the nested one"
        totalOf(executionId) >= 3L
    }

    def "finishWorkflowExecution writes under the closure-captured execution id, and the store is read-and-remove"() {
        given:
        def executionId = 116L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def item = Mock(StepExecutionItem) {
            getType() >> "some-workflow-step"
        }

        when:
        listener.beginStepExecution(executor, stepContext, item)
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        StepNodeUsageStore.getInstance().takeFinishedBreakdown(executionId) != null
        StepNodeUsageStore.getInstance().takeFinishedBreakdown(executionId) == null
    }

    def "unmatched finish calls do not throw and do not corrupt the breakdown"() {
        given:
        def executionId = 117L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def item = Mock(StepExecutionItem)
        def nodeItem = Mock(NodeStepExecutionItem)
        def n = node("orphan-node")

        when:
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, nodeItem, n)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        noExceptionThrown()
        StepNodeUsageStore.getInstance().takeFinishedBreakdown(executionId) == [:]
    }

    def "two distinct steps produce two distinct breakdown entries, each with its own duration and plugin type"() {
        given:
        def executionId = 118L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def step1Context = Mock(StepExecutionContext) {
            getStepNumber() >> 1
            getStepContext() >> []
        }
        def step2Context = Mock(StepExecutionContext) {
            getStepNumber() >> 2
            getStepContext() >> []
        }
        def item1 = Mock(StepExecutionItem) {
            getType() >> "notification-plugin"
        }
        def item2 = Mock(NodeStepExecutionItem) {
            getNodeStepType() >> "exec-command"
        }

        when:
        listener.beginStepExecution(executor, step1Context, item1)
        Thread.sleep(1200)
        listener.finishStepExecution(executor, statusResult, step1Context, item1)

        listener.beginStepExecution(executor, step2Context, item2)
        Thread.sleep(2200)
        listener.finishStepExecution(executor, statusResult, step2Context, item2)

        listener.finishWorkflowExecution(workflowResult, step1Context, null)

        then:
        def breakdown = StepNodeUsageStore.getInstance().takeFinishedBreakdown(executionId)
        breakdown.keySet() == (["1", "2"] as Set)
        breakdown["1"].seconds == 1L
        breakdown["1"].pluginType == "notification-plugin"
        breakdown["1"].isNodeStep == false
        breakdown["1"].nodeCount == 1L
        breakdown["2"].seconds == 2L
        breakdown["2"].pluginType == "exec-command"
        breakdown["2"].isNodeStep == true
        breakdown["2"].nodeCount == 1L
    }

    def "a step dispatched to multiple nodes counts one node-step unit per node, not per step"() {
        given:
        def executionId = 121L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def item = Mock(NodeStepExecutionItem) {
            getNodeStepType() >> "exec-command"
        }
        def nodeA = node("nodeA")
        def nodeB = node("nodeB")
        def nodeC = node("nodeC")

        when:
        listener.beginStepExecution(executor, stepContext, item)
        listener.beginExecuteNodeStep(execContext, item, nodeA)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, item, nodeA)
        listener.beginExecuteNodeStep(execContext, item, nodeB)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, item, nodeB)
        listener.beginExecuteNodeStep(execContext, item, nodeC)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, item, nodeC)
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        def breakdown = StepNodeUsageStore.getInstance().takeFinishedBreakdown(executionId)
        breakdown.values().first().nodeCount == 3L
    }

    def "a step nested under another step produces a breakdown key reflecting the parent path"() {
        given:
        def executionId = 119L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def nestedContext = Mock(StepExecutionContext) {
            getStepNumber() >> 1
            getStepContext() >> [3]
        }
        def item = Mock(StepExecutionItem) {
            getType() >> "some-workflow-step"
        }

        when:
        listener.beginStepExecution(executor, nestedContext, item)
        Thread.sleep(1200)
        listener.finishStepExecution(executor, statusResult, nestedContext, item)
        listener.finishWorkflowExecution(workflowResult, nestedContext, null)

        then:
        def breakdown = StepNodeUsageStore.getInstance().takeFinishedBreakdown(executionId)
        breakdown.keySet() == (["3/1"] as Set)
        breakdown["3/1"].seconds == 1L
    }

    def "a node-dispatching step's breakdown entry uses the node-step type, not the generic step type"() {
        given:
        def executionId = 120L
        def listener = new StepNodeUsageWorkflowListener(executionId)
        def item = Mock(NodeStepExecutionItem) {
            getType() >> "generic-node-step-wrapper"
            getNodeStepType() >> "exec-command"
        }
        def n = node("node1")

        when:
        listener.beginStepExecution(executor, stepContext, item)
        listener.beginExecuteNodeStep(execContext, item, n)
        Thread.sleep(1200)
        listener.finishExecuteNodeStep(nodeStepResult, execContext, item, n)
        listener.finishStepExecution(executor, statusResult, stepContext, item)
        listener.finishWorkflowExecution(workflowResult, stepContext, null)

        then:
        def breakdown = StepNodeUsageStore.getInstance().takeFinishedBreakdown(executionId)
        breakdown.values().first().pluginType == "exec-command"
        breakdown.values().first().isNodeStep == true
    }
}
