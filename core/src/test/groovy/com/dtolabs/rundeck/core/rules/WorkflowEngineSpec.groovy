package com.dtolabs.rundeck.core.rules

import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by greg on 5/2/16.
 */
class WorkflowEngineSpec extends Specification {
    class TestOpCompleted implements WorkflowSystem.OperationCompleted<Map> {
        StateObj newState
        Map result
        String identity
        boolean success
    }

    class TestOpCompletedB implements WorkflowSystem.OperationCompleted<WFSharedContext> {
        StateObj newState
        WFSharedContext result
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
        String identity

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
            input=o
            def result = toCall?.call()
            return result
        }
    }


    class TestOperationB implements WorkflowSystem.Operation<WFSharedContext, TestOpCompletedB> {
        Closure<TestOpCompletedB> toCall
        Closure<Boolean> shouldRunClos
        private boolean shouldRun
        Closure<Boolean> shouldSkipClos
        private boolean shouldSkip
        StateObj failureState
        StateObj skipState
        Long id
        boolean hasRun = false
        WFSharedContext input = null
        String identity

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
        TestOpCompletedB apply(final WFSharedContext o) throws Exception {
            hasRun = true
            input=o
            def result = toCall?.call()
            return result
        }
    }

    def "no operations"() {
        given:
        RuleEngine ruleEngine = Rules.createEngine()
        MutableStateObj state = States.mutable()
        ExecutorService executor = Executors.newFixedThreadPool(1)
        WorkflowEngine engine = new WorkflowEngine(ruleEngine, state, executor)
        Set<TestOperation> operations = []
        when:
        def result = engine.processOperations(operations,null)

        then:
        result.size() == 0
    }

    def "one operation run on start"() {
        given:
        RuleEngine ruleEngine = Rules.createEngine()
        MutableStateObj state = States.mutable()
        ExecutorService executor = Executors.newFixedThreadPool(1)
        WorkflowEngine engine = new WorkflowEngine(ruleEngine, state, executor)
        Set<TestOperation> operations = [
                new TestOperation(shouldRunClos: {
                    StateObj st -> st.hasState(Workflows.WORKFLOW_STATE_KEY, Workflows.WORKFLOW_STATE_STARTED)
                },
                                  toCall: {
                                      return new TestOpCompleted(newState: States.state('akey', 'avalue'))
                                  }
                ),
                new TestOperation()
        ]
        when:
        def result = engine.processOperations(operations,null)

        then:
        result.size() == 1
        result[0].success.newState.state == [akey: 'avalue']
        state.hasState('akey', 'avalue')
        operations[0].hasRun
    }

    def "skip one operation run on start"() {
        given:
        RuleEngine ruleEngine = Rules.createEngine()
        MutableStateObj state = States.mutable()
        ExecutorService executor = Executors.newFixedThreadPool(1)
        WorkflowEngine engine = new WorkflowEngine(ruleEngine, state, executor)
        Set<TestOperation> operations = [
                new TestOperation(shouldRunClos: {
                    StateObj st -> st.hasState(Workflows.WORKFLOW_STATE_KEY, Workflows.WORKFLOW_STATE_STARTED)
                },
                                  toCall: {
                                      return new TestOpCompleted(newState: States.state('akey', 'avalue'))
                                  },
                                  shouldSkipClos: { StateObj st -> true },
                                  skipState: States.state('ckey', 'cvalue')
                ),
                new TestOperation()
        ]
        when:
        def result = engine.processOperations(operations,null)

        then:
        result.size() == 0
        !state.hasState('akey', 'avalue')
        state.hasState('ckey', 'cvalue')
        !operations[0].hasRun
    }
    def "one operation no skip or start"() {
        given:
        RuleEngine ruleEngine = Rules.createEngine()
        MutableStateObj state = States.mutable()
        ExecutorService executor = Executors.newFixedThreadPool(1)
        WorkflowEngine engine = new WorkflowEngine(ruleEngine, state, executor)
        Set<TestOperation> operations = [
                new TestOperation(shouldRunClos: {
                    StateObj st -> false
                },
                                  toCall: {
                                      return new TestOpCompleted(newState: States.state('akey', 'avalue'))
                                  },
                                  shouldSkipClos: { StateObj st -> false },
                                  skipState: States.state('ckey', 'cvalue')
                ),
                new TestOperation()
        ]
        when:
        def result = engine.processOperations(operations,null)

        then:
        result.size() == 0
        !state.hasState('akey', 'avalue')
        !state.hasState('ckey', 'cvalue')
        !operations[0].hasRun
    }

    def "two operation run sequentially from success"() {
        given:
        RuleEngine ruleEngine = Rules.createEngine()
        MutableStateObj state = States.mutable()
        ExecutorService executor = Executors.newFixedThreadPool(1)
        WorkflowEngine engine = new WorkflowEngine(ruleEngine, state, executor)
        Set<TestOperation> operations = [
                new TestOperation(
                        id: 1,
                        shouldRunClos: {
                            StateObj st
                                ->
                                st.hasState(Workflows.WORKFLOW_STATE_KEY, Workflows.WORKFLOW_STATE_STARTED)
                        },
                        toCall: {
                            return new TestOpCompleted(newState: States.state('akey', 'avalue'))
                        }
                ),
                new TestOperation(
                        id: 2,
                        shouldRunClos: {
                            StateObj st -> st.hasState('akey', 'avalue')
                        },
                        toCall: {
                            return new TestOpCompleted(newState: States.state('bkey', 'bvalue'))
                        }
                ),
        ]
        when:
        def result = engine.processOperations(operations,null)

        then:
        result.size() == 2
        result.find { it.operation.id == 1 }.success.newState.state == [akey: 'avalue']
        result.find { it.operation.id == 2 }.success.newState.state == [bkey: 'bvalue']
        state.hasState('akey', 'avalue')
        state.hasState('bkey', 'bvalue')
        operations[0].hasRun
        operations[1].hasRun
    }

    def "two operation run sequentially skip one"() {
        given:
        RuleEngine ruleEngine = Rules.createEngine()
        MutableStateObj state = States.mutable()
        ExecutorService executor = Executors.newFixedThreadPool(1)
        WorkflowEngine engine = new WorkflowEngine(ruleEngine, state, executor)
        Set<TestOperation> operations = [
                new TestOperation(
                        id: 1,
                        shouldRunClos: {
                            StateObj st
                                ->
                                st.hasState(Workflows.WORKFLOW_STATE_KEY, Workflows.WORKFLOW_STATE_STARTED)
                        },
                        toCall: {
                            return new TestOpCompleted(newState: States.state('akey', 'avalue'))
                        }
                ),
                new TestOperation(
                        id: 2,
                        shouldRunClos: {
                            StateObj st -> st.hasState('akey', 'avalue')
                        },
                        toCall: {
                            return new TestOpCompleted(newState: States.state('bkey', 'bvalue'))
                        },
                        shouldSkipClos: {
                            StateObj st -> st.hasState('akey', 'avalue')
                        },
                        skipState: States.state('ckey', 'cvalue')
                ),
        ]
        when:
        def result = engine.processOperations(operations,null)

        then:
        result.size() == 1
        result.find { it.operation.id == 1 }.success.newState.state == [akey: 'avalue']
        !result.find { it.operation.id == 2 }
        state.hasState('akey', 'avalue')
        !state.hasState('bkey', 'bvalue')
        state.hasState('ckey', 'cvalue')
        operations[0].hasRun
        !operations[1].hasRun
    }

    def "operation halts workflow"() {
        given:
        RuleEngine ruleEngine = Rules.createEngine()
        MutableStateObj state = States.mutable()
        ExecutorService executor = Executors.newFixedThreadPool(1)
        WorkflowEngine engine = new WorkflowEngine(ruleEngine, state, executor)
        Set<TestOperation> operations = [
                new TestOperation(
                        id: 1,
                        shouldRunClos: {
                            StateObj st
                                ->
                                st.hasState(Workflows.WORKFLOW_STATE_KEY, Workflows.WORKFLOW_STATE_STARTED)
                        },
                        toCall: {
                            return new TestOpCompleted(newState: new DataState(Workflows.WORKFLOW_DONE, 'true'))
                        }
                ),
                new TestOperation(
                        id: 2,
                        shouldRunClos: {
                            StateObj st -> st.hasState('akey', 'avalue')
                        },
                        toCall: {
                            return new TestOpCompleted(newState: States.state('bkey', 'bvalue'))
                        }
                ),
        ]
        when:
        def result = engine.processOperations(operations,null)

        then:
        result.size() == 1
        result.find { it.operation.id == 1 }.success.newState.state == [(Workflows.WORKFLOW_DONE): 'true']
        state.hasState(Workflows.WORKFLOW_DONE, 'true')
        operations[0].hasRun
        !operations[1].hasRun
    }

    def "one operation run sequentially fails"() {
        given:
        RuleEngine ruleEngine = Rules.createEngine()
        MutableStateObj state = States.mutable()
        ExecutorService executor = Executors.newFixedThreadPool(1)
        WorkflowEngine engine = new WorkflowEngine(ruleEngine, state, executor)
        Set<TestOperation> operations = [
                new TestOperation(
                        id: 1,
                        shouldRunClos: {
                            StateObj st ->
                                st.hasState(Workflows.WORKFLOW_STATE_KEY, Workflows.WORKFLOW_STATE_STARTED)
                        },
                        toCall: {
                            throw new RuntimeException("testing intentional failure")
                        },
                        failureState: States.state('akey', 'xvalue')
                ),
                new TestOperation(
                        id: 2,
                        shouldRunClos: {
                            StateObj st -> st.hasState('akey', 'avalue')
                        },
                        toCall: {
                            return new TestOpCompleted(newState: States.state('bkey', 'bvalue'))
                        }
                ),
        ]
        when:
        def result = engine.processOperations(operations,null)

        then:
        result.size() == 1
        result.find { it.operation.id == 1 }.success == null
        result.find { it.operation.id == 1 }.failure.message == 'testing intentional failure'
        state.hasState('akey', 'xvalue')
        !state.hasState('bkey', 'bvalue')
        operations[0].hasRun
        !operations[1].hasRun
    }

    def "shared data is merged"() {
        given:
        RuleEngine ruleEngine = Rules.createEngine()
        MutableStateObj state = States.mutable()
        ExecutorService executor = Executors.newFixedThreadPool(1)
        WorkflowEngine engine = new WorkflowEngine(ruleEngine, state, executor)
        Set<TestOperation> operations = [
                new TestOperation(
                        id: 1,
                        shouldRunClos: {
                            StateObj st
                                ->
                                st.hasState(Workflows.WORKFLOW_STATE_KEY, Workflows.WORKFLOW_STATE_STARTED)
                        },
                        toCall: {
                            return new TestOpCompleted(newState: States.state('akey', 'avalue'), result: [c: 'd'])
                        }
                ),
                new TestOperation(
                        id: 2,
                        shouldRunClos: {
                            StateObj st -> st.hasState('akey', 'avalue')
                        },
                        toCall: {
                            return new TestOpCompleted(newState: States.state('bkey', 'bvalue'), result: [e: 'f'])
                        },
                        ),
        ]

        def shared = new SharedMap()
        when:
        def result = engine.processOperations(operations, shared)

        then:
        shared.addedData == [[c: 'd'], [e: 'f']]
        operations[0].hasRun
        operations[0].input == [:]
        operations[1].hasRun
        operations[1].input == [c: 'd']
    }

    def "global shared data should not modify engine state"() {
        given:
        RuleEngine ruleEngine = Rules.createEngine()
        MutableStateObj state = States.mutable()
        ExecutorService executor = Executors.newFixedThreadPool(1)
        WorkflowEngine engine = new WorkflowEngine(ruleEngine, state, executor)
        def shared = new Sharedcontext()
        Set<TestOperationB> operations = [
                new TestOperationB(
                        id: 1,
                        shouldRunClos: {
                            StateObj st
                                ->
                                st.hasState(Workflows.WORKFLOW_STATE_KEY, Workflows.WORKFLOW_STATE_STARTED)
                        },
                        toCall: {
                            return new TestOpCompletedB(newState: States.state('akey', 'avalue'), result: WFSharedContext.with(ContextView.global(), new BaseDataContext(map)))
                        }
                ),
                new TestOperationB(
                        id: 2,
                        shouldRunClos: {
                            StateObj st -> st.hasState('akey', 'avalue')
                        },
                        toCall: {
                            return new TestOpCompletedB(newState: States.state('bkey', 'bvalue'), result: null)
                        },
                ),
        ]
        when:
        def result = engine.processOperations(operations, shared)

        then:
        !state.state.containsKey(expect)

        where:
        map                         | expect        | value
        [test: [val: "something"]]  | 'test.val'    | 'something'
        [export: [a: "b"]]          | 'export.a'    | 'b'
    }

    static class SharedMap implements WorkflowSystem.SharedData<Map, Map> {
        List<Map> addedData = []

        @Override
        void addData(final Map item) {
            addedData.add(item)
        }

        @Override
        Map produceNext() {
            addedData.isEmpty() ? [:] : addedData.last()
        }

        @Override
        Map produceState() {
            [:]
        }
    }

    static class Sharedcontext implements WorkflowSystem.SharedData<WFSharedContext, Map<String, String>> {
        WFSharedContext data = new WFSharedContext()
        Map<String, String> state = [:]
        @Override
        void addData(final WFSharedContext item) {
            data.merge(item)
            state.putAll(DataContextUtils.flattenDataContext(item.getData(ContextView.global())))
        }

        @Override
        WFSharedContext produceNext() {
            data
        }

        @Override
        Map<String, String> produceState() {
            state
        }
    }

}
