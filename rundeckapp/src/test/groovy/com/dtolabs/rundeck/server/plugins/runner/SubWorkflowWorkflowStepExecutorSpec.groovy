/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.server.plugins.runner

import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.jobs.SubWorkflowExecutionItem
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for SubWorkflowWorkflowStepExecutor
 */
class SubWorkflowWorkflowStepExecutorSpec extends Specification {

    // Test enum for failure reasons
    enum TestFailureReason implements FailureReason {
        TEST_FAILURE
    }

    @Unroll
    def "isNodeDispatchStep returns false for #itemType"() {
        given: "a SubWorkflowWorkflowStepExecutor instance"
            def executor = new SubWorkflowWorkflowStepExecutor()

        when: "isNodeDispatchStep is called with the item"
            def result = executor.isNodeDispatchStep(item)

        then: "it returns false"
            !result

        where: "different item types are tested"
            itemType                    | item
            "StepExecutionItem"         | Mock(StepExecutionItem)
            "SubWorkflowExecutionItem"  | Mock(SubWorkflowExecutionItem)
    }

    def "executeWorkflowStep delegates with SubWorkflowExecutionItem"() {
        given: "a SubWorkflowWorkflowStepExecutor with mocked executionService"
            def mockExecutionService = Mock(StepExecutor)
            def executor = new SubWorkflowWorkflowStepExecutor(executionService: mockExecutionService)

        and: "a step execution context and SubWorkflowExecutionItem"
            def context = Mock(StepExecutionContext)
            def item = Mock(SubWorkflowExecutionItem)
            def expectedResult = Mock(StepExecutionResult)

        when: "executeWorkflowStep is called"
            def result = executor.executeWorkflowStep(context, item)

        then: "it delegates to the executionService with the correct parameters"
            1 * mockExecutionService.executeWorkflowStep(context, item) >> expectedResult

        and: "returns the result from executionService"
            result == expectedResult
    }

    def "executeWorkflowStep throws StepException when executionService throws"() {
        given: "a SubWorkflowWorkflowStepExecutor with mocked executionService that throws"
            def mockExecutionService = Mock(StepExecutor)
            def executor = new SubWorkflowWorkflowStepExecutor(executionService: mockExecutionService)

        and: "a step execution context and item"
            def context = Mock(StepExecutionContext)
            def item = Mock(StepExecutionItem)
            def expectedException = new StepException("Test exception", TestFailureReason.TEST_FAILURE)

        and: "executionService is configured to throw an exception"
            mockExecutionService.executeWorkflowStep(context, item) >> { throw expectedException }

        when: "executeWorkflowStep is called"
            executor.executeWorkflowStep(context, item)

        then: "the StepException is propagated"
            def thrownException = thrown(StepException)
            thrownException == expectedException
    }

    def "executeWorkflowStep handles successful execution result"() {
        given: "a SubWorkflowWorkflowStepExecutor with mocked executionService"
            def mockExecutionService = Mock(StepExecutor)
            def executor = new SubWorkflowWorkflowStepExecutor(executionService: mockExecutionService)

        and: "a step execution context and item"
            def context = Mock(StepExecutionContext)
            def item = Mock(StepExecutionItem)
            def successResult = Mock(StepExecutionResult) {
                isSuccess() >> true
            }

        and: "executionService returns a successful result"
            mockExecutionService.executeWorkflowStep(context, item) >> successResult

        when: "executeWorkflowStep is called"
            def result = executor.executeWorkflowStep(context, item)

        then: "it returns the successful result"
            result == successResult
            result.isSuccess()
    }

    def "executeWorkflowStep handles failed execution result"() {
        given: "a SubWorkflowWorkflowStepExecutor with mocked executionService"
            def mockExecutionService = Mock(StepExecutor)
            def executor = new SubWorkflowWorkflowStepExecutor(executionService: mockExecutionService)

        and: "a step execution context and item"
            def context = Mock(StepExecutionContext)
            def item = Mock(StepExecutionItem)
            def failureResult = Mock(StepExecutionResult) {
                isSuccess() >> false
            }

        and: "executionService returns a failed result"
            mockExecutionService.executeWorkflowStep(context, item) >> failureResult

        when: "executeWorkflowStep is called"
            def result = executor.executeWorkflowStep(context, item)

        then: "it returns the failed result"
            result == failureResult
            !result.isSuccess()
    }

}

