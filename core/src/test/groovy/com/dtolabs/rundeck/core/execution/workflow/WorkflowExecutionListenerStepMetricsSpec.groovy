package com.dtolabs.rundeck.core.execution.workflow

import com.dtolabs.rundeck.core.execution.StatusResult
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import spock.lang.Specification

class WorkflowExecutionListenerStepMetricsSpec extends Specification {

    def workflowMetricsWriter = Mock(WorkflowMetricsWriter)
    def listener = new WorkflowExecutionListenerStepMetrics(workflowMetricsWriter)

    def "test finishStepExecution success case"() {
        given:
        def executor = Mock(StepExecutor)
        def result = Mock(StatusResult) {
            isSuccess() >> true
        }
        def context = Mock(StepExecutionContext)
        def item = Mock(StepExecutionItem)

        when:
        listener.finishStepExecution(executor, result, context, item)

        then:
        1 * workflowMetricsWriter.markMeterStepMetric(_, "finishWorkflowStepSucceededMeter")
    }

    def "test finishStepExecution failure case with NodeStepExecutionItem"() {
        given:
        def executor = Mock(StepExecutor)
        def result = Mock(StatusResult) {
            isSuccess() >> false
        }
        def context = Mock(StepExecutionContext) {
            getStepNumber() >> 1
        }
        def item = Mock(NodeStepExecutionItem) {
            getNodeStepType() >> "testNodeStep"
        }

        when:
        listener.finishStepExecution(executor, result, context, item)

        then:
        1 * workflowMetricsWriter.markMeterStepMetric(_, "finishWorkflowStepFailedMeter")
    }

    def "test finishStepExecution failure case with StepExecutionItem"() {
        given:
        def executor = Mock(StepExecutor)
        def result = Mock(StatusResult) {
            isSuccess() >> false
        }
        def context = Mock(StepExecutionContext) {
            getStepNumber() >> 2
        }
        def item = Mock(StepExecutionItem) {
            getType() >> "testStep"
        }

        when:
        listener.finishStepExecution(executor, result, context, item)

        then:
        1 * workflowMetricsWriter.markMeterStepMetric(_, "finishWorkflowStepFailedMeter")
    }

    def "test finishStepExecution with null result"() {
        given:
        def executor = Mock(StepExecutor)
        def context = Mock(StepExecutionContext) {
            getStepNumber() >> 3
        }
        def item = Mock(StepExecutionItem) {
            getType() >> "testStep"
        }

        when:
        listener.finishStepExecution(executor, null, context, item)

        then:
        1 * workflowMetricsWriter.markMeterStepMetric(_, "finishWorkflowStepFailedMeter")
    }
}
