package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStepState

import static com.dtolabs.rundeck.core.execution.workflow.state.StateUtils.*

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/17/13
 * Time: 12:09 PM
 */
class MutableWorkflowStateImplTest extends GroovyTestCase {
    public void testCreate() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        assertEquals(ExecutionState.WAITING,mutableWorkflowState.getExecutionState());
        assertEquals([] , mutableWorkflowState.getNodeSet());
        assertEquals([] , mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertNull(mutableWorkflowState.getTimestamp());
        (0..1).each{i->
            assertEquals(ExecutionState.WAITING,mutableWorkflowState.stepStates[i].stepState.executionState)
            assertEquals([i+1],mutableWorkflowState.stepStates[i].stepIdentifier.context.collect{it.step})
        }
    }
    public void testCreateWithParentStepId() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2,null,StateUtils.stepIdentifier(6));
        assertEquals(ExecutionState.WAITING,mutableWorkflowState.getExecutionState());
        assertEquals([] , mutableWorkflowState.getNodeSet());
        assertEquals([] , mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertNull(mutableWorkflowState.getTimestamp());
        (0..1).each{i->
            assertEquals(ExecutionState.WAITING,mutableWorkflowState.stepStates[i].stepState.executionState)
            assertEquals([6,i+1],mutableWorkflowState.stepStates[i].stepIdentifier.context.collect{it.step})
        }
    }
    public void testWorkflowTime() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        Date date = new Date(123)
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a'])
        assertEquals(date,mutableWorkflowState.startTime)
        assertEquals(date,mutableWorkflowState.timestamp)
        assertNull(mutableWorkflowState.endTime)
        Date date2 = date+1
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, date2, null)
        assertEquals(date, mutableWorkflowState.startTime)
        assertEquals(date2, mutableWorkflowState.timestamp)
        assertEquals(date2,mutableWorkflowState.endTime)
    }
    public void testWorkflowStepTime() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        Date date = new Date(123)
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a'])
        assertEquals(date,mutableWorkflowState.startTime)
        assertEquals(date,mutableWorkflowState.timestamp)
        assertNull(mutableWorkflowState.endTime)
        Date date2 = date + 1

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)),
                date2)

        assertEquals(date2,mutableWorkflowState[1].stepState.startTime)
        assertEquals(date2,mutableWorkflowState[1].stepState.updateTime)
        assertEquals(null,mutableWorkflowState[1].stepState.endTime)

        Date date3 = date2 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.FAILED)),
                date3)

        assertEquals(date2, mutableWorkflowState[1].stepState.startTime)
        assertEquals(date3, mutableWorkflowState[1].stepState.updateTime)
        assertEquals(date3, mutableWorkflowState[1].stepState.endTime)

        Date date4 = date3 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER)),
                date4)

        assertEquals(date2, mutableWorkflowState[1].stepState.startTime)
        assertEquals(date4, mutableWorkflowState[1].stepState.updateTime)
        assertEquals(date3, mutableWorkflowState[1].stepState.endTime)

        Date date5 = date4 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.SUCCEEDED)),
                date5)

        assertEquals(date2, mutableWorkflowState[1].stepState.startTime)
        assertEquals(date5, mutableWorkflowState[1].stepState.updateTime)
        assertEquals(date5, mutableWorkflowState[1].stepState.endTime)
    }
    public void testNodeStepTime() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        Date date = new Date(123)
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a'])
        Date date2 = date + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)), date2)

        Date date3 = date2 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING),'a'),
                date3)

        assertEquals(date3, mutableWorkflowState[1].nodeStateMap['a'].startTime)
        assertEquals(date3, mutableWorkflowState[1].nodeStateMap['a'].updateTime)
        assertEquals(null, mutableWorkflowState[1].nodeStateMap['a'].endTime)

        Date date4 = date3 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.FAILED), 'a'),
                date4)

        assertEquals(date3, mutableWorkflowState[1].nodeStateMap['a'].startTime)
        assertEquals(date4, mutableWorkflowState[1].nodeStateMap['a'].updateTime)
        assertEquals(date4, mutableWorkflowState[1].nodeStateMap['a'].endTime)

        Date date5 = date4 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER), 'a'),
                date5)

        assertEquals(date3, mutableWorkflowState[1].nodeStateMap['a'].startTime)
        assertEquals(date5, mutableWorkflowState[1].nodeStateMap['a'].updateTime)
        assertEquals(date4, mutableWorkflowState[1].nodeStateMap['a'].endTime)

        Date date6 = date5 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'a'),
                date6)

        assertEquals(date3, mutableWorkflowState[1].nodeStateMap['a'].startTime)
        assertEquals(date6, mutableWorkflowState[1].nodeStateMap['a'].updateTime)
        assertEquals(date6, mutableWorkflowState[1].nodeStateMap['a'].endTime)
    }

    public void testUpdateWorkflowStep() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date()

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)),
                date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.getExecutionState());
        assertEquals([] , mutableWorkflowState.getNodeSet());
        assertEquals([] , mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getTimestamp());

        def WorkflowStepState state = mutableWorkflowState.getStepStates()[0]
        assertStepId(1, state.stepIdentifier)
        assertEquals(false, state.hasSubWorkflow())
        assertEquals(ExecutionState.RUNNING, state.stepState.executionState)
        assertEquals(null, state.stepState.errorMessage)
        assertEquals(null, state.stepState.metadata)
        assertEquals(null, state.subWorkflowState)
        assertEquals([:], state.nodeStateMap)
    }

    public void testUpdateWorkflowStepServerNode() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['x'], 2);
        mutableWorkflowState.serverNode='x'
        def date = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['x'])

        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.executionState)
        assertEquals(date, mutableWorkflowState.timestamp)

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)),
                date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.getExecutionState());
        assertEquals(['x'] , mutableWorkflowState.getNodeSet());
        assertEquals(['x'] , mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getTimestamp());

        def WorkflowStepState state = mutableWorkflowState.getStepStates()[0]
        assertStepId(1, state.stepIdentifier)
        assertEquals(false, state.hasSubWorkflow())
        assertEquals(ExecutionState.RUNNING, state.stepState.executionState)
        assertEquals(null, state.stepState.errorMessage)
        assertEquals(null, state.stepState.metadata)
        assertEquals(null, state.subWorkflowState)
        assertEquals(1, state.nodeStateMap.size())
        StepState nodestate = state.nodeStateMap['x']
        assertEquals(ExecutionState.RUNNING, nodestate.executionState)
        assertEquals(null, nodestate.metadata)
        assertEquals(null, nodestate.errorMessage)
    }

    public void testUpdateWorkflowStepServerNodeComplete() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['x'], 2);
        mutableWorkflowState.serverNode='x'
        def date = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['x'])

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)),
                date)

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.SUCCEEDED)),
                date)

        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.getExecutionState());
        assertEquals(['x'], mutableWorkflowState.getNodeSet());
        assertEquals(['x'], mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getTimestamp());

        def WorkflowStepState state = mutableWorkflowState.getStepStates()[0]
        assertStepId(1, state.stepIdentifier)
        assertEquals(false, state.hasSubWorkflow())
        assertEquals(ExecutionState.SUCCEEDED, state.stepState.executionState)
        assertEquals(null, state.stepState.errorMessage)
        assertEquals(null, state.stepState.metadata)
        assertEquals(null, state.subWorkflowState)
        assertEquals(1, state.nodeStateMap.size())
        StepState nodestate = state.nodeStateMap['x']
        assertEquals(ExecutionState.SUCCEEDED, nodestate.executionState)
        assertEquals(null, nodestate.metadata)
        assertEquals(null, nodestate.errorMessage)

    }

    private void assertStepId(int ident, StepIdentifier identifier) {
        assertEquals(1, identifier.context.size())
        assertEquals(ident, identifier.context[0].step)
    }

    public void testUpdateWorkflowSubStep() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date()

        mutableWorkflowState.updateStateForStep(stepIdentifier(1,1), stepStateChange(stepState(ExecutionState.RUNNING)),
                date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.getExecutionState());
        assertEquals([] , mutableWorkflowState.getNodeSet());
        assertEquals([] , mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getTimestamp());

        def WorkflowStepState state = mutableWorkflowState.getStepStates()[0]
        assertStepId(1, state.stepIdentifier)
        assertEquals(true, state.hasSubWorkflow())
        assertEquals(ExecutionState.RUNNING, state.stepState.executionState)
        assertEquals(null, state.stepState.errorMessage)
        assertEquals(null, state.stepState.metadata)
        assertEquals([:], state.nodeStateMap)

        def WorkflowState subWorkflowState = state.subWorkflowState
        assertEquals(ExecutionState.RUNNING, subWorkflowState.executionState)
        assertEquals([] , subWorkflowState.nodeSet)
        assertEquals(1, subWorkflowState.getStepStates().size())
        assertEquals(1, subWorkflowState.getStepCount())
        assertEquals(date, subWorkflowState.getTimestamp())
        def WorkflowStepState subStepState1 = subWorkflowState.getStepStates()[0]
        assertStepId(1, subStepState1.stepIdentifier)
        assertEquals(false  , subStepState1.hasSubWorkflow())
        assertEquals(ExecutionState.RUNNING, subStepState1.stepState.executionState)
        assertEquals(null, subStepState1.stepState.errorMessage)
        assertEquals(null, subStepState1.stepState.metadata)
        assertEquals([:], subStepState1.nodeStateMap)
    }

    public void testUpdateNodeStep() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date()

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING), 'testnode'),
                date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.getExecutionState());
        assertEquals([] , mutableWorkflowState.getNodeSet());
        assertEquals([] , mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getTimestamp());

        def WorkflowStepState state = mutableWorkflowState.getStepStates()[0]
        assertStepId(1, state.stepIdentifier)
        assertEquals(false, state.hasSubWorkflow())
        assertEquals(ExecutionState.RUNNING, state.stepState.executionState)
        assertEquals(null, state.stepState.errorMessage)
        assertEquals(null, state.stepState.metadata)
        assertEquals(null, state.subWorkflowState)
        assertEquals(1, state.nodeStateMap.size())
        StepState nodestate = state.nodeStateMap['testnode']
        assertEquals(ExecutionState.RUNNING, nodestate.executionState)
        assertEquals(null, nodestate.metadata)
        assertEquals(null, nodestate.errorMessage)
    }

    public void testUpdateNodeStepPredictedNodes() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['testnode'], 2);
        def date = new Date()

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING), 'testnode'),
                date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.getExecutionState());
        assertEquals(['testnode'] , mutableWorkflowState.getNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getTimestamp());

        def WorkflowStepState state = mutableWorkflowState.getStepStates()[0]
        assertStepId(1, state.stepIdentifier)
        assertEquals(false, state.hasSubWorkflow())
        assertEquals(ExecutionState.RUNNING, state.stepState.executionState)
        assertEquals(null, state.stepState.errorMessage)
        assertEquals(null, state.stepState.metadata)
        assertEquals(null, state.subWorkflowState)
        assertEquals(1, state.nodeStateMap.size())
        StepState nodestate = state.nodeStateMap['testnode']
        assertEquals(ExecutionState.RUNNING, nodestate.executionState)
        assertEquals(null, nodestate.metadata)
        assertEquals(null, nodestate.errorMessage)
    }

    public void testUpdateWorkflow() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a', 'b'] )

        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.executionState)
        assertEquals(date, mutableWorkflowState.timestamp)
        assertEquals(['a', 'b'] , mutableWorkflowState.nodeSet)

    }

    public void testUpdateWorkflowFinish() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a', 'b'] )
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a', 'b'] , mutableWorkflowState.nodeSet)

    }

    public void testUpdateWorkflowInvalid() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a', 'b'] )
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)
        try {
            mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, newdate, null)
            fail("shouldn't update state")
        } catch (IllegalStateException e) {
            assertNotNull(e)
        }

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a', 'b'] , mutableWorkflowState.nodeSet)
    }

    public void testBasicSubworkflow() {
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep1.nodeStep = false
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['a'], 1,[0:mutableStep1]);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a'])

        assertEquals(['a'], mutableWorkflowState.allNodes)

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        //step 1: sub workflow
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, ExecutionState.RUNNING, newdate, ['c', 'd'], null)
        assertEquals(['a', 'c', 'd'], mutableWorkflowState.allNodes)
        //start sub steps
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.RUNNING), 'c'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'c'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.RUNNING), 'd'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'd'), newdate)

        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, ExecutionState.SUCCEEDED, newdate, null, null)

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)

        //finish
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)


        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a'], mutableWorkflowState.nodeSet)
        assertEquals(['a','c','d'], mutableWorkflowState.allNodes)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.SUCCEEDED, step1.stepState.executionState)
        assertEquals(null, step1.nodeStepTargets)
        assertNotNull( step1.mutableSubWorkflowState)
        def sub1 = step1.subWorkflowState

        def substep1 = sub1.stepStates[0]
        assertEquals([1], substep1.stepIdentifier.context.collect{it.step})
        assertEquals(ExecutionState.SUCCEEDED, substep1.stepState.executionState)
        assertEquals(['c','d'], substep1.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, substep1.nodeStateMap['c'].executionState)
        assertEquals(ExecutionState.SUCCEEDED, substep1.nodeStateMap['d'].executionState)

    }

    public void testErrorHandlerBasic() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 1);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a'] )

        //step 1: error handler
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.FAILED), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'a'), newdate)

        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)


        //finish
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)


        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a'] , mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.SUCCEEDED, step1.stepState.executionState)
        assertEquals(['a'] , step1.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['a'].executionState)

    }

    public void testErrorHandler() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 1);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a','b'] )

        //step 1: error handler
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.FAILED), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'a'), newdate)

        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)


        //finish
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)


        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a','b'] , mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.NODE_PARTIAL_SUCCEEDED, step1.stepState.executionState)
        assertEquals(['a','b'] , step1.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['a'].executionState)

    }
    public void testErrorHandlerNodeFirst() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 1);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a','b'] )

        //step 1: error handler
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.FAILED), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'a'), newdate)


        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING), 'b'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.FAILED), 'b'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER), 'b'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'b'), newdate)


        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)

        //finish
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)


        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a','b'] , mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.SUCCEEDED, step1.stepState.executionState)
        assertEquals(['a','b'] , step1.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['a'].executionState)

    }
    public void testErrorHandlerStepFirst() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 1);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a','b'] )

        //step 1: error handler
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.FAILED), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'a'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER), 'b'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'b'), newdate)


        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)


        //finish
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)


        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a','b'] , mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.SUCCEEDED, step1.stepState.executionState)
        assertEquals(['a','b'] , step1.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['a'].executionState)

    }

    public void testUpdateSubWorkflowResolveState() {
        def mutableStep8 = new MutableWorkflowStepStateImpl(stepIdentifier(8))
        mutableStep8.nodeStep=true

        def mutableStep4 = new MutableWorkflowStepStateImpl(stepIdentifier(4))
        mutableStep4.nodeStep=true

        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 9,[7: mutableStep8,3:mutableStep4]);
        def date = new Date(123)
        def newdate = new Date()


        assertFalse(mutableWorkflowState[1].nodeStep)
        assertFalse(mutableWorkflowState[2].nodeStep)
        assertFalse(mutableWorkflowState[3].nodeStep)
        assertTrue(mutableWorkflowState[4].nodeStep)
        assertFalse(mutableWorkflowState[5].nodeStep)
        assertFalse(mutableWorkflowState[6].nodeStep)
        assertFalse(mutableWorkflowState[7].nodeStep)
        assertTrue(mutableWorkflowState[8].nodeStep)
        assertFalse(mutableWorkflowState[9].nodeStep)

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a', 'b'] )

        //step 1: partial success
        mutableWorkflowState.updateStateForStep(stepIdentifier(1),stepStateChange(stepState(ExecutionState.RUNNING)),newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1),stepStateChange(stepState(ExecutionState.RUNNING),'a'),newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1),stepStateChange(stepState(ExecutionState.SUCCEEDED),'a'),newdate)

        assertFalse(mutableWorkflowState[2].nodeStep)
        mutableWorkflowState.updateStateForStep(stepIdentifier(2), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        assertFalse(mutableWorkflowState[2].nodeStep)
        //step 2: sub workflow
        //start subworkflow
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(2),0, ExecutionState.RUNNING, newdate, ['c','d'], null)
        //start sub steps
        //step 2/1: mixed failure
        mutableWorkflowState.updateStateForStep(stepIdentifier(2,1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(2,1), stepStateChange(stepState(ExecutionState.RUNNING),'c'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(2,1), stepStateChange(stepState(ExecutionState.FAILED),'c'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(2, 1), stepStateChange(stepState(ExecutionState.FAILED)), newdate)
        //nb: node 'd' was not run

        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(2),0, ExecutionState.FAILED, newdate, null, null)

        //error handler executed to recover
        mutableWorkflowState.updateStateForStep(stepIdentifier(2), stepStateChange(stepState(ExecutionState.FAILED)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(2,true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(2, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)

        //step 3:  mixed state node results
        stepIdentifier(3).with {
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING), 'a'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.SUCCEEDED), 'a'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING), 'b'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.FAILED), 'b'), newdate)
        }

        //step 4: node step, no nodes not started
        mutableWorkflowState.updateStateForStep(stepIdentifier(4), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)

        //step 5: all aborted
        stepIdentifier(5).with{
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING), 'a'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.ABORTED), 'a'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING), 'b'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.ABORTED), 'b'), newdate)
        }
        //step 6: all failed
        stepIdentifier(6).with{
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING), 'a'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.FAILED), 'a'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING), 'b'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.FAILED), 'b'), newdate)
        }
        //step 7: all succeeeded
        stepIdentifier(7).with{
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING), 'a'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.SUCCEEDED), 'a'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING), 'b'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.SUCCEEDED), 'b'), newdate)
        }

        //step 8: did not start, no nodes executed
        //step 9: did not start, workflow step

        //finish
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)


        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a', 'b'] , mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.NODE_PARTIAL_SUCCEEDED, step1.stepState.executionState)
        assertEquals(['a','b'] , step1.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['a'].executionState)
        assertNotNull(step1.nodeStateMap['b'])
        assertEquals(ExecutionState.NOT_STARTED, step1.nodeStateMap['b'].executionState)

        def step2 = mutableWorkflowState[2]
        assertStepId(2, step2.stepIdentifier)
        assertEquals(ExecutionState.SUCCEEDED, step2.stepState.executionState)
        assertNotNull(step2.subWorkflowState)
        assertStepId(1, step2.subWorkflowState[1].stepIdentifier)
        assertEquals(ExecutionState.FAILED, step2.subWorkflowState[1].stepState.executionState)
        assertEquals(['c', 'd'] , step2.subWorkflowState[1].nodeStepTargets)
        assertEquals(ExecutionState.FAILED, step2.subWorkflowState[1].nodeStateMap['c'].executionState)
        assertEquals(ExecutionState.NOT_STARTED, step2.subWorkflowState[1].nodeStateMap['d'].executionState)

        def step3 = mutableWorkflowState[3]
        assertStepId(3, step3.stepIdentifier)
        assertEquals(ExecutionState.NODE_MIXED, step3.stepState.executionState)
        assertEquals(['a', 'b'] , step3.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step3.nodeStateMap['a'].executionState)
        assertNotNull(step3.nodeStateMap['b'])
        assertEquals(ExecutionState.FAILED, step3.nodeStateMap['b'].executionState)

        def step4 = mutableWorkflowState[4]
        assertStepId(4, step4.stepIdentifier)
        assertEquals(ExecutionState.NOT_STARTED, step4.stepState.executionState)
        assertEquals(['a', 'b'] , step4.nodeStepTargets)
        assertEquals(ExecutionState.NOT_STARTED, step4.nodeStateMap['a'].executionState)
        assertNotNull(step4.nodeStateMap['b'])
        assertEquals(ExecutionState.NOT_STARTED, step4.nodeStateMap['b'].executionState)

        def step5 = mutableWorkflowState[5]
        assertStepId(5, step5.stepIdentifier)
        assertEquals(ExecutionState.ABORTED, step5.stepState.executionState)
        assertEquals(['a', 'b'] , step5.nodeStepTargets)
        assertEquals(ExecutionState.ABORTED, step5.nodeStateMap['a'].executionState)
        assertNotNull(step5.nodeStateMap['b'])
        assertEquals(ExecutionState.ABORTED, step5.nodeStateMap['b'].executionState)

        def step6 = mutableWorkflowState[6]
        assertStepId(6, step6.stepIdentifier)
        assertEquals(ExecutionState.FAILED, step6.stepState.executionState)
        assertEquals(['a', 'b'] , step6.nodeStepTargets)
        assertEquals(ExecutionState.FAILED, step6.nodeStateMap['a'].executionState)
        assertNotNull(step6.nodeStateMap['b'])
        assertEquals(ExecutionState.FAILED, step6.nodeStateMap['b'].executionState)

        def step7 = mutableWorkflowState[7]
        assertStepId(7, step7.stepIdentifier)
        assertEquals(ExecutionState.SUCCEEDED, step7.stepState.executionState)
        assertEquals(['a', 'b'] , step7.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step7.nodeStateMap['a'].executionState)
        assertNotNull(step7.nodeStateMap['b'])
        assertEquals(ExecutionState.SUCCEEDED, step7.nodeStateMap['b'].executionState)

        def step8 = mutableWorkflowState[8]
        assertStepId(8, step8.stepIdentifier)
        assertEquals(true, step8.nodeStep)
        assertEquals(ExecutionState.NOT_STARTED, step8.stepState.executionState)
//        assertEquals(['a', 'b'] , step8.nodeStepTargets)
        assertEquals(ExecutionState.NOT_STARTED, step8.nodeStateMap['a'].executionState)
        assertNotNull(step8.nodeStateMap['b'])
        assertEquals(ExecutionState.NOT_STARTED, step8.nodeStateMap['b'].executionState)
        def step9 = mutableWorkflowState[9]
        assertStepId(9, step9.stepIdentifier)
        assertEquals(false, step9.nodeStep)
        assertEquals(ExecutionState.NOT_STARTED, step9.stepState.executionState)

    }
    public void testUpdateStateNormal() {
        assertEquals(ExecutionState.WAITING, MutableWorkflowStateImpl.updateState(null, ExecutionState.WAITING))
        assertEquals(ExecutionState.RUNNING, MutableWorkflowStateImpl.updateState(null, ExecutionState.RUNNING))
        assertEquals(ExecutionState.RUNNING, MutableWorkflowStateImpl.updateState(ExecutionState.RUNNING, ExecutionState.RUNNING))
        assertEquals(ExecutionState.RUNNING, MutableWorkflowStateImpl.updateState(ExecutionState.WAITING, ExecutionState.RUNNING))
        assertEquals(ExecutionState.SUCCEEDED, MutableWorkflowStateImpl.updateState(ExecutionState.WAITING, ExecutionState.SUCCEEDED))
        assertEquals(ExecutionState.FAILED, MutableWorkflowStateImpl.updateState(ExecutionState.WAITING, ExecutionState.FAILED))
        assertEquals(ExecutionState.ABORTED, MutableWorkflowStateImpl.updateState(ExecutionState.WAITING, ExecutionState.ABORTED))
        assertEquals(ExecutionState.SUCCEEDED, MutableWorkflowStateImpl.updateState(ExecutionState.RUNNING, ExecutionState.SUCCEEDED))
        assertEquals(ExecutionState.FAILED, MutableWorkflowStateImpl.updateState(ExecutionState.RUNNING, ExecutionState.FAILED))
        assertEquals(ExecutionState.ABORTED, MutableWorkflowStateImpl.updateState(ExecutionState.RUNNING, ExecutionState.ABORTED))
    }
    public void testUpdateStateInvalid() {
        try {
            MutableWorkflowStateImpl.updateState(ExecutionState.RUNNING, ExecutionState.WAITING)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }
        try {
            MutableWorkflowStateImpl.updateState(ExecutionState.SUCCEEDED, ExecutionState.RUNNING)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }
        try {
            MutableWorkflowStateImpl.updateState(ExecutionState.FAILED, ExecutionState.RUNNING)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }
        try {
            MutableWorkflowStateImpl.updateState(ExecutionState.ABORTED, ExecutionState.RUNNING)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }
        try {
            MutableWorkflowStateImpl.updateState(ExecutionState.ABORTED, ExecutionState.WAITING)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }
        try {
            MutableWorkflowStateImpl.updateState(ExecutionState.SUCCEEDED, null)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }
        try {
            MutableWorkflowStateImpl.updateState(ExecutionState.ABORTED, null)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }
        try {
            MutableWorkflowStateImpl.updateState(ExecutionState.FAILED, null)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }
        try {
            MutableWorkflowStateImpl.updateState(ExecutionState.RUNNING, null)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }

    }
}
