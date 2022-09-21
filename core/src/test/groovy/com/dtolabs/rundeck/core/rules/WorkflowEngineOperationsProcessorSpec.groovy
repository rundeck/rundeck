/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.rules

import com.google.common.util.concurrent.ListeningExecutorService
import spock.lang.Specification

class WorkflowEngineOperationsProcessorSpec extends Specification {

    class TestOpCompleted implements WorkflowSystem.OperationCompleted<Map> {
        StateObj newState
        Map result
        String identity
        boolean success
    }

    class TestOperation implements WorkflowSystem.Operation<Map, TestOpCompleted> {
        Closure<TestOpCompleted> toCall
        Closure<Boolean> shouldRunClos
        private boolean shouldRun
        Closure<Boolean> shouldSkipClos
        private boolean shouldSkip
        StateObj failureState
        StateObj skipState
        Long id
        boolean hasRun = false
        Map input = null
        String identity;

        @Override
        boolean shouldRun(final StateObj state) {
            return shouldRunClos?.call(state) ?: shouldRun
        }

        @Override
        StateObj getFailureState(final Throwable t) {
            return failureState
        }

        @Override
        boolean shouldSkip(final StateObj state) {
            return shouldSkipClos?.call(state) ?: shouldSkip
        }

        @Override
        StateObj getSkipState(final StateObj state) {
            skipState
        }


        @Override
        TestOpCompleted apply(final Map o) throws Exception {
            hasRun = true
            input = o
            def result = toCall?.call()
            return result
        }
    }

    def "detect no more changes"() {
        given:
            Set<TestOperation> operations = new HashSet<TestOperation>()
            def engine = Mock(StateWorkflowSystem)
            Map shared = [:]
            WorkflowSystem.SharedData<Map,Map> sharedData = WorkflowSystem.SharedData.<Map,Map> with(
                    { Map d -> shared.putAll(d) },
                    { -> shared },
                    {->[:]}
            )
            def executor = Mock(ListeningExecutorService)
            def manager = Mock(ListeningExecutorService)
            def handler = Mock(WorkflowSystemEventHandler)
            def testOp = new TestOperation()

            WorkflowEngineOperationsProcessor processor = new WorkflowEngineOperationsProcessor<Map, TestOpCompleted,
                    TestOperation>(
                    engine,
                    handler,
                    operations,
                    sharedData,
                    executor,
                    manager
            )
        expect:
            processor.detectNoMoreChanges()
        when:
            processor.initialize()
        then:
            !processor.detectNoMoreChanges()
        when:
            processor.stateChangeQueue.clear()
        then:
            processor.detectNoMoreChanges()
        when:
            processor.inProcess.add(testOp)
        then:
            !processor.detectNoMoreChanges()
    }

    def "shouldWorkflowEnd()"() {
        given:
            Set<TestOperation> operations = new HashSet<TestOperation>()
            def engine = Mock(StateWorkflowSystem)
            Map shared = [:]
            WorkflowSystem.SharedData<Map,Map> sharedData = WorkflowSystem.SharedData.<Map,Map> with(
                    { Map d -> shared.putAll(d) },
                    { -> shared },
                    {->[:]}
            )
            def executor = Mock(ListeningExecutorService)
            def manager = Mock(ListeningExecutorService)
            def handler = Mock(WorkflowSystemEventHandler)

            WorkflowEngineOperationsProcessor processor = new WorkflowEngineOperationsProcessor<Map, TestOpCompleted,
                    TestOperation>(
                    engine,
                    handler,
                    operations,
                    sharedData,
                    executor,
                    manager
            )
            processor.tuneEndStateGather(gather)
            if(ops){
                processor.inProcess.add(new TestOperation())
            }
            if(changes){
                processor.stateChangeQueue.add(Mock(WorkflowSystem.OperationCompleted))
            }
            engine.isWorkflowEndState()>>endState

        expect:
            processor.shouldWorkflowEnd() == expect
        where:
            ops   | changes | endState | gather | expect
            false | false   | false    | false  | false
            false | false   | true     | false  | true
            false | true    | true     | false  | true
            true  | false   | true     | false  | true
            true  | true    | true     | false  | true
            false | false   | true     | true   | true
            true  | false   | true     | true   | false
            true  | true    | true     | true   | false
            false | true    | true     | true   | false
    }
}
