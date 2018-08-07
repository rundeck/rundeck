package com.dtolabs.rundeck.core.execution.workflow.state

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import spock.lang.Specification

class WorkflowExecutionStateListenerAdapterSpec extends Specification {

    static class TestListener1 implements WorkflowStateListener {
        List<Map> events = []

        @Override
        public void stepStateChanged(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
            events.add([type: "step", id: identifier, change: stepStateChange, timestamp: timestamp]);
        }

        @Override
        public void workflowExecutionStateChanged(ExecutionState executionState, Date timestamp, List<String> nodeSet) {
            events.add([type: "workflow", state: executionState, timestamp: timestamp, nodeSet: nodeSet]);
        }

        @Override
        public void subWorkflowExecutionStateChanged(
            StepIdentifier identifier, ExecutionState executionState, Date
                timestamp, List<String> nodeSet
        ) {
            events.add(
                [type: "subworkflow", id: identifier, state: executionState, timestamp: timestamp, nodeSet: nodeSet]
            );
        }
    }

    static class TestJobRefItem implements NodeStepExecutionItem {

        String nodeStepType = "non-node-dispatch"

        String type = "non-node-dispatch"

        String label = null

        boolean nodeStep

    }

    def "finish job ref workflow item"() {
        given:
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        nodes.put("test1", new NodeEntryImpl("test1"));
        def testListener1 = new TestListener1()


        List<WorkflowStateListener> testListener1s = Arrays.asList((WorkflowStateListener) testListener1);
        def test = new WorkflowExecutionStateListenerAdapter(testListener1s);

        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);
        TestJobRefItem testJobRefItem = new TestJobRefItem(nodeStep: isNodeStep);
        test.beginWorkflowItem(1, testJobRefItem);

        when:
        test.finishWorkflowItem(1, testJobRefItem, new StepExecutionResultImpl());
        then:
        (size) == testListener1.events.size()
        def evt1 = testListener1.events.get(0)
        'workflow' == evt1.type
        ExecutionState.RUNNING == evt1.state

        def evt2 = testListener1.events.get(1)
        evt2.type == 'step'
        evt2.change.stepState.executionState == ExecutionState.RUNNING

        if (size > 2) {
            def evt3 = testListener1.events.get(2)
            evt3.type == 'step'
            evt3.change.stepState.executionState == ExecutionState.SUCCEEDED
        }

        where:
        isNodeStep | size
        false      | 3
        true       | 2

    }
}
