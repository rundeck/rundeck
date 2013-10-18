package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
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

    public void testUpdateStateNormal() {
        assertEquals(ExecutionState.WAITING, MutableWorkflowStateImpl.updateState(null, ExecutionState.WAITING))
        assertEquals(ExecutionState.RUNNING, MutableWorkflowStateImpl.updateState(null, ExecutionState.RUNNING))
        assertEquals(ExecutionState.RUNNING, MutableWorkflowStateImpl.updateState(ExecutionState.WAITING, ExecutionState.RUNNING))
        assertEquals(ExecutionState.SUCCEEDED, MutableWorkflowStateImpl.updateState(ExecutionState.RUNNING, ExecutionState.SUCCEEDED))
        assertEquals(ExecutionState.FAILED, MutableWorkflowStateImpl.updateState(ExecutionState.RUNNING, ExecutionState.FAILED))
        assertEquals(ExecutionState.ABORTED, MutableWorkflowStateImpl.updateState(ExecutionState.RUNNING, ExecutionState.ABORTED))
    }
    public void testUpdateStateInvalid() {
        try {
            MutableWorkflowStateImpl.updateState(ExecutionState.RUNNING, ExecutionState.RUNNING)
            fail("Should not succeed")
        } catch (IllegalStateException e) {
        }
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
