package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifierImpl
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateChangeImpl
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateImpl
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
        assertNull(mutableWorkflowState.getExecutionState());
        assertEquals([] as Set, mutableWorkflowState.getNodeSet());
        assertEquals([] as Set, mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertNull(mutableWorkflowState.getTimestamp());
        (0..1).each{i->
            assertEquals(ExecutionState.WAITING,mutableWorkflowState.stepStates[i].stepState.executionState)
        }
    }

    public void testUpdateWorkflowStep() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date()

        mutableWorkflowState.updateStateForStep(stepIdentifier(1), stepStateChange(stepState(ExecutionState.RUNNING)),
                date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.getExecutionState());
        assertEquals([] as Set, mutableWorkflowState.getNodeSet());
        assertEquals([] as Set, mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getTimestamp());

        def WorkflowStepState state = mutableWorkflowState.getStepStates()[0]
        assertEquals([1], state.stepIdentifier.context)
        assertEquals(false, state.hasSubWorkflow())
        assertEquals(ExecutionState.RUNNING, state.stepState.executionState)
        assertEquals(null, state.stepState.errorMessage)
        assertEquals(null, state.stepState.metadata)
        assertEquals(null, state.subWorkflowState)
        assertEquals([:], state.nodeStateMap)
    }
    public void testUpdateWorkflowSubStep() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date()

        mutableWorkflowState.updateStateForStep(stepIdentifier(1,1), stepStateChange(stepState(ExecutionState.RUNNING)),
                date)
        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.getExecutionState());
        assertEquals([] as Set, mutableWorkflowState.getNodeSet());
        assertEquals([] as Set, mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getTimestamp());

        def WorkflowStepState state = mutableWorkflowState.getStepStates()[0]
        assertEquals([1], state.stepIdentifier.context)
        assertEquals(true, state.hasSubWorkflow())
        assertEquals(ExecutionState.RUNNING, state.stepState.executionState)
        assertEquals(null, state.stepState.errorMessage)
        assertEquals(null, state.stepState.metadata)
        assertEquals([:], state.nodeStateMap)

        def WorkflowState subWorkflowState = state.subWorkflowState
        assertEquals(ExecutionState.RUNNING, subWorkflowState.executionState)
        assertEquals([] as Set, subWorkflowState.nodeSet)
        assertEquals(1, subWorkflowState.getStepStates().size())
        assertEquals(1, subWorkflowState.getStepCount())
        assertEquals(date, subWorkflowState.getTimestamp())
        def WorkflowStepState subStepState1 = subWorkflowState.getStepStates()[0]
        assertEquals([1], subStepState1.stepIdentifier.context)
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
        assertEquals([] as Set, mutableWorkflowState.getNodeSet());
        assertEquals([] as Set, mutableWorkflowState.getMutableNodeSet());
        assertEquals(2, mutableWorkflowState.getStepStates().size());
        assertEquals(2, mutableWorkflowState.getStepCount());
        assertEquals(date, mutableWorkflowState.getTimestamp());

        def WorkflowStepState state = mutableWorkflowState.getStepStates()[0]
        assertEquals([1], state.stepIdentifier.context)
        assertEquals(false, state.hasSubWorkflow())
        assertEquals(ExecutionState.RUNNING, state.stepState.executionState)
        assertEquals(null, state.stepState.errorMessage)
        assertEquals(null, state.stepState.metadata)
        assertEquals(null, state.subWorkflowState)
        assertEquals(1, state.nodeStateMap.size())
        StepState nodestate = state.nodeStateMap.testnode
        assertEquals(ExecutionState.RUNNING, nodestate.executionState)
        assertEquals(null, nodestate.metadata)
        assertEquals(null, nodestate.errorMessage)
    }

    public void testUpdateWorkflow() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a', 'b'] as Set)

        assertEquals(ExecutionState.RUNNING, mutableWorkflowState.executionState)
        assertEquals(date, mutableWorkflowState.timestamp)
        assertEquals(['a', 'b'] as Set, mutableWorkflowState.nodeSet)

    }

    public void testUpdateWorkflowFinish() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a', 'b'] as Set)
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a', 'b'] as Set, mutableWorkflowState.nodeSet)

    }

    public void testUpdateWorkflowInvalid() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a', 'b'] as Set)
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)
        try {
            mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, newdate, null)
            fail("shouldn't update state")
        } catch (IllegalStateException e) {
            assertNotNull(e)
        }

        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a', 'b'] as Set, mutableWorkflowState.nodeSet)
    }

    public void testUpdateSubWorkflowResolveState() {
        MutableWorkflowStateImpl mutableWorkflowState = new MutableWorkflowStateImpl(null, 2);
        def date = new Date(123)
        def newdate = new Date()

        mutableWorkflowState.updateWorkflowState(ExecutionState.RUNNING, date, ['a', 'b'] as Set)

        //step 1: partial success
        mutableWorkflowState.updateStateForStep(stepIdentifier(1),stepStateChange(stepState(ExecutionState.RUNNING)),newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1),stepStateChange(stepState(ExecutionState.RUNNING),'a'),newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(1),stepStateChange(stepState(ExecutionState.SUCCEEDED),'a'),newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(2), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)

        //step 2: sub workflow
        //start subworkflow
        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(2), ExecutionState.RUNNING, newdate, ['c','d'] as Set)
        //start sub steps
        //step 2/1: mixed failure
        mutableWorkflowState.updateStateForStep(stepIdentifier(2,1), stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(2,1), stepStateChange(stepState(ExecutionState.RUNNING),'c'), newdate)
        mutableWorkflowState.updateStateForStep(stepIdentifier(2,1), stepStateChange(stepState(ExecutionState.FAILED),'c'), newdate)
        //nb: node 'd' was not run

        mutableWorkflowState.updateSubWorkflowState(stepIdentifier(2), ExecutionState.FAILED, newdate, null)

        //error handler executed to recover
        mutableWorkflowState.updateStateForStep(stepIdentifier(2), stepStateChange(stepState(ExecutionState.SUCCEEDED)), newdate)

        //step 3:  mixed state node results
        stepIdentifier(3).with {
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING)), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING), 'a'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.SUCCEEDED), 'a'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.RUNNING), 'b'), newdate)
            mutableWorkflowState.updateStateForStep(it, stepStateChange(stepState(ExecutionState.FAILED), 'b'), newdate)
        }

        //step 4: nodes not started
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

        //finish
        mutableWorkflowState.updateWorkflowState(ExecutionState.SUCCEEDED, newdate, null)


        assertEquals(ExecutionState.SUCCEEDED, mutableWorkflowState.executionState)
        assertEquals(newdate, mutableWorkflowState.timestamp)
        assertEquals(['a', 'b'] as Set, mutableWorkflowState.nodeSet)

        def step1 = mutableWorkflowState[1]
        assertEquals([1], step1.stepIdentifier.context)
        assertEquals(ExecutionState.NODE_PARTIAL_SUCCEEDED, step1.stepState.executionState)
        assertEquals(['a','b'] as Set, step1.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step1.nodeStateMap['a'].executionState)
        assertNotNull(step1.nodeStateMap['b'])
        assertEquals(ExecutionState.NOT_STARTED, step1.nodeStateMap['b'].executionState)

        def step2 = mutableWorkflowState[2]
        assertEquals([2], step2.stepIdentifier.context)
        assertEquals(ExecutionState.SUCCEEDED, step2.stepState.executionState)
        assertNotNull(step2.subWorkflowState)
        assertEquals([1], step2.subWorkflowState[1].stepIdentifier.context)
        assertEquals(ExecutionState.FAILED, step2.subWorkflowState[1].stepState.executionState)
        assertEquals(['c', 'd'] as Set, step2.subWorkflowState[1].nodeStepTargets)
        assertEquals(ExecutionState.FAILED, step2.subWorkflowState[1].nodeStateMap['c'].executionState)
        assertEquals(ExecutionState.NOT_STARTED, step2.subWorkflowState[1].nodeStateMap['d'].executionState)

        def step3 = mutableWorkflowState[3]
        assertEquals([3], step3.stepIdentifier.context)
        assertEquals(ExecutionState.NODE_MIXED, step3.stepState.executionState)
        assertEquals(['a', 'b'] as Set, step3.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step3.nodeStateMap['a'].executionState)
        assertNotNull(step3.nodeStateMap['b'])
        assertEquals(ExecutionState.FAILED, step3.nodeStateMap['b'].executionState)

        def step4 = mutableWorkflowState[4]
        assertEquals([4], step4.stepIdentifier.context)
        assertEquals(ExecutionState.NOT_STARTED, step4.stepState.executionState)
        assertEquals(['a', 'b'] as Set, step4.nodeStepTargets)
        assertEquals(ExecutionState.NOT_STARTED, step4.nodeStateMap['a'].executionState)
        assertNotNull(step4.nodeStateMap['b'])
        assertEquals(ExecutionState.NOT_STARTED, step4.nodeStateMap['b'].executionState)

        def step5 = mutableWorkflowState[5]
        assertEquals([5], step5.stepIdentifier.context)
        assertEquals(ExecutionState.ABORTED, step5.stepState.executionState)
        assertEquals(['a', 'b'] as Set, step5.nodeStepTargets)
        assertEquals(ExecutionState.ABORTED, step5.nodeStateMap['a'].executionState)
        assertNotNull(step5.nodeStateMap['b'])
        assertEquals(ExecutionState.ABORTED, step5.nodeStateMap['b'].executionState)

        def step6 = mutableWorkflowState[6]
        assertEquals([6], step6.stepIdentifier.context)
        assertEquals(ExecutionState.FAILED, step6.stepState.executionState)
        assertEquals(['a', 'b'] as Set, step6.nodeStepTargets)
        assertEquals(ExecutionState.FAILED, step6.nodeStateMap['a'].executionState)
        assertNotNull(step6.nodeStateMap['b'])
        assertEquals(ExecutionState.FAILED, step6.nodeStateMap['b'].executionState)

        def step7 = mutableWorkflowState[7]
        assertEquals([7], step7.stepIdentifier.context)
        assertEquals(ExecutionState.SUCCEEDED, step7.stepState.executionState)
        assertEquals(['a', 'b'] as Set, step7.nodeStepTargets)
        assertEquals(ExecutionState.SUCCEEDED, step7.nodeStateMap['a'].executionState)
        assertNotNull(step7.nodeStateMap['b'])
        assertEquals(ExecutionState.SUCCEEDED, step7.nodeStateMap['b'].executionState)

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
