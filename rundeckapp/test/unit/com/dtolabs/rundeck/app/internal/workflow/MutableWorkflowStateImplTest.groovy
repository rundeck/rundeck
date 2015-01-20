package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.MutableExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils
import com.dtolabs.rundeck.core.execution.workflow.state.StepContextId
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStepState

import grails.converters.JSON
import grails.test.mixin.support.GrailsUnitTestMixin;

import java.text.SimpleDateFormat

import static com.dtolabs.rundeck.core.execution.workflow.state.StateUtils.*

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/17/13
 * Time: 12:09 PM
 */

@TestMixin(GrailsUnitTestMixin)
class MutableWorkflowStateImplTest  {
    class TestMutableExecutionState implements MutableExecutionState{
        ExecutionState executionState
    }
    public void testCreate() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        assertEquals(ExecutionState.WAITING,mutableWorkflowState.getExecutionState());
        assertEquals([] , mutableWorkflowState.getNodeSet());
        assertEquals([] , mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertNull(mutableWorkflowState.getUpdateTime());
        (0..1).each{i->
            assertEquals(ExecutionState.WAITING,mutableWorkflowState.stepStates[i].stepState.executionState)
            assertEquals([i+1],mutableWorkflowState.stepStates[i].stepIdentifier.context.collect{it.step})
        }
    }
    public void testCreateWithParentStepId() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2,null,StateUtils.stepIdentifier(6),null);
        assertEquals(ExecutionState.WAITING,mutableWorkflowState.getExecutionState());
        assertEquals([] , mutableWorkflowState.getNodeSet());
        assertEquals([] , mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertNull(mutableWorkflowState.getUpdateTime());
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
        assertEquals(date,mutableWorkflowState.updateTime)
        assertNull(mutableWorkflowState.endTime)
        Date date2 = date+1
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, date2, null)
        assertEquals(date, mutableWorkflowState.startTime)
        assertEquals(date2, mutableWorkflowState.updateTime)
        assertEquals(date2,mutableWorkflowState.endTime)
    }
    public void testWorkflowStepTime() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        Date date = new Date(123)
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a'])
        assertEquals(date,mutableWorkflowState.startTime)
        assertEquals(date,mutableWorkflowState.updateTime)
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
    public void testWorkflowStepServerNodeTime() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['a'], 2,null,null,'a');
        Date date = new Date(123)
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a'])
        Date date2 = date + 1
        Date date3 = date2 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)), date3)


        assertEquals(date3, mutableWorkflowState[1].nodeStateMap['a'].startTime)
        assertEquals(date3, mutableWorkflowState[1].nodeStateMap['a'].updateTime)
        assertEquals(null, mutableWorkflowState[1].nodeStateMap['a'].endTime)

        Date date4 = date3 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.FAILED)), date4)

        assertEquals(date3, mutableWorkflowState[1].nodeStateMap['a'].startTime)
        assertEquals(date4, mutableWorkflowState[1].nodeStateMap['a'].updateTime)
        assertEquals(date4, mutableWorkflowState[1].nodeStateMap['a'].endTime)

        Date date5 = date4 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER)), date5)

        assertEquals(date3, mutableWorkflowState[1].nodeStateMap['a'].startTime)
        assertEquals(date5, mutableWorkflowState[1].nodeStateMap['a'].updateTime)
        assertEquals(date4, mutableWorkflowState[1].nodeStateMap['a'].endTime)

        Date date6 = date5 + 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.SUCCEEDED)), date6)

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
        assertEquals(date, mutableWorkflowState.getUpdateTime());

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
        assertEquals(date, mutableWorkflowState.updateTime)

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)),
                date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.getExecutionState());
        assertEquals(['x'] , mutableWorkflowState.getNodeSet());
        assertEquals(['x'] , mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getUpdateTime());

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
        def date2 = date + 1

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['x'])

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)),
                date)

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.SUCCEEDED)),
                date2)

        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.getExecutionState());
        assertEquals(['x'], mutableWorkflowState.getNodeSet());
        assertEquals(['x'], mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date2, mutableWorkflowState.getUpdateTime());

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
        assertEquals(date, mutableWorkflowState.getUpdateTime());

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
        assertEquals(date, subWorkflowState.getUpdateTime())
        def WorkflowStepState subStepState1 = subWorkflowState.getStepStates()[0]
        assertStepId(1, subStepState1.stepIdentifier)
        assertEquals(false  , subStepState1.hasSubWorkflow())
        assertEquals(ExecutionState.RUNNING, subStepState1.stepState.executionState)
        assertEquals(null, subStepState1.stepState.errorMessage)
        assertEquals(null, subStepState1.stepState.metadata)
        assertEquals([:], subStepState1.nodeStateMap)
    }
    /**
     * Finishing a top-level workflow should cascade the finalized state to sub workflows
     */
    public void testUpdateWorkflowSubStepPrepare() {
        def (MutableWorkflowStateImpl mutableWorkflowState, Date date) = prepareSubWorkflow()
        assertSubworkflowState(mutableWorkflowState, date, ExecutionState.RUNNING, ExecutionState.RUNNING, ExecutionState.WAITING, ExecutionState.RUNNING)
    }

    private void assertSubworkflowState(MutableWorkflowStateImpl mutableWorkflowState, Date date,
                                        ExecutionState subworkflowState, ExecutionState runningState,
                                        ExecutionState waitingState, ExecutionState nodeSubState) {
        assertEquals(runningState, mutableWorkflowState.getExecutionState());
        assertEquals(['a'], mutableWorkflowState.getNodeSet());
        assertEquals(['a'], mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getUpdateTime());

        mutableWorkflowState.getStepStates()[0].with { WorkflowStepState step1 ->
            assertStepId(1, step1.stepIdentifier)
            assertEquals(true, step1.hasSubWorkflow())
            assertEquals(false, step1.isNodeStep())
            assertEquals(subworkflowState, step1.stepState.executionState)
            assertEquals(null, step1.stepState.errorMessage)
            assertEquals(null, step1.stepState.metadata)
            assertEquals([:], step1.nodeStateMap)

            step1.subWorkflowState.with { WorkflowState subWorkflowState ->
                assertEquals(runningState, subWorkflowState.executionState)
                assertEquals(['b'], subWorkflowState.nodeSet)
                assertEquals(2, subWorkflowState.getStepStates().size())
                assertEquals(2, subWorkflowState.getStepCount())
                assertEquals(date, subWorkflowState.getUpdateTime())

                def WorkflowStepState subStepState1 = subWorkflowState.getStepStates()[0]
                assertStepId(1, subStepState1.stepIdentifier)
                assertEquals(false, subStepState1.hasSubWorkflow())
                assertEquals(true, subStepState1.isNodeStep())
                assertEquals(nodeSubState, subStepState1.stepState.executionState)
                assertNotNull(subStepState1.nodeStateMap)
                assertNotNull(subStepState1.nodeStateMap['b'])
                StepState subnodestate1 = subStepState1.nodeStateMap['b']
                assertEquals(runningState, subnodestate1.executionState)

                def WorkflowStepState subStepState2 = subWorkflowState.getStepStates()[1]
                assertStepId(2, subStepState2.stepIdentifier)
                assertEquals(false, subStepState2.hasSubWorkflow())
                assertEquals(false, subStepState2.isNodeStep())
                assertEquals(waitingState, subStepState2.stepState.executionState)
                assertNotNull(subStepState2.nodeStateMap)
                assertNotNull(subStepState2.nodeStateMap['a'])
                StepState subnodestate2 = subStepState2.nodeStateMap['a']
                assertEquals(waitingState, subnodestate2.executionState)
            }
        }

        mutableWorkflowState.getStepStates()[1].with { WorkflowStepState step2 ->
            assertStepId(2, step2.stepIdentifier)
            assertEquals(false, step2.hasSubWorkflow())
            assertEquals(false, step2.isNodeStep())
            assertEquals(waitingState, step2.stepState.executionState)
            assertNotNull(step2.nodeStateMap['a'])
            StepState subnodestate2 = step2.nodeStateMap['a']
            assertEquals(waitingState, subnodestate2.executionState)

        }
    }

    public void testUpdateWorkflowSubStepResolve() {
        def (MutableWorkflowStateImpl mutableWorkflowState, Date date) = prepareSubWorkflow()
        Date endDate= date+1
        mutableWorkflowState.updateWorkflowState(ExecutionState.ABORTED, endDate, null)
        assertSubworkflowState(mutableWorkflowState, endDate, ExecutionState.NODE_MIXED, ExecutionState.ABORTED, ExecutionState.NOT_STARTED,
                ExecutionState.NODE_MIXED)

    }

    private List prepareSubWorkflow() {
        def mutableStep11 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep11.nodeStep = true
        def mutableStep12 = new MutableWorkflowStepStateImpl(stepIdentifier(2))
        mutableStep12.nodeStep = false

        MutableWorkflowStateImpl sub1 = new MutableWorkflowStateImpl(['b'], 2, [0: mutableStep11,1:mutableStep12], StateUtils.stepIdentifier(1), 'a');

        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1), sub1)
        mutableStep1.nodeStep = false
        def mutableStep2 = new MutableWorkflowStepStateImpl(stepIdentifier(2))
        mutableStep2.nodeStep = false

        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['a'], 2, [0: mutableStep1,1:mutableStep2], null, 'a');

        def date = new Date()
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a'])
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)), date)

        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0,false, ExecutionState.RUNNING, date, ['b'], null)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.RUNNING)), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.RUNNING),'b'), date)
        [mutableWorkflowState, date]
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
        assertEquals(date, mutableWorkflowState.getUpdateTime());

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
        assertEquals(date, mutableWorkflowState.getUpdateTime());

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
        assertEquals(date, mutableWorkflowState.updateTime)
        assertEquals(['a', 'b'] , mutableWorkflowState.nodeSet)

    }

    public void testUpdateWorkflowFinish() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a', 'b'] )
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.updateTime)
        assertEquals(['a', 'b'] , mutableWorkflowState.nodeSet)

    }

    public void testUpdateWorkflowFinalStateDoesntChange() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a', 'b'] )
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, newdate, null)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.updateTime)
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
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0,false, ExecutionState.RUNNING, newdate, ['c', 'd'], null)
        assertEquals(['a', 'c', 'd'], mutableWorkflowState.allNodes)
        //start sub steps
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.RUNNING), 'c'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'c'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.RUNNING), 'd'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'd'), newdate)

        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, false, ExecutionState.SUCCEEDED, newdate, null, null)

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)

        //finish
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)


        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.updateTime)
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
        assertEquals(newdate, mutableWorkflowState.updateTime)
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
        assertEquals(newdate, mutableWorkflowState.updateTime)
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
        assertEquals(newdate, mutableWorkflowState.updateTime)
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
        assertEquals(newdate, mutableWorkflowState.updateTime)
        assertEquals(['a','b'] , mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.SUCCEEDED, step1.stepState.executionState)
        assertEquals(['a','b'] , step1.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['a'].executionState)

    }

    void processStateChanges(MutableWorkflowStateImpl mutableWorkflowState, List<Map> changes) {
        changes.eachWithIndex{Map change, int ndx->
            try{
                processStateChange(mutableWorkflowState,change)
            }catch (Throwable t){
                t.printStackTrace(System.err)
                fail("Error processing change: ${t}: ${ndx}: ${change}")
            }
        }
    }

    protected void processStateChange(MutableWorkflowStateImpl mutableWorkflowState,Map change) {
        if (change.workflow) {
            mutableWorkflowState.updateWorkflowState(parseState(change.workflow.state),
                    parseDate(change.workflow.date), change.workflow.nodes?:null)
        } else if (change.step) {
            def stepchange = change.step.node ? stepStateChange(stepState(parseState(change.step.state)),
                    change.step.node) : stepStateChange(stepState(parseState(change.step.state)))
            def ident = parseStepIdent(change.step.ident)
            def index = change.step.index ?: 0
            def date = parseDate(change.step.date)

            mutableWorkflowState.updateStateForStep(ident, index, stepchange,
                    date)
        } else if (change.subworkflow) {
            mutableWorkflowState.updateSubWorkflowState(parseStepIdent(change.subworkflow.ident),
                    change.subworkflow.index, change.subworkflow.quell ? true : false,
                    parseState(change.subworkflow.state),
                    parseDate(change.subworkflow.date), change.subworkflow.nodes?:null, change.subworkflow.parent?:null)
        }
    }

    public void testErrorHandlerSubworkflow1() {
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep1.nodeStep = false
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['a'], 1, [0: mutableStep1]);

        def date = new Date(123)
        def newdate = new Date()
        def changes=[]

//        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a'])
        changes << [workflow:[state: ExecutionState.RUNNING, date: date, nodes: ['a']]]

//        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        changes << [step: [ident:stepIdentifier(1), state: ExecutionState.RUNNING, date: newdate]]
        //step 1: sub workflow
//        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, false, ExecutionState.RUNNING, newdate, ['a' ], null)
        changes << [subworkflow: [ident: stepIdentifier(1), index:0, quell:false, state: ExecutionState.RUNNING, date: newdate, nodes: ['a'] ]]
//        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        changes << [step: [ident: stepIdentifier(1,1), state: ExecutionState.RUNNING, date: newdate]]
//        mutableWorkflowState.updateStateForStep(stepIdentifier(1,1), stepStateChange(stepState(ExecutionState.RUNNING), 'a'), newdate)
        changes << [step: [ident: stepIdentifier(1, 1), state: ExecutionState.RUNNING, date: newdate, node: 'a']]
//        mutableWorkflowState.updateStateForStep(stepIdentifier(1,1), stepStateChange(stepState(ExecutionState.FAILED), 'a'), newdate)
        changes << [step: [ident: stepIdentifier(1, 1), state: ExecutionState.FAILED, date: newdate, node: 'a']]
//        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), stepStateChange(stepState(ExecutionState.FAILED)), newdate)
        changes << [step: [ident: stepIdentifier(1, 1), state: ExecutionState.FAILED, date: newdate]]

        changes << [step: [ident: stepIdentifier(1), state: ExecutionState.FAILED, date: newdate]]
//        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER)), newdate)
        changes << [step: [ident: stepIdentifier(stepContextId(1, true)), state: ExecutionState.RUNNING_HANDLER, date: newdate]]
//        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER), 'a'), newdate)
        changes << [step: [ident: stepIdentifier(stepContextId(1, true)), state: ExecutionState.RUNNING_HANDLER, date : newdate, node:'a']]
//        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED), 'a'), newdate)
        changes << [step: [ident: stepIdentifier(stepContextId(1, true)), state: ExecutionState.SUCCEEDED, date: newdate, node: 'a']]
//        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(1, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)
        changes << [step: [ident: stepIdentifier(stepContextId(1, true)), state: ExecutionState.SUCCEEDED, date: newdate]]

        //finish
//        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)
        changes << [workflow: [state: ExecutionState.SUCCEEDED, date: date]]
        processStateChanges(mutableWorkflowState,changes)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.updateTime)
        assertEquals(['a'], mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.SUCCEEDED, step1.stepState.executionState)
        assertEquals(['a'], step1.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['a'].executionState)
        def WorkflowState subWorkflowState = step1.subWorkflowState
        assertEquals(ExecutionState.SUCCEEDED, subWorkflowState.executionState)
        def WorkflowStepState subStepState1 = subWorkflowState.getStepStates()[0]
        assertEquals(ExecutionState.FAILED, subStepState1.stepState.executionState)
    }

    /**
     * Workflow step is a job reference, and the error handler is too
     */
    public void testSubworkflowErrorHandlerWorkflow() {
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep1.nodeStep = false
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['localhost'], 1, [0: mutableStep1]);

        processStateChange mutableWorkflowState,
                ["workflow":
                        [
                                "date": "2014-09-05T19:16:14Z",
                                "nodes": ["localhost"],
                                "state": "RUNNING"
                        ]
                ]
        processStateChange mutableWorkflowState,
                ["step":
                        [
                                "state": "RUNNING",
                                "date": "2014-09-05T19:16:14Z",
                                "index": 0,
                                "ident": "1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                ["subworkflow":
                        [
                                "quell": false,
                                "nodes": ["localhost"],
                                "state": "RUNNING",
                                "index": 0,
                                "ident": "1",
                                "date": "2014-09-05T19:16:14Z"
                        ]
                ]
        processStateChange mutableWorkflowState,       [
                        "step": [
                                "state": "RUNNING",
                                "date": "2014-09-05T19:16:14Z",
                                "index": 0,
                                "ident": "1/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "localhost",
                                "state": "RUNNING",
                                "date": "2014-09-05T19:16:14Z",
                                "index": 0,
                                "ident": "1/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "localhost",
                                "meta": [
                                        "failureReason": "NonZeroResultCode",
                                        "resultCode": 1
                                ],
                                "state": "FAILED",
                                "errorMessage": "Result code was 1",
                                "date": "2014-09-05T19:16:15Z",
                                "index": 0,
                                "ident": "1/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "subworkflow": [
                                "quell": false,
                                "nodes": null,
                                "state": "FAILED",
                                "index": 0,
                                "ident": "1",
                                "date": "2014-09-05T19:16:15Z"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "meta": [
                                        "handlerTriggered": "true"
                                ],
                                "state": "RUNNING_HANDLER",
                                "errorMessage": null,
                                "date": "2014-09-05T19:16:15Z",
                                "index": 0,
                                "ident": "1e"
                        ]
                ]
        //error handler context allows transition from FAILED to RUNNING in this change
        processStateChange mutableWorkflowState,
                [
                        "subworkflow": [
                                "quell": false,
                                "nodes": [
                                        "alpha1",
                                        "alpha2",
                                        "alpha4"
                                ],
                                "state": "RUNNING",
                                "index": 0,
                                "ident": "1e",
                                "date": "2014-09-05T19:16:15Z"
                        ]
                ]

        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.executionState)
        assertEquals(['localhost'], mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.RUNNING_HANDLER, step1.stepState.executionState)
        def WorkflowState subWorkflowState = step1.subWorkflowState
        assertEquals(ExecutionState.RUNNING, subWorkflowState.executionState)
        def WorkflowStepState subStepState1 = subWorkflowState.getStepStates()[0]
        assertEquals(['localhost'], subStepState1.nodeStepTargets)
        assertEquals(ExecutionState.FAILED, subStepState1.nodeStateMap['localhost'].executionState)
        assertEquals(ExecutionState.FAILED, subStepState1.stepState.executionState)
    }
    /**
     * Workflow step is a job reference, and the error handler is too
     */
    public void testSubworkflowErrorHandlerWorkflow2() {
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep1.nodeStep = false
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['localhost'], 1, [0: mutableStep1]);

        processStateChange mutableWorkflowState, [
                "workflow": [
                        "date": "2014-09-05T19:57:37Z",
                        "nodes": [
                                "localhost"
                        ],
                        "state": "RUNNING"
                ]
        ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:38Z",
                                "index": 0,
                                "ident": "1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "subworkflow": [
                                "quell": false,
                                "nodes": [
                                        "localhost"
                                ],
                                "state": "RUNNING",
                                "index": 0,
                                "ident": "1",
                                "date": "2014-09-05T19:57:38Z"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:38Z",
                                "index": 0,
                                "ident": "1/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "localhost",
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:38Z",
                                "index": 0,
                                "ident": "1/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "localhost",
                                "meta": [
                                        "failureReason": "NonZeroResultCode",
                                        "resultCode": 1
                                ],
                                "state": "FAILED",
                                "errorMessage": "Result code was 1",
                                "date": "2014-09-05T19:57:38Z",
                                "index": 0,
                                "ident": "1/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "subworkflow": [
                                "quell": false,
                                "nodes": null,
                                "state": "FAILED",
                                "index": 0,
                                "ident": "1",
                                "date": "2014-09-05T19:57:38Z"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "meta": [
                                        "handlerTriggered": "true"
                                ],
                                "state": "RUNNING_HANDLER",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:38Z",
                                "index": 0,
                                "ident": "1e"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "subworkflow": [
                                "quell": false,
                                "nodes": [
                                        "alpha1",
                                        "alpha2",
                                        "alpha4"
                                ],
                                "state": "RUNNING",
                                "index": 0,
                                "ident": "1e",
                                "date": "2014-09-05T19:57:38Z"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:38Z",
                                "index": 0,
                                "ident": "1e/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "alpha1",
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:38Z",
                                "index": 0,
                                "ident": "1e/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "alpha1",
                                "meta": null,
                                "state": "SUCCEEDED",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:39Z",
                                "index": 0,
                                "ident": "1e/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:39Z",
                                "index": 0,
                                "ident": "1e/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "alpha2",
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:39Z",
                                "index": 0,
                                "ident": "1e/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "alpha2",
                                "meta": null,
                                "state": "SUCCEEDED",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:39Z",
                                "index": 0,
                                "ident": "1e/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:39Z",
                                "index": 0,
                                "ident": "1e/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "alpha4",
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:39Z",
                                "index": 0,
                                "ident": "1e/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "alpha4",
                                "meta": null,
                                "state": "SUCCEEDED",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:40Z",
                                "index": 0,
                                "ident": "1e/1"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "subworkflow": [
                                "quell": false,
                                "nodes": null,
                                "state": "SUCCEEDED",
                                "index": 0,
                                "ident": "1e",
                                "date": "2014-09-05T19:57:40Z"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:40Z",
                                "index": 0,
                                "ident": "2"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "localhost",
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:40Z",
                                "index": 0,
                                "ident": "2"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "step": [
                                "node": "localhost",
                                "meta": null,
                                "state": "SUCCEEDED",
                                "errorMessage": null,
                                "date": "2014-09-05T19:57:40Z",
                                "index": 0,
                                "ident": "2"
                        ]
                ]
        processStateChange mutableWorkflowState,
                [
                        "workflow": [
                                "date": "2014-09-05T19:57:40Z",
                                "nodes": null,
                                "state": "SUCCEEDED"
                        ]
                ]




        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(['localhost'], mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.NODE_MIXED, step1.stepState.executionState)
        def WorkflowState subWorkflowState = step1.subWorkflowState
        assertEquals(ExecutionState.SUCCEEDED, subWorkflowState.executionState)
        def WorkflowStepState subStepState1 = subWorkflowState.getStepStates()[0]
        assertEquals(['localhost'], subStepState1.nodeStepTargets)
        assertEquals(ExecutionState.FAILED, subStepState1.nodeStateMap['localhost'].executionState)
        assertEquals(ExecutionState.SUCCEEDED, subStepState1.nodeStateMap['alpha1'].executionState)
        assertEquals(ExecutionState.SUCCEEDED, subStepState1.nodeStateMap['alpha2'].executionState)
        assertEquals(ExecutionState.SUCCEEDED, subStepState1.nodeStateMap['alpha4'].executionState)
        assertEquals(ExecutionState.NODE_MIXED, subStepState1.stepState.executionState)
    }
    /**
     * Workflow step is a node step, error handler is a subworkflow as node step
     * Workflow error handler should ignore the node "parameter"
     */
    public void testSubworkflowErrorHandlerWorkflow3() {
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep1.nodeStep = true
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['localhost'], 1, [0: mutableStep1]);

        def changes= [
                [
                        "workflow": [
                                "date": "2014-09-05T21:28:04Z",
                                "nodes": [
                                        "localhost"
                                ],
                                "state": "RUNNING"
                        ]
                ],
                [
                        "step": [
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:04Z",
                                "index": 0,
                                "ident": "1"
                        ]
                ],
                [
                        "step": [
                                "node": "localhost",
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:04Z",
                                "index": 0,
                                "ident": "1"
                        ]
                ],
                [
                        "step": [
                                "node": "localhost",
                                "meta": [
                                        "failureReason": "NonZeroResultCode"
                                ],
                                "state": "FAILED",
                                "errorMessage": "Result code was 1",
                                "date": "2014-09-05T21:28:04Z",
                                "index": 0,
                                "ident": "1"
                        ]
                ],
                [
                        "step": [
                                "meta": [
                                        "handlerTriggered": "true"
                                ],
                                "state": "RUNNING_HANDLER",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:04Z",
                                "index": 0,
                                "ident": "1e"
                        ]
                ],
                [
                        "step": [
                                "node": "localhost",
                                "meta": null,
                                "state": "RUNNING_HANDLER",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:04Z",
                                "index": 0,
                                "ident": "1e"
                        ]
                ],
                //this state transition should apply to step "1"
                [
                        "subworkflow": [
                                "quell": false,
                                "nodes": [
                                        "alpha1",
                                        "alpha2",
                                        "alpha4"
                                ],
                                "state": "RUNNING",
                                "index": 0,
                                "ident": "1e@node=localhost",
                                "date": "2014-09-05T21:28:04Z"
                        ]
                ],
            ]

        processStateChanges(mutableWorkflowState, changes)

        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.executionState)
        assertEquals(['localhost'], mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.RUNNING_HANDLER, step1.stepState.executionState)
        def WorkflowState subWorkflowState = step1.subWorkflowState
        assertEquals(ExecutionState.RUNNING, subWorkflowState.executionState)

        changes=[
                [
                        "step": [
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:04Z",
                                "index": 0,
                                "ident": "1e@node=localhost/1"
                        ]
                ],
                [
                        "step": [
                                "node": "alpha1",
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:04Z",
                                "index": 0,
                                "ident": "1e@node=localhost/1"
                        ]
                ],
                [
                        "step": [
                                "node": "alpha1",
                                "meta": null,
                                "state": "SUCCEEDED",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:05Z",
                                "index": 0,
                                "ident": "1e@node=localhost/1"
                        ]
                ],
                [
                        "step": [
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:05Z",
                                "index": 0,
                                "ident": "1e@node=localhost/1"
                        ]
                ],
                [
                        "step": [
                                "node": "alpha2",
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:05Z",
                                "index": 0,
                                "ident": "1e@node=localhost/1"
                        ]
                ],
                [
                        "step": [
                                "node": "alpha2",
                                "meta": null,
                                "state": "SUCCEEDED",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:05Z",
                                "index": 0,
                                "ident": "1e@node=localhost/1"
                        ]
                ],
                [
                        "step": [
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:05Z",
                                "index": 0,
                                "ident": "1e@node=localhost/1"
                        ]
                ],
                [
                        "step": [
                                "node": "alpha4",
                                "meta": null,
                                "state": "RUNNING",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:05Z",
                                "index": 0,
                                "ident": "1e@node=localhost/1"
                        ]
                ],
                [
                        "step": [
                                "node": "alpha4",
                                "meta": null,
                                "state": "SUCCEEDED",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:06Z",
                                "index": 0,
                                "ident": "1e@node=localhost/1"
                        ]
                ],
                [
                        "subworkflow": [
                                "quell": false,
                                "nodes": null,
                                "state": "SUCCEEDED",
                                "index": 0,
                                "ident": "1e@node=localhost",
                                "date": "2014-09-05T21:28:06Z"
                        ]
                ],
                [
                        "step": [
                                "node": "localhost",
                                "meta": null,
                                "state": "SUCCEEDED",
                                "errorMessage": null,
                                "date": "2014-09-05T21:28:06Z",
                                "index": 0,
                                "ident": "1e@node=localhost"
                        ]
                ]
        ]

        processStateChanges(mutableWorkflowState,changes)

        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.executionState)
        assertEquals(['localhost'], mutableWorkflowState.nodeSet)

        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.SUCCEEDED, step1.stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, subWorkflowState.executionState)

    }
    static List loadJson(String name){
        def stream = getClassLoader().getResourceAsStream("com/dtolabs/rundeck/app/internal/workflow/"+name)
        return JSON.parse(stream,null)
    }

    /**
     * Multiple parallel nodes in a node-step sub workflow, with a failure node
     */
    public void testSubworkflowParallelNodeFailure() {
        def nodes = (0..<40).collect { 'node-' + it }
        //sub step 1
        def mutableStep11 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep11.nodeStep = true
        def mutableStep12 = new MutableWorkflowStepStateImpl(stepIdentifier(2))
        mutableStep12.nodeStep = true

        //sub workflow
        MutableWorkflowStateImpl sub1 = new MutableWorkflowStateImpl(nodes, 1, [0: mutableStep11,1:mutableStep12],
                StateUtils.stepIdentifier(1), 'a');

        //step 1, both a node step and a subworkflow
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1), sub1)
        mutableStep1.nodeStep = true


        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(nodes, 1, [0: mutableStep1]);

        def changes=loadJson("state1.json")

        processStateChanges(mutableWorkflowState, changes)

        assertEquals(ExecutionState.FAILED, mutableWorkflowState.executionState)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.NODE_MIXED, step1.stepState.executionState)
        def WorkflowState subWorkflowState = step1.subWorkflowState
        assertEquals(ExecutionState.FAILED, subWorkflowState.executionState)

    }
    public void testSubworkflowNodeStepParallelSubworkflow() {
        def nodes = (0..<3).collect { 'node-' + it }
        //sub step 1
        def mutableStep11 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep11.nodeStep = true
        def mutableStep12 = new MutableWorkflowStepStateImpl(stepIdentifier(2))
        mutableStep12.nodeStep = true

        //sub workflow
        MutableWorkflowStateImpl sub1 = new MutableWorkflowStateImpl(nodes, 1, [0: mutableStep11,1:mutableStep12],
                StateUtils.stepIdentifier(1), 'a');

        //step 1, both a node step and a subworkflow
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1), sub1)
        mutableStep1.nodeStep = true

        def node0statetest=22

        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(nodes, 1, [0: mutableStep1]);

        def changes=loadJson("state2.json")
        def changesa=changes[0..<node0statetest]
        processStateChanges(mutableWorkflowState, changesa)

        def changesb=[changes[node0statetest]]
        assertEquals("was: "+changesb,"node-0",changes[node0statetest].step.node)
        assertEquals("1@node=node-0",changes[node0statetest].step.ident)
        assertEquals("SUCCEEDED",changes[node0statetest].step.state)
        assertEquals(0,changes[node0statetest].step.index)
        processStateChanges(mutableWorkflowState, changesb)

        //test state after step 25
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.executionState)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.RUNNING, step1.stepState.executionState)

        //node-0 state should be finished for step 1, and for the parameterized substate
        def node0state1=step1.nodeStateMap.get('node-0')
        assertEquals(ExecutionState.SUCCEEDED,node0state1.executionState)
        def node0stateparam1=step1.parameterizedStateMap.get('node=node-0')
        assertEquals(ExecutionState.RUNNING,node0stateparam1.stepState.executionState)
        def node0stateparam1sub=node0stateparam1.nodeStateMap['node-0']
        assertEquals(ExecutionState.SUCCEEDED, node0stateparam1sub.executionState)

        def changesc=changes[(node0statetest+1)..<changes.size()]

        processStateChanges(mutableWorkflowState, changesc)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)

        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.NODE_MIXED, step1.stepState.executionState)
        def WorkflowState subWorkflowState = step1.subWorkflowState
        assertEquals(ExecutionState.SUCCEEDED, subWorkflowState.executionState)

    }

    protected Date parseDate(date) {
        if(date instanceof Date){
            return date
        }
        def format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        format.parse(date)
    }
    protected ExecutionState parseState(state) {
        if(state instanceof ExecutionState){
            return state
        }
        ExecutionState.valueOf(state)
    }
    protected StepIdentifier parseStepIdent(ident) {
        if(ident instanceof StepIdentifier){
            return ident
        }
        StateUtils.stepIdentifierFromString(ident)
    }

    /**
     * A step which is both a node-step and a sub-workflow step, should finalize correctly
     */
    public void testNodeStepSubWorkflowSuccess() {
        //sub step 1
        def mutableStep11 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep11.nodeStep = true

        //sub workflow
        MutableWorkflowStateImpl sub1 = new MutableWorkflowStateImpl(['c'], 1, [0: mutableStep11], StateUtils.stepIdentifier(1), 'a');

        //step 1, both a node step and a subworkflow
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1), sub1)
        mutableStep1.nodeStep = true


        //top workflow runs on two nodes
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['a','b'], 1, [0: mutableStep1], null, 'a');

        def date = new Date()

        //workflow start
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a','b'])
        //step 1 start
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0,stepStateChange(stepState(ExecutionState.RUNNING)), date)

        //step 1; start on node a
        def node='a'
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.RUNNING), node), date)

        //step1; workflow start
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, false, ExecutionState.RUNNING, date, [node], null)
        //step 1/1 run on node a
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.RUNNING)), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.RUNNING), node), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), node), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)
        //step 1 finish workflow
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, false, ExecutionState.SUCCEEDED, date, [node], null)
        assertEquals(ExecutionState.RUNNING,mutableWorkflowState.stepStates[0].stepState.executionState)
        //setp 1; finish on node
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), node), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)

        node='b'
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.RUNNING)), date)
        //step 1; node b
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.RUNNING), node), date)
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, false, ExecutionState.RUNNING, date, [node], null)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.RUNNING)), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.RUNNING), node), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), node), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, false, ExecutionState.SUCCEEDED, date, [node], null)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), node), date)
        //assertEquals(ExecutionState.RUNNING, mutableWorkflowState.stepStates[0].stepState.executionState)

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, date, null   )

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(['a', 'b'], mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.SUCCEEDED, step1.stepState.executionState)
        assertEquals(['a', 'b'], step1.nodeStepTargets)
        assertNotNull(step1.nodeStateMap['a'])
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['a'].executionState)
        assertNotNull(step1.nodeStateMap['b'])
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['b'].executionState)

        MutableWorkflowState subworkflow = step1.mutableSubWorkflowState

        def step11 = subworkflow.stepStates[0]
        assertStepId(1, step11.stepIdentifier)
        println(step11.nodeStateMap)
        assertEquals(ExecutionState.NODE_PARTIAL_SUCCEEDED, step11.stepState.executionState)
        assertEquals(['c'], step11.nodeStepTargets)
        assertNotNull(step11.nodeStateMap['a'])
        assertEquals(ExecutionState.SUCCEEDED, step11.nodeStateMap['a'].executionState)
        assertNotNull(step11.nodeStateMap['b'])
        assertEquals(ExecutionState.SUCCEEDED, step11.nodeStateMap['b'].executionState)
        assertNotNull(step11.nodeStateMap['c'])
        assertEquals(ExecutionState.NOT_STARTED, step11.nodeStateMap['c'].executionState)

    }
    /**
     * A step which is both a node-step and a sub-workflow step, should finalize correctly
     */
    public void testParameterizedSubworkflowOtherNode() {
        //sub step 1
        def mutableStep11 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep11.nodeStep = true

        //sub workflow
        MutableWorkflowStateImpl sub1 = new MutableWorkflowStateImpl(['c'], 1, [0: mutableStep11], StateUtils.stepIdentifier(1), 'a');

        //step 1, both a node step and a subworkflow, will be invoked parameterized
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1), sub1)
        mutableStep1.nodeStep = true


        //top workflow runs on two nodes
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['a','b'], 1, [0: mutableStep1], null, 'a');

        def date = new Date()

        //workflow start
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a','b'])
        //step 1 start
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0,stepStateChange(stepState(ExecutionState.RUNNING)), date)

        //step 1; start on node a
        def nodeA='a'
        def nodeB='b'
        def serverNode='c'

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.RUNNING), nodeA), date)
        StepContextId ctx1 = stepContextId(1, false, [node: nodeA])
        StepContextId ctx2 = stepContextId(1, false)
        def paramStepId_1A1=stepIdentifier([ctx1, ctx2])

        //step1; workflow start
        mutableWorkflowState.updateSubWorkflowState(mkstepIdentifier(1,nodeA), 0, false, ExecutionState.RUNNING, date, [serverNode], null)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.stepStates[0].stepState.executionState)
        assertNotNull( mutableWorkflowState.mutableStepStates[0].parameterizedStateMap['node=a'])
        assertNull(mutableWorkflowState.mutableStepStates[0].parameterizedStateMap['node=b'])
        assertEquals(ExecutionState.RUNNING,mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').stepState.executionState)
        //step 1/1 run on node a
        mutableWorkflowState.updateStateForStep(paramStepId_1A1, 0, stepStateChange(stepState(ExecutionState.RUNNING)), date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.stepStates[0].stepState.executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1A1, 0, stepStateChange(stepState(ExecutionState.RUNNING), serverNode), date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.stepStates[0].nodeStateMap.get(serverNode).executionState)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].subWorkflowState.stepStates[0].nodeStateMap.get(serverNode).executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1A1, 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), serverNode), date)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.stepStates[0].nodeStateMap.get(serverNode).executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].subWorkflowState.stepStates[0].nodeStateMap.get(serverNode).executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1A1, 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.stepStates[0].stepState.executionState)
        //step 1 finish workflow
        mutableWorkflowState.updateSubWorkflowState(mkstepIdentifier(1, nodeA), 0, false, ExecutionState.SUCCEEDED, date, [nodeA], null)
        assertEquals(ExecutionState.RUNNING,mutableWorkflowState.stepStates[0].stepState.executionState)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').stepState.executionState)

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.RUNNING), nodeB), date)
        StepContextId ctx1B = stepContextId(1, false, [node: nodeB])
        def paramStepId_1B1=stepIdentifier([ctx1B, ctx2])

        //step1; workflow start
        mutableWorkflowState.updateSubWorkflowState(mkstepIdentifier(1,nodeB), 0, false, ExecutionState.RUNNING, date, [serverNode], null)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.stepStates[0].stepState.executionState)
        assertNotNull(mutableWorkflowState.mutableStepStates[0].parameterizedStateMap['node=b'])
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').stepState.executionState)
        //step 1/1 run on node b
        mutableWorkflowState.updateStateForStep(paramStepId_1B1, 0, stepStateChange(stepState(ExecutionState.RUNNING)), date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.stepStates[0].stepState.executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1B1, 0, stepStateChange(stepState(ExecutionState.RUNNING), serverNode), date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.stepStates[0].nodeStateMap.get(serverNode).executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1B1, 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), serverNode), date)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.stepStates[0].nodeStateMap.get(serverNode).executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1B1, 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.stepStates[0].stepState.executionState)
        //step 1 finish workflow
        mutableWorkflowState.updateSubWorkflowState(mkstepIdentifier(1, nodeB), 0, false, ExecutionState.SUCCEEDED, date, [nodeB], null)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.stepStates[0].stepState.executionState)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').stepState.executionState)

        assertEquals(ExecutionState.RUNNING,mutableWorkflowState.stepStates[0].stepState.executionState)

        //finish step 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)

        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.stepStates[0].stepState.executionState)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.executionState)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.executionState)

        //finalize workflow
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, date, null)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(['a', 'b'], mutableWorkflowState.nodeSet)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].stepState.executionState)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.stepStates.get(0).stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.stepStates.get(0).nodeStateMap.get(serverNode).executionState)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.stepStates.get(0).stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.stepStates.get(0).nodeStateMap.get(serverNode).executionState)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].subWorkflowState.stepStates.get(0).nodeStateMap.get(serverNode).executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].subWorkflowState.stepStates.get(0).stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].subWorkflowState.executionState)

    }

    /**
     * subworkflow executes on same parameterized nodes
     */
    public void testParameterizedSubworkflowSameNodes() {
        //sub step 1
        def mutableStep11 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep11.nodeStep = true

        //sub workflow
        MutableWorkflowStateImpl sub1 = new MutableWorkflowStateImpl([], 1, [0: mutableStep11], StateUtils.stepIdentifier(1), 'a');

        //step 1, both a node step and a subworkflow, will be invoked parameterized
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1), sub1)
        mutableStep1.nodeStep = true


        //top workflow runs on two nodes
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['a','b'], 1, [0: mutableStep1], null, 'a');

        def date = new Date()

        //workflow start
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a','b'])
        //step 1 start
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0,stepStateChange(stepState(ExecutionState.RUNNING)), date)

        //step 1; start on node a
        def nodeA='a'
        def nodeB='b'

        def currentNode=nodeA

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.RUNNING), nodeA), date)
        StepContextId ctx1 = stepContextId(1, false, [node: nodeA])
        StepContextId ctx2 = stepContextId(1, false)
        def paramStepId_1A1=stepIdentifier([ctx1, ctx2])

        //step1; workflow start
        mutableWorkflowState.updateSubWorkflowState(mkstepIdentifier(1,nodeA), 0, false, ExecutionState.RUNNING, date, [currentNode], null)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.stepStates[0].stepState.executionState)
        assertNotNull( mutableWorkflowState.mutableStepStates[0].parameterizedStateMap['node=a'])
        assertNull(mutableWorkflowState.mutableStepStates[0].parameterizedStateMap['node=b'])
        assertEquals(ExecutionState.RUNNING,mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').stepState.executionState)
        //step 1/1 run on node a
        mutableWorkflowState.updateStateForStep(paramStepId_1A1, 0, stepStateChange(stepState(ExecutionState.RUNNING)), date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.stepStates[0].stepState.executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1A1, 0, stepStateChange(stepState(ExecutionState.RUNNING), currentNode), date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.stepStates[0].nodeStateMap.get(currentNode).executionState)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].subWorkflowState.stepStates[0].nodeStateMap.get(currentNode).executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1A1, 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), currentNode), date)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.stepStates[0].nodeStateMap.get(currentNode).executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].subWorkflowState.stepStates[0].nodeStateMap.get(currentNode).executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1A1, 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.stepStates[0].stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].subWorkflowState.stepStates[0].stepState.executionState)
        //step 1 finish workflow
        mutableWorkflowState.updateSubWorkflowState(mkstepIdentifier(1, nodeA), 0, false, ExecutionState.SUCCEEDED, date, [nodeA], null)
        assertEquals(ExecutionState.RUNNING,mutableWorkflowState.stepStates[0].stepState.executionState)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').stepState.executionState)


        //switch to nodeB
        currentNode = nodeB

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.RUNNING), nodeB), date)
        StepContextId ctx1B = stepContextId(1, false, [node: nodeB])
        def paramStepId_1B1=stepIdentifier([ctx1B, ctx2])

        //step1; workflow start
        mutableWorkflowState.updateSubWorkflowState(mkstepIdentifier(1,nodeB), 0, false, ExecutionState.RUNNING, date, [currentNode], null)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.stepStates[0].stepState.executionState)
        assertNotNull(mutableWorkflowState.mutableStepStates[0].parameterizedStateMap['node=b'])
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').stepState.executionState)
        //step 1/1 run on node b
        mutableWorkflowState.updateStateForStep(paramStepId_1B1, 0, stepStateChange(stepState(ExecutionState.RUNNING)), date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.stepStates[0].stepState.executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1B1, 0, stepStateChange(stepState(ExecutionState.RUNNING), currentNode), date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.stepStates[0].nodeStateMap.get(currentNode).executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1B1, 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), currentNode), date)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.stepStates[0].nodeStateMap.get(currentNode).executionState)
        mutableWorkflowState.updateStateForStep(paramStepId_1B1, 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.stepStates[0].stepState.executionState)
        //step 1 finish workflow
        mutableWorkflowState.updateSubWorkflowState(mkstepIdentifier(1, nodeB), 0, false, ExecutionState.SUCCEEDED, date, [nodeB], null)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.stepStates[0].stepState.executionState)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').stepState.executionState)

        assertEquals(ExecutionState.RUNNING,mutableWorkflowState.stepStates[0].stepState.executionState)

        //finish step 1
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)

        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.stepStates[0].stepState.executionState)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.executionState)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.executionState)

        //finalize workflow
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, date, null)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(['a', 'b'], mutableWorkflowState.nodeSet)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.stepStates[0].stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=b').subWorkflowState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').stepState.executionState)
        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.mutableStepStates[0].parameterizedStateMap.get('node=a').subWorkflowState.executionState)


    }

    public static StepIdentifier mkstepIdentifier(int id, String nodeparam) {
        return stepIdentifier(stepContextId(id, false, map("node", nodeparam)));
    }

    private static Map<String, String> map(String key, String value) {
        HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        stringStringHashMap.put(key, value);
        return stringStringHashMap;
    }

    /**
     * A step which is both a node-step and a sub-workflow step, abort should finalize node and step
     */
    public void testNodeStepSubWorkflowAbort() {
        //sub step 1
        def mutableStep11 = new MutableWorkflowStepStateImpl(stepIdentifier(1))
        mutableStep11.nodeStep = true

        //sub workflow
        MutableWorkflowStateImpl sub1 = new MutableWorkflowStateImpl(['c'], 1, [0: mutableStep11], StateUtils.stepIdentifier(1), 'a');

        //step 1, both a node step and a subworkflow
        def mutableStep1 = new MutableWorkflowStepStateImpl(stepIdentifier(1), sub1)
        mutableStep1.nodeStep = true


        //top workflow runs on two nodes
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(['a','b'], 1, [0: mutableStep1], null, 'a');

        def date = new Date()

        //workflow start
        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a','b'])
        //step 1 start
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0,stepStateChange(stepState(ExecutionState.RUNNING)), date)

        //step 1; start on node a
        def node='a'
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.RUNNING), node), date)

        //step1; workflow start
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, false, ExecutionState.RUNNING, date, [node], null)
        //step 1/1 run on node a
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.RUNNING)), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.RUNNING), node), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), node), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1, 1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)
        //step 1 finish workflow
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(1), 0, false, ExecutionState.SUCCEEDED, date, [node], null)
        assertEquals(ExecutionState.RUNNING,mutableWorkflowState.stepStates[0].stepState.executionState)
        //setp 1; finish on node
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED), node), date)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1), 0, stepStateChange(stepState(ExecutionState.SUCCEEDED)), date)

        //fail workflow before running on node b
        mutableWorkflowState.updateWorkflowState(ExecutionState.FAILED, date, null)
        assertEquals(ExecutionState.FAILED, mutableWorkflowState.executionState)


        assertEquals(['a', 'b'], mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertStepId(1, step1.stepIdentifier)
        assertEquals(ExecutionState.NODE_PARTIAL_SUCCEEDED, step1.stepState.executionState)
        assertEquals(['a', 'b'], step1.nodeStepTargets)
        assertNotNull(step1.nodeStateMap['a'])
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['a'].executionState)
        assertNotNull(step1.nodeStateMap['b'])
        assertEquals(ExecutionState.NOT_STARTED, step1.nodeStateMap['b'].executionState)

        MutableWorkflowState subworkflow = step1.mutableSubWorkflowState

        def step11 = subworkflow.stepStates[0]
        assertStepId(1, step11.stepIdentifier)
        println(step11.nodeStateMap)
        assertEquals(ExecutionState.NODE_PARTIAL_SUCCEEDED, step11.stepState.executionState)
        assertEquals(['c'], step11.nodeStepTargets)
        assertNotNull(step11.nodeStateMap['a'])
        assertEquals(ExecutionState.SUCCEEDED, step11.nodeStateMap['a'].executionState)
        assertNull(step11.nodeStateMap['b'])
        assertNotNull(step11.nodeStateMap['c'])
        assertEquals(ExecutionState.NOT_STARTED, step11.nodeStateMap['c'].executionState)

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
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(2),0, false, ExecutionState.RUNNING, newdate, ['c','d'], null)
        //start sub steps
        //step 2/1: mixed failure
        mutableWorkflowState.updateStateForStep(stepIdentifier(2,1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(2,1), stepStateChange(stepState(ExecutionState.RUNNING),'c'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(2,1), stepStateChange(stepState(ExecutionState.FAILED),'c'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(2, 1), stepStateChange(stepState(ExecutionState.FAILED)), newdate)
        //nb: node 'd' was not run

        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(2),0, false, ExecutionState.FAILED, newdate, null, null)

        //error handler executed to recover
        mutableWorkflowState.updateStateForStep(stepIdentifier(2), stepStateChange(stepState(ExecutionState.FAILED)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(2,true)), stepStateChange(stepState(ExecutionState.RUNNING_HANDLER)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(stepContextId(2, true)), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)
        assertEquals(ExecutionState.SUCCEEDED,mutableWorkflowState[2].stepState.executionState)

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
        assertEquals(newdate, mutableWorkflowState.updateTime)
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
        assertUpdateExecutionState(ExecutionState.WAITING, null, ExecutionState.WAITING)
        assertUpdateExecutionState(ExecutionState.RUNNING, null, ExecutionState.RUNNING)
        assertUpdateExecutionState(ExecutionState.RUNNING, ExecutionState.RUNNING, ExecutionState.RUNNING)
        assertUpdateExecutionState(ExecutionState.RUNNING, ExecutionState.WAITING, ExecutionState.RUNNING)
        assertUpdateExecutionState(ExecutionState.SUCCEEDED, ExecutionState.WAITING, ExecutionState.SUCCEEDED)
        assertUpdateExecutionState(ExecutionState.FAILED, ExecutionState.WAITING, ExecutionState.FAILED)
        assertUpdateExecutionState(ExecutionState.ABORTED, ExecutionState.WAITING, ExecutionState.ABORTED)
        assertUpdateExecutionState(ExecutionState.SUCCEEDED, ExecutionState.RUNNING, ExecutionState.SUCCEEDED)
        assertUpdateExecutionState(ExecutionState.FAILED, ExecutionState.RUNNING, ExecutionState.FAILED)
        assertUpdateExecutionState(ExecutionState.ABORTED, ExecutionState.RUNNING, ExecutionState.ABORTED)
        assertUpdateExecutionState(ExecutionState.SUCCEEDED, ExecutionState.SUCCEEDED, ExecutionState.RUNNING)
        assertUpdateExecutionState(ExecutionState.FAILED, ExecutionState.FAILED, ExecutionState.RUNNING)
        assertUpdateExecutionState(ExecutionState.ABORTED, ExecutionState.ABORTED, ExecutionState.RUNNING)
        assertUpdateExecutionState(ExecutionState.ABORTED, ExecutionState.ABORTED, ExecutionState.WAITING)
    }
    public void testUpdateStateErrorHandler() {
        //error handler allows FAILED->RUNNING transition
        assertUpdateExecutionState(ExecutionState.RUNNING, ExecutionState.FAILED, ExecutionState.RUNNING, true)
        //other final states do not change
        assertUpdateExecutionState(ExecutionState.SUCCEEDED, ExecutionState.SUCCEEDED, ExecutionState.RUNNING,true)
        assertUpdateExecutionState(ExecutionState.ABORTED, ExecutionState.ABORTED, ExecutionState.RUNNING, true)
    }

    protected void assertUpdateExecutionState(ExecutionState expected, ExecutionState start, ExecutionState update, boolean errorHandler=false) {
        def test = new TestMutableExecutionState()
        test.executionState = start
        MutableWorkflowStateImpl.updateState(null,test, update,errorHandler)
        assertEquals(expected, test.executionState)
    }
    protected void assertUpdateExecutionStateException(ExecutionState start, ExecutionState update, boolean errorHandler=false) {
        def test = new TestMutableExecutionState()
        test.executionState = start
        try {
            MutableWorkflowStateImpl.updateState(null, test, update,errorHandler)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }
        assertEquals(start, test.executionState)
    }

    public void testUpdateStateInvalid() {
        assertUpdateExecutionStateException(ExecutionState.RUNNING, ExecutionState.WAITING)
        assertUpdateExecutionStateException(ExecutionState.SUCCEEDED, null)
        assertUpdateExecutionStateException(ExecutionState.ABORTED, null)
        assertUpdateExecutionStateException(ExecutionState.FAILED, null)
        assertUpdateExecutionStateException(ExecutionState.RUNNING, null)
    }
}
