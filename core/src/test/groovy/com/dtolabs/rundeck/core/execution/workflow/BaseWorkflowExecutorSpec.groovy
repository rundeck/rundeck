/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution.workflow

import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl
import spock.lang.Specification
import spock.lang.Unroll

class BaseWorkflowExecutorSpec extends Specification {
    static class TestHaltWorkflowExecutor extends BaseWorkflowExecutor {
        StepExecutionResult originStepResult
        boolean haltSuccess
        String haltString
        boolean doHalt
        boolean doContinue

        TestHaltWorkflowExecutor(
            final IFramework framework,
            final StepExecutionResult originStepResult,
            final boolean haltSuccess,
            final String haltString,
            final boolean doHalt,
            final boolean doContinue
        ) {
            super(framework)
            this.originStepResult = originStepResult
            this.haltSuccess = haltSuccess
            this.haltString = haltString
            this.doHalt = doHalt
            this.doContinue = doContinue
        }

        @Override
        WorkflowExecutionResult executeWorkflowImpl(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item
        ) {
            return null
        }

        protected StepExecutionResult executeWFItem(
            final StepExecutionContext executionContext,
            final Map<Integer, StepExecutionResult> failedMap,
            final int c,
            final StepExecutionItem cmd
        ) {
            if (doHalt) {
                if (haltString) {
                    executionContext.flowControl.Halt(haltString)
                } else {
                    executionContext.flowControl.Halt(haltSuccess)
                }
            } else if (doContinue) {
                executionContext.flowControl.Continue()
            }
            originStepResult
        }
    }

    static enum TestFailureReason implements FailureReason {
        TestFailure
    }

    @Unroll
    def "execute workflow step flow control halt status is preserved"() {
        given:
            def testFramework = Mock(IFramework)
            def originStepResult = srcSuccess ? new StepExecutionResultImpl() :
                                   new StepExecutionResultImpl(null, TestFailureReason.TestFailure, 'a message')
            BaseWorkflowExecutor instance = new TestHaltWorkflowExecutor(
                testFramework,
                originStepResult,
                haltSuccess,
                haltString,
                doHalt,
                doContinue
            )
            def listener = Mock(WorkflowExecutionListener)
            def cmd = Mock(StepExecutionItem)
            def context = Mock(StepExecutionContext) {
                getExecutionListener() >> Mock(ExecutionListener)
            }
        when:
            def result = instance.executeWorkflowStep(context, [:], [], false, listener, 1, cmd)

        then:
            result.success == expectSuccess
            result.statusString == expectString
            result.controlBehavior == (doHalt ? ControlBehavior.Halt : doContinue ? ControlBehavior.Continue : null)
            result.stepResult.failureReason?.toString() == expectReason

        where:
            doHalt | doContinue | haltSuccess | haltString   | srcSuccess | expectSuccess | expectString | expectReason
            //no control
            false  | false      | false       | null         | true       | true          | null         | null
            false  | false      | false       | null         | false      | false         | null         | 'TestFailure'
            //halt success=true
            true   | false      | true        | null         | false      | true          | null         | null
            true   | false      | true        | null         | true       | true          | null         | null
            //halt status string
            true   | false      | false       | 'testStatus' | false      | false         | 'testStatus' | 'FlowControlHalted'
            true   | false      | false       | 'testStatus' | true       | false         | 'testStatus' | 'FlowControlHalted'
            //halt success=false
            true   | false      | false       | null         | false      | false         | null         | 'FlowControlHalted'
            true   | false      | false       | null         | true       | false         | null         | 'FlowControlHalted'
            //continue
            false  | true       | false       | null         | true       | true          | null         | null
            false  | true       | false       | null         | false      | false         | null         | 'TestFailure'
    }
}
