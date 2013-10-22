package com.dtolabs.rundeck.core.execution.workflow.state;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import junit.framework.TestCase;

import java.util.*;

/**
 * $INTERFACE is ... User: greg Date: 10/22/13 Time: 10:08 AM
 */
public class WorkflowExecutionStateListenerAdapterTest extends TestCase {

    private WorkflowExecutionStateListenerAdapterTest.testListener1 testListener1;
    private WorkflowExecutionStateListenerAdapter test;
    private StepExecutionItem testitem;

    static class testListener1 implements WorkflowStateListener {
        List events = new ArrayList();

        @Override
        public void stepStateChanged(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
            events.add(new Object[]{"step", identifier, stepStateChange, timestamp});
        }

        @Override
        public void workflowExecutionStateChanged(ExecutionState executionState, Date timestamp, Set<String> nodeSet) {
            events.add(new Object[]{"workflow", executionState, timestamp, nodeSet});
        }

        @Override
        public void subWorkflowExecutionStateChanged(StepIdentifier identifier, ExecutionState executionState, Date
                timestamp, Set<String> nodeSet) {
            events.add(new Object[]{"subworkflow", identifier, executionState, timestamp, nodeSet});
        }
    }

    static class wfresult implements WorkflowExecutionResult {

        List<StepExecutionResult> resultSet;
        Map<String, Collection<StepExecutionResult>> nodeFailures;
        Map<Integer, StepExecutionResult> stepFailures;
        Exception exception;
        boolean success;

        static WorkflowExecutionResult forSuccess(boolean success) {
            wfresult wfresult = new WorkflowExecutionStateListenerAdapterTest.wfresult();
            wfresult.success = success;
            return wfresult;
        }

        @Override
        public List<StepExecutionResult> getResultSet() {
            return resultSet;
        }

        @Override
        public Map<String, Collection<StepExecutionResult>> getNodeFailures() {
            return nodeFailures;
        }

        @Override
        public Map<Integer, StepExecutionResult> getStepFailures() {
            return stepFailures;
        }

        @Override
        public Exception getException() {
            return exception;
        }

        @Override
        public boolean isSuccess() {
            return success;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testListener1 = new WorkflowExecutionStateListenerAdapterTest.testListener1();
        List<WorkflowStateListener> testListener1s = Arrays.asList((WorkflowStateListener) testListener1);
        test = new WorkflowExecutionStateListenerAdapter(testListener1s);

        testitem = new StepExecutionItem() {
            @Override
            public String getType() {
                return "test1";
            }
        };
    }

    public void testEventsBeginWorkflowExecution() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        nodes.put("test1", new NodeEntryImpl("test1"));
        NodeSetImpl nodeSet = new NodeSetImpl(nodes);
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(nodeSet).build(), null);

        assertEquals(1, testListener1.events.size());
        Object[] o = (Object[]) testListener1.events.get(0);
        assertWorkflowStateEvent(o, ExecutionState.RUNNING, "test1");
    }

    public void testEventsFinishWorkflowExecution() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        nodes.put("test1", new NodeEntryImpl("test1"));
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);
        test.finishWorkflowExecution(wfresult.forSuccess(true), ExecutionContextImpl.builder().nodes(new NodeSetImpl
                (nodes)).build(), null);

        assertEquals(2, testListener1.events.size());
        Object[] o = (Object[]) testListener1.events.get(0);
        assertWorkflowStateEvent(o, ExecutionState.RUNNING, "test1");
        Object[] o2 = (Object[]) testListener1.events.get(1);
        assertWorkflowStateEvent(o2, ExecutionState.SUCCEEDED);
    }

    public void testEventsBeginWorkflowItem() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        nodes.put("test1", new NodeEntryImpl("test1"));
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);

        test.beginWorkflowItem(1, testitem);

        assertEquals(2, testListener1.events.size());
        Object[] o = (Object[]) testListener1.events.get(0);
        assertWorkflowStateEvent(o, ExecutionState.RUNNING, "test1");
        Object[] o2 = (Object[]) testListener1.events.get(1);
        assertStepStateEvent(o2, ExecutionState.RUNNING, null,1);
    }
    public void testEventsFinishWorkflowItem() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        nodes.put("test1", new NodeEntryImpl("test1"));
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);

        test.beginWorkflowItem(1, testitem);
        test.finishWorkflowItem(1, testitem, true);

        assertEquals(3, testListener1.events.size());
        assertWorkflowStateEvent((Object[]) testListener1.events.get(0), ExecutionState.RUNNING, "test1");
        assertStepStateEvent((Object[]) testListener1.events.get(1), ExecutionState.RUNNING, null, 1);
        assertStepStateEvent((Object[]) testListener1.events.get(2), ExecutionState.SUCCEEDED, null,1);
    }
    public void testEventsBeginSubStepItem() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        NodeEntryImpl node1 = new NodeEntryImpl("test1");
        nodes.put("test1", node1);
        HashMap<String, INodeEntry> nodes2 = new HashMap<String, INodeEntry>();
        NodeEntryImpl node2 = new NodeEntryImpl("test2");
        nodes2.put("test2", node2);
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);

        test.beginWorkflowItem(1, testitem);
        //sub workflow
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes2)).build(), null);

        test.beginWorkflowItem(1, testitem);
        assertEquals(4, testListener1.events.size());
        assertWorkflowStateEvent((Object[]) testListener1.events.get(0), ExecutionState.RUNNING, "test1");
        assertStepStateEvent((Object[]) testListener1.events.get(1), ExecutionState.RUNNING, null, 1);
        assertSubWorkflowStateEvent((Object[]) testListener1.events.get(2), ExecutionState.RUNNING, new int[]{1}, "test2");
        assertStepStateEvent((Object[]) testListener1.events.get(3), ExecutionState.RUNNING, null, 1, 1);
    }
    public void testEventsFinishSubStepItem() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        NodeEntryImpl node1 = new NodeEntryImpl("test1");
        nodes.put("test1", node1);
        HashMap<String, INodeEntry> nodes2 = new HashMap<String, INodeEntry>();
        NodeEntryImpl node2 = new NodeEntryImpl("test2");
        nodes2.put("test2", node2);
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);

        test.beginWorkflowItem(1, testitem);
        //sub workflow
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes2)).build(), null);

        test.beginWorkflowItem(1, testitem);
        test.finishWorkflowItem(1, testitem, true);

        assertEquals(5, testListener1.events.size());
        assertWorkflowStateEvent((Object[]) testListener1.events.get(0), ExecutionState.RUNNING, "test1");
        assertStepStateEvent((Object[]) testListener1.events.get(1), ExecutionState.RUNNING, null, 1);
        assertSubWorkflowStateEvent((Object[]) testListener1.events.get(2), ExecutionState.RUNNING, new int[]{1},
                "test2");
        assertStepStateEvent((Object[]) testListener1.events.get(3), ExecutionState.RUNNING, null, 1, 1);
        assertStepStateEvent((Object[]) testListener1.events.get(4), ExecutionState.SUCCEEDED, null, 1, 1);
    }
    public void testEventsFinishSubFlow() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        NodeEntryImpl node1 = new NodeEntryImpl("test1");
        nodes.put("test1", node1);
        HashMap<String, INodeEntry> nodes2 = new HashMap<String, INodeEntry>();
        NodeEntryImpl node2 = new NodeEntryImpl("test2");
        nodes2.put("test2", node2);
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);

        test.beginWorkflowItem(1, testitem);
        //sub workflow
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes2)).build(), null);

        test.beginWorkflowItem(1, testitem);
        test.finishWorkflowItem(1, testitem, true);
        test.finishWorkflowExecution(wfresult.forSuccess(true), null, null);
        test.finishWorkflowItem(1, testitem, true);
        test.finishWorkflowExecution(wfresult.forSuccess(true), null, null);

        assertEquals(8, testListener1.events.size());
        assertWorkflowStateEvent((Object[]) testListener1.events.get(0), ExecutionState.RUNNING, "test1");
        assertStepStateEvent((Object[]) testListener1.events.get(1), ExecutionState.RUNNING, null, 1);
        assertSubWorkflowStateEvent((Object[]) testListener1.events.get(2), ExecutionState.RUNNING, new int[]{1},
                "test2");
        assertStepStateEvent((Object[]) testListener1.events.get(3), ExecutionState.RUNNING, null, 1, 1);
        assertStepStateEvent((Object[]) testListener1.events.get(4), ExecutionState.SUCCEEDED, null, 1, 1);
        assertSubWorkflowStateEvent((Object[]) testListener1.events.get(5), ExecutionState.SUCCEEDED, new int[]{1});
        assertStepStateEvent((Object[]) testListener1.events.get(6), ExecutionState.SUCCEEDED, null, 1);
        assertWorkflowStateEvent((Object[]) testListener1.events.get(7), ExecutionState.SUCCEEDED);
    }
    public void testEventsBeginNodeStepItem() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        NodeEntryImpl node1 = new NodeEntryImpl("test1");
        nodes.put("test1", node1);
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);

        test.beginWorkflowItem(1, testitem);
        test.beginExecuteNodeStep(null, null, node1);

        assertEquals(3, testListener1.events.size());
        assertWorkflowStateEvent((Object[]) testListener1.events.get(0), ExecutionState.RUNNING, "test1");
        assertStepStateEvent((Object[]) testListener1.events.get(1), ExecutionState.RUNNING, null,1);
        assertStepStateEvent((Object[]) testListener1.events.get(2), ExecutionState.RUNNING, "test1",1);
    }

    private void assertStepStateEvent(Object[] o, ExecutionState running, String nodename, int... ctxid) {
        assertEquals("step", o[0]);
        StepIdentifier ident = (StepIdentifier)o[1];
        assertIdentifier(ident, ctxid);
        assertTrue(o[2] instanceof StepStateChange);
        StepStateChange change=(StepStateChange)o[2];
        if(null!=nodename){
            assertEquals(true,change.isNodeState());
            assertEquals(nodename,change.getNodeName());
        }else{
            assertFalse(change.isNodeState());
        }
        assertEquals(running, change.getStepState().getExecutionState());
        assertEquals(null, change.getStepState().getErrorMessage());
        assertEquals(null, change.getStepState().getMetadata());


        assertTrue(o[3] instanceof Date);

    }

    private void assertIdentifier(StepIdentifier ident, int[] ctxid) {
        assertEquals(ctxid.length, ident.getContext().size());
        for (int i =0;i<ctxid.length;i++) {
            assertEquals(ctxid[i], (int)ident.getContext().get(i));
        }
    }

    private void assertWorkflowStateEvent(Object[] o, ExecutionState running, String... nodes) {
        assertEquals("workflow", o[0]);
        assertEquals(running, o[1]);
        assertTrue(o[2] instanceof Date);
        if (nodes.length <= 0) {
            assertEquals(null, o[3]);
        } else {
            assertTrue(o[3] instanceof Set);
            Set<String> names = (Set<String>) o[3];
            assertEquals(nodes.length, names.size());
            for (String node : nodes) {
                assertTrue(names.contains(node));
            }
        }
    }
    private void assertSubWorkflowStateEvent(Object[] o, ExecutionState running, int[] ctxid,String... nodes) {
        int i=0;
        assertEquals("subworkflow", o[i++]);
        Object ident = o[i++];
        assertTrue(ident instanceof StepIdentifier);
        assertIdentifier((StepIdentifier)ident,ctxid);
        assertEquals(running, o[i++]);
        assertTrue(o[i++] instanceof Date);
        if (nodes.length <= 0) {
            assertEquals(null, o[i++]);
        } else {
            Object o1 = o[i++];
            assertTrue(o1 instanceof Set);
            Set<String> names = (Set<String>) o1;
            assertEquals(nodes.length, names.size());
            for (String node : nodes) {
                assertTrue("expected "+node+" in names: "+names,names.contains(node));
            }
        }
    }
}
