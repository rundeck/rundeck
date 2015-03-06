package com.dtolabs.rundeck.core.execution.workflow.state;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.ControlBehavior;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl;
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
        public void workflowExecutionStateChanged(ExecutionState executionState, Date timestamp, List<String> nodeSet) {
            events.add(new Object[]{"workflow", executionState, timestamp, nodeSet});
        }

        @Override
        public void subWorkflowExecutionStateChanged(StepIdentifier identifier, ExecutionState executionState, Date
                timestamp, List<String> nodeSet) {
            events.add(new Object[]{"subworkflow", identifier, executionState, timestamp, nodeSet});
        }
    }

    static class wfresult implements WorkflowExecutionResult {

        List<StepExecutionResult> resultSet;
        Map<String, Collection<StepExecutionResult>> nodeFailures;
        Map<Integer, StepExecutionResult> stepFailures;
        Exception exception;
        boolean success;
        private String statusString;
        private ControlBehavior controlBehavior;

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

        @Override
        public String getStatusString() {
            return statusString;
        }

        @Override
        public ControlBehavior getControlBehavior() {
            return controlBehavior;
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

    public void testFinishExecuteNodeStepNullResult() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        NodeEntryImpl node1 = new NodeEntryImpl("test1");
        nodes.put("test1", node1);
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);

        test.beginWorkflowItem(1, testitem);
        test.beginExecuteNodeStep(null, null, node1);
        testReason reason1 = new testReason("Unknown");
        //simulate exception thrown with null result
        test.finishExecuteNodeStep(null, null, null, node1);

        assertEquals(4, testListener1.events.size());
        assertWorkflowStateEvent((Object[]) testListener1.events.get(0), ExecutionState.RUNNING, "test1");
        assertStepStateEvent((Object[]) testListener1.events.get(1), ExecutionState.RUNNING, null, 1);
        assertStepStateEvent((Object[]) testListener1.events.get(2), ExecutionState.RUNNING, "test1", 1);
        assertStepStateEvent((Object[]) testListener1.events.get(3), ExecutionState.FAILED, "test1", null,
                reason1.map(), 1);
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
        test.finishWorkflowItem(1, testitem, successResult());

        assertEquals(3, testListener1.events.size());
        assertWorkflowStateEvent((Object[]) testListener1.events.get(0), ExecutionState.RUNNING, "test1");
        assertStepStateEvent((Object[]) testListener1.events.get(1), ExecutionState.RUNNING, null, 1);
        assertStepStateEvent((Object[]) testListener1.events.get(2), ExecutionState.SUCCEEDED, null,1);
    }

    private StepExecutionResult successResult() {
        return new StepExecutionResultImpl();
    }


    private NodeStepResult successNodeResult(INodeEntry node) {
        return new NodeStepResultImpl(node);
    }
    class testReason implements FailureReason{
        String reason;

        testReason(String reason) {
            this.reason = reason;
        }

        @Override
        public String toString() {
            return reason;
        }
        public Map map() {
            HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
            stringStringHashMap.put("failureReason", reason);
            return stringStringHashMap;
        }
    }

    public void testEventsWorkflowItemErrorHandler() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        NodeEntryImpl node1 = new NodeEntryImpl("test1");
        nodes.put("test1", node1);
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);

        test.beginWorkflowItem(1, testitem);
        test.beginExecuteNodeStep(null, null, node1);
        testReason reason1 = new testReason("reason1");
        test.finishExecuteNodeStep(failureNodeResult(node1,"blah", reason1), null, null, node1);
        test.beginWorkflowItemErrorHandler(1, testitem);
        test.beginExecuteNodeStep(null, null, node1);
        test.finishExecuteNodeStep(successNodeResult(node1), null, null, node1);
        test.finishWorkflowItemErrorHandler(1, testitem, successResult());
        test.finishWorkflowItem(1, testitem, successResult());

        assertEquals(8, testListener1.events.size());
        assertWorkflowStateEvent((Object[]) testListener1.events.get(0), ExecutionState.RUNNING, "test1");
        assertStepStateEvent((Object[]) testListener1.events.get(1), ExecutionState.RUNNING, null, 1);
        assertStepStateEvent((Object[]) testListener1.events.get(2), ExecutionState.RUNNING, "test1", 1);
        assertStepStateEvent((Object[]) testListener1.events.get(3), ExecutionState.FAILED,"test1","blah", reason1.map(), 1);
        HashMap<String, String> handlermap = new HashMap<String, String>();
        handlermap.put("handlerTriggered", "true");
        assertStepStateEvent((Object[]) testListener1.events.get(4), ExecutionState.RUNNING_HANDLER, null, null, handlermap,
                StateUtils.stepContextId(1, true));
        assertStepStateEvent((Object[]) testListener1.events.get(5), ExecutionState.RUNNING_HANDLER, "test1", null,
                null, StateUtils.stepContextId(1, true));
        assertStepStateEvent((Object[]) testListener1.events.get(6), ExecutionState.SUCCEEDED, "test1", null,
                null, StateUtils.stepContextId(1, true));
        assertStepStateEvent((Object[]) testListener1.events.get(7), ExecutionState.SUCCEEDED, null, null, null, StateUtils.stepContextId(1, true));
    }

    public void testEventsSubflowWithErrorHandler() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        NodeEntryImpl node1 = new NodeEntryImpl("test1");
        nodes.put("test1", node1);
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);
        test.beginWorkflowItem(1, testitem);
        //sub workflow
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);

        test.beginWorkflowItem(1, testitem);
        test.beginExecuteNodeStep(null, null, node1);
        testReason reason1 = new testReason("reason1");
        test.finishExecuteNodeStep(failureNodeResult(node1,"blah", reason1), null, null, node1);
        test.beginWorkflowItemErrorHandler(1, testitem);
        test.beginExecuteNodeStep(null, null, node1);
        test.finishExecuteNodeStep(successNodeResult(node1), null, null, node1);
        test.finishWorkflowItemErrorHandler(1, testitem, successResult());
        test.finishWorkflowItem(1, testitem, successResult());

        test.finishWorkflowExecution(wfresult.forSuccess(true), null, null);
        test.finishWorkflowItem(1, testitem, successResult());
        test.finishWorkflowExecution(wfresult.forSuccess(true), null, null);

        assertEquals(13, testListener1.events.size());
        int x=0;
        assertWorkflowStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.RUNNING, "test1");
        assertStepStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.RUNNING, null, 1);
        assertSubWorkflowStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.RUNNING,new int[]{1},"test1");

        assertStepStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.RUNNING, null, 1,1);
        assertStepStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.RUNNING, "test1", 1,1);
        assertStepStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.FAILED,"test1","blah", reason1.map(), 1,1);
        HashMap<String, String> handlermap = new HashMap<String, String>();
        handlermap.put("handlerTriggered", "true");
        assertStepStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.RUNNING_HANDLER, null, null, handlermap,
                StateUtils.stepContextId(1,false),StateUtils.stepContextId(1, true));
        assertStepStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.RUNNING_HANDLER, "test1", null,
                null, StateUtils.stepContextId(1, false), StateUtils.stepContextId(1, true));
        assertStepStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.SUCCEEDED, "test1", null,
                null, StateUtils.stepContextId(1, false), StateUtils.stepContextId(1, true));
        assertStepStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.SUCCEEDED, null, null, null,
                StateUtils.stepContextId(1, false), StateUtils.stepContextId(1, true));
        assertSubWorkflowStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.SUCCEEDED, new int[]{1});
        assertStepStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.SUCCEEDED, null, 1);
        assertWorkflowStateEvent((Object[]) testListener1.events.get(x++), ExecutionState.SUCCEEDED);
    }

    private NodeStepResult failureNodeResult(INodeEntry node1, String message, FailureReason failureReason) {
        return new NodeStepResultImpl(null, failureReason, message, node1);
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
        test.finishWorkflowItem(1, testitem, successResult());

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
        test.finishWorkflowItem(1, testitem, successResult());
        test.finishWorkflowExecution(wfresult.forSuccess(true), null, null);
        test.finishWorkflowItem(1, testitem, successResult());
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

    public void testEventsFinishNodeStepItem() {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();
        NodeEntryImpl node1 = new NodeEntryImpl("test1");
        nodes.put("test1", node1);
        test.beginWorkflowExecution(ExecutionContextImpl.builder().nodes(new NodeSetImpl(nodes)).build(), null);

        test.beginWorkflowItem(1, testitem);
        test.beginExecuteNodeStep(null, null, node1);
        test.finishExecuteNodeStep(successNodeResult(node1), null, null, node1);

        assertEquals(4, testListener1.events.size());
        assertWorkflowStateEvent((Object[]) testListener1.events.get(0), ExecutionState.RUNNING, "test1");
        assertStepStateEvent((Object[]) testListener1.events.get(1), ExecutionState.RUNNING, null, 1);
        assertStepStateEvent((Object[]) testListener1.events.get(2), ExecutionState.RUNNING, "test1",1);
        assertStepStateEvent((Object[]) testListener1.events.get(3), ExecutionState.SUCCEEDED, "test1",1);
    }

    private void assertStepStateEvent(Object[] o, ExecutionState running, String nodename, int... ctxid) {
        assertStepStateEvent(o, running, nodename, null, null, ctxid);
    }
    private void assertStepStateEvent(Object[] o, ExecutionState running, String nodename, String errorMessage, Map metadata, int... ctxid) {
        assertStepStateEvent(o, running, nodename, errorMessage, metadata, asContextIds(ctxid));
    }

    private StepContextId[] asContextIds(int[] ctxid) {
        StepContextId[] stepContextIds = new StepContextId[ctxid.length];
        for (int i = 0; i < ctxid.length; i++) {
            int i1 = ctxid[i];
            stepContextIds[i] = StateUtils.stepContextId(i1, false);
        }
        return stepContextIds;
    }

    private void assertStepStateEvent(Object[] o, ExecutionState running, String nodename, String errorMessage,
            Map metadata,
            StepContextId... ctxid) {
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
        assertEquals(errorMessage, change.getStepState().getErrorMessage());
        assertEquals(metadata, change.getStepState().getMetadata());


        assertTrue(o[3] instanceof Date);

    }

    private void assertIdentifier(StepIdentifier ident, StepContextId[] ctxid) {
        assertEquals(ctxid.length, ident.getContext().size());
        for (int i =0;i<ctxid.length;i++) {
            assertEquals(ctxid[i].getStep(), ident.getContext().get(i).getStep());
            assertEquals(ctxid[i].getAspect(), ident.getContext().get(i).getAspect());
        }
    }

    private void assertWorkflowStateEvent(Object[] o, ExecutionState running, String... nodes) {
        assertEquals("workflow", o[0]);
        assertEquals(running, o[1]);
        assertTrue(o[2] instanceof Date);
        if (nodes.length <= 0) {
            assertEquals(null, o[3]);
        } else {
            assertTrue(o[3] instanceof List);
            List<String> names = (List<String>) o[3];
            assertEquals(nodes.length, names.size());
            for (String node : nodes) {
                assertTrue(names.contains(node));
            }
        }
    }
    private void assertSubWorkflowStateEvent(Object[] o, ExecutionState running, int[] ctxid,String... nodes) {
        assertSubWorkflowStateEvent(o, running, asContextIds(ctxid), nodes);
    }
    private void assertSubWorkflowStateEvent(Object[] o, ExecutionState running, StepContextId[] ctxid,String... nodes) {
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
            assertTrue(o1 instanceof List);
            List<String> names = (List<String>) o1;
            assertEquals(nodes.length, names.size());
            for (String node : nodes) {
                assertTrue("expected "+node+" in names: "+names,names.contains(node));
            }
        }
    }
}
