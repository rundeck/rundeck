/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* TestBaseWorkflowStrategy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 9/11/12 2:15 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;
import org.apache.tools.ant.BuildListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


/**
 * TestBaseWorkflowStrategy is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestBaseWorkflowStrategy extends AbstractBaseTest {

    public static final String TEST_PROJECT="TestBaseWorkflowStrategy";
    Framework testFramework;
    String testnode;

    public TestBaseWorkflowStrategy(String name) {
        super(name);
    }

    protected void setUp() {
        super.setUp();
        testFramework = getFrameworkInstance();
        testnode = testFramework.getFrameworkNodeName();
        final FrameworkProject frameworkProject = testFramework.getFrameworkProjectMgr().createFrameworkProject(
            TEST_PROJECT);
        File resourcesfile = new File(frameworkProject.getNodesResourceFilePath());
        //copy test nodes to resources file
        try {
            FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                                      resourcesfile);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        File projectdir = new File(getFrameworkProjectsBase(), TEST_PROJECT);
        FileUtils.deleteDir(projectdir);
    }

    public static class testWorkflowStrategy extends BaseWorkflowStrategy{
        private WorkflowExecutionResult result;
        private int execIndex=0;
        private List<Object> results;
        private List<Map<String,Object>> inputs;

        public testWorkflowStrategy(Framework framework) {
            super(framework);
            inputs = new ArrayList<Map<String, Object>>();
            results = new ArrayList<Object>();
        }

        @Override
        public WorkflowExecutionResult executeWorkflowImpl(ExecutionContext executionContext,
                                                           WorkflowExecutionItem item) {

            return result;
        }

        protected StepExecutionResult executeWFItem(final ExecutionContext executionContext,
                                                    final Map<Integer, Object> failedMap,
                                                    final int c,
                                                    final StepExecutionItem cmd, final boolean keepgoing)
            throws WorkflowStepFailureException {

            HashMap<String, Object> input = new HashMap<String, Object>();
            input.put("context", executionContext);
            input.put("failedMap", failedMap);
            input.put("c", c);
            input.put("cmd", cmd);
            input.put("keepgoing", keepgoing);
            inputs.add(input);

            int ndx = execIndex++;
            final Object o = results.get(ndx);
            if (o instanceof Boolean) {
                return new StepExecutionResultImpl((Boolean) o);
            } else if (o instanceof WorkflowStepFailureException) {
                throw (WorkflowStepFailureException) o;
            } else if (o instanceof String) {
                throw new WorkflowStepFailureException((String) o, new StepExecutionResult() {
                    public Exception getException() {
                        return null;
                    }

                    public DispatcherResult getResultObject() {
                        return null;
                    }

                    public boolean isSuccess() {
                        return false;
                    }
                }, c);
            } else {
                fail("Unexpected result at index " + ndx + ": " + o);
                return new StepExecutionResultImpl(false);
            }

        }

        public WorkflowExecutionResult getResult() {
            return result;
        }

        public void setResult(WorkflowExecutionResult result) {
            this.result = result;
        }

        public List<Object> getResults() {
            return results;
        }

        public void setResults(List<Object> results) {
            this.results = results;
        }

        public List<Map<String, Object>> getInputs() {
            return inputs;
        }
    }

    void assertExecWFItems(final List<StepExecutionItem> items,
                           final boolean wfKeepgoing,
                           final List<Map<String, Object>> expected,
                           final boolean expectedSuccess,
                           final List<Object> returnResults, final boolean expectStepException)
         {

        //test success 1 item
        final NodeSet nodeset = new NodeSet();
             final com.dtolabs.rundeck.core.execution.ExecutionContext context =
                 new ExecutionContextImpl.Builder()
                     .frameworkProject(TEST_PROJECT)
                     .user("user1")
                     .nodeSelector(nodeset)
                     .executionListener(new testListener())
                     .framework(testFramework)
                     .build();

        testWorkflowStrategy strategy = new testWorkflowStrategy(testFramework);

        strategy.getResults().addAll(returnResults);


        final Map<Integer, Object> map = new HashMap<Integer, Object>();
        final List<StepExecutionResult> resultList = new ArrayList<StepExecutionResult>();
        final boolean keepgoing = wfKeepgoing;

        boolean itemsSuccess=false;
        boolean sawException=false;
        try {
            itemsSuccess = strategy.executeWorkflowItemsForNodeSet(context,
                                                                                 map,
                                                                                 resultList,
                                                                                 items,
                                                                                 keepgoing);
            assertFalse(expectStepException);
        } catch (WorkflowStepFailureException e) {
            assertTrue("Unexpected step exception: " + e.getMessage(), expectStepException);
            e.printStackTrace();
            sawException = true;
        }

        assertEquals(expectStepException, sawException);
        assertEquals(expectedSuccess, itemsSuccess);

        assert expected.size() == strategy.getInputs().size();
        int i=0;
        for (final Map<String, Object> expectedMap : expected) {
            final Map<String, Object> map1 = strategy.getInputs().get(i);
            assertEquals("ExpectedMap index " + i + " value c",expectedMap.get("c"), map1.get("c"));
            assertEquals("ExpectedMap index " + i + " value cmd",expectedMap.get("cmd"), map1.get("cmd"));
            assertEquals("ExpectedMap index "+i+" value keepgoing",expectedMap.get("keepgoing"), map1.get("keepgoing"));
            i++;
        }

    }
    private List<StepExecutionItem> mkTestItems(StepExecutionItem... item) {
        return Arrays.asList(item);
    }

    public void testExecuteWorkflowItemsForNodeSet() throws Exception {
        {
            //test success 1 item

            final testWorkflowCmdItem testCmd1 = new testWorkflowCmdItem();
            getFrameworkInstance().getStepExecutionService().registerInstance("test1",new StepExecutor() {
                @Override
                public boolean isNodeDispatchStep(StepExecutionItem item) {
                    return true;
                }

                @Override
                public StepExecutionResult executeWorkflowStep(ExecutionContext executionContext,
                                                               StepExecutionItem item)
                    throws StepException {
                    return null;
                }
            });
            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", false);

            assertExecWFItems(
                mkTestItems(testCmd1),
                false,
                Arrays.asList(expectResult1),
                true,
                Arrays.asList((Object) true), false
            );


        }


        {

            //test failure 1 item no keepgoing

            final testWorkflowCmdItem testCmd1 = new testWorkflowCmdItem();

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", false);

            assertExecWFItems(
                mkTestItems(testCmd1),
                false,
                Arrays.asList(expectResult1),
                false,
                Arrays.asList((Object) false), false
            );
        }

        {
            //test failure 1 exception no keepgoing


            final testWorkflowCmdItem testCmd1 = new testWorkflowCmdItem();

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", false);

            assertExecWFItems(
                mkTestItems(testCmd1),
                false,
                Arrays.asList(expectResult1),
                false,
                Arrays.asList((Object) "Failure"),
                true
            );

        }
        {
            //test failure 1 exception yes keepgoing


            final testWorkflowCmdItem testCmd1 = new testWorkflowCmdItem();

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", true);

            assertExecWFItems(
                mkTestItems(testCmd1),
                true,
                Arrays.asList(expectResult1),
                false,
                Arrays.asList((Object) "Failure"), false
            );

        }

    }

    public void testExecuteWorkflowItemsForNodeSetFailureHandler() throws Exception {
        {
            //test success 1 item, no failure handler


            final testHandlerWorkflowCmdItem testCmd1 = new testHandlerWorkflowCmdItem();
            testCmd1.failureHandler = null;

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", false);

            assertExecWFItems(
                mkTestItems(testCmd1),
                false,
                Arrays.asList(expectResult1),
                true,
                Arrays.asList((Object) true), false
            );

        }


        {
            //test failure 1 item no keepgoing, with failure handler (keepgoing=false)


            final testHandlerWorkflowCmdItem testCmd1 = new testHandlerWorkflowCmdItem();
            testCmd1.failureHandler = null;
            final testWorkflowCmdItem testCmdHandler1 = new testWorkflowCmdItem();
            testCmd1.failureHandler = testCmdHandler1;
            testCmdHandler1.keepgoingOnSuccess = false;

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", false);
            final Map<String, Object> expectResult2 = new HashMap<String, Object>();
            expectResult2.put("c", 1);
            expectResult2.put("cmd", testCmdHandler1);
            expectResult2.put("keepgoing", false);

            assertExecWFItems(
                mkTestItems(testCmd1),
                false,
                Arrays.asList(expectResult1,expectResult2),
                false,
                Arrays.asList((Object) false,true),//item1 fails, handler succeeds
                false
            );


        }

        {
            //test failure 1 item no keepgoing throw exception, with failure handler (keepgoing=false)


            final testHandlerWorkflowCmdItem testCmd1 = new testHandlerWorkflowCmdItem();
            testCmd1.failureHandler = null;
            final testWorkflowCmdItem testCmdHandler1 = new testWorkflowCmdItem();
            testCmd1.failureHandler = testCmdHandler1;
            testCmdHandler1.keepgoingOnSuccess = false;

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", false);
            final Map<String, Object> expectResult2 = new HashMap<String, Object>();
            expectResult2.put("c", 1);
            expectResult2.put("cmd", testCmdHandler1);
            expectResult2.put("keepgoing", false);

            assertExecWFItems(
                mkTestItems(testCmd1),
                false,
                Arrays.asList(expectResult1, expectResult2),
                false,
                Arrays.asList((Object) "Failure", true),//item1 fails, handler succeeds
                true
            );

        }
    }
    public void testExecuteWorkflowItemsForNodeSetFailureHandlerKeepgoing() throws Exception {
        {
            //test failure 1 item yes keepgoing, with failure handler (keepgoing=false)


            final testHandlerWorkflowCmdItem testCmd1 = new testHandlerWorkflowCmdItem();
            testCmd1.failureHandler = null;
            final testWorkflowCmdItem testCmdHandler1 = new testWorkflowCmdItem();
            testCmd1.failureHandler = testCmdHandler1;
            testCmdHandler1.keepgoingOnSuccess = false;

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", true);
            final Map<String, Object> expectResult2 = new HashMap<String, Object>();
            expectResult2.put("c", 1);
            expectResult2.put("cmd", testCmdHandler1);
            expectResult2.put("keepgoing", false);

            assertExecWFItems(
                mkTestItems(testCmd1),
                true,
                Arrays.asList(expectResult1, expectResult2),
                true,
                Arrays.asList((Object) false, true),//item1 fails, handler succeeds
                false
            );

        }

        {
            //test failure 1 item yes keepgoing throw exception, with failure handler (keepgoing=false)


            final testHandlerWorkflowCmdItem testCmd1 = new testHandlerWorkflowCmdItem();
            testCmd1.failureHandler = null;
            final testWorkflowCmdItem testCmdHandler1 = new testWorkflowCmdItem();
            testCmd1.failureHandler = testCmdHandler1;
            testCmdHandler1.keepgoingOnSuccess = false;

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", true);
            final Map<String, Object> expectResult2 = new HashMap<String, Object>();
            expectResult2.put("c", 1);
            expectResult2.put("cmd", testCmdHandler1);
            expectResult2.put("keepgoing", false);

            assertExecWFItems(
                mkTestItems(testCmd1),
                true,
                Arrays.asList(expectResult1, expectResult2),
                true,
                Arrays.asList((Object) "Failure", true),//item1 fails, handler succeeds
                false
            );

        }

        {
            //test failure 2 items yes keepgoing throw exception, with failure handler (keepgoing=false)
            final testHandlerWorkflowCmdItem testCmd1 = new testHandlerWorkflowCmdItem();
            testCmd1.failureHandler = null;
            final testWorkflowCmdItem testCmdHandler1 = new testWorkflowCmdItem();
            testCmd1.failureHandler = testCmdHandler1;
            testCmdHandler1.keepgoingOnSuccess = false;
            final testWorkflowCmdItem testCmd2 = new testWorkflowCmdItem();

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", true);
            final Map<String, Object> expectResult2 = new HashMap<String, Object>();
            expectResult2.put("c", 1);
            expectResult2.put("cmd", testCmdHandler1);
            expectResult2.put("keepgoing", false);
            final Map<String, Object> expectResult3 = new HashMap<String, Object>();
            expectResult3.put("c", 2);
            expectResult3.put("cmd", testCmd2);
            expectResult3.put("keepgoing", true);

            assertExecWFItems(
                mkTestItems(testCmd1, testCmd2),
                true,
                Arrays.asList(expectResult1, expectResult2, expectResult3),
                true,
                Arrays.asList((Object) "Failure",true, true),//item1 fails, handler succeeds, item2 succeeds
                false
            );

        }
        {
            //test failure 2 items yes keepgoing throw exception, with failure handler (keepgoing=false)

            final testHandlerWorkflowCmdItem testCmd1 = new testHandlerWorkflowCmdItem();
            testCmd1.failureHandler = null;
            final testWorkflowCmdItem testCmdHandler1 = new testWorkflowCmdItem();
            testCmd1.failureHandler = testCmdHandler1;
            testCmdHandler1.keepgoingOnSuccess = false;
            final testWorkflowCmdItem testCmd2 = new testWorkflowCmdItem();

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", true);
            final Map<String, Object> expectResult2 = new HashMap<String, Object>();
            expectResult2.put("c", 1);
            expectResult2.put("cmd", testCmdHandler1);
            expectResult2.put("keepgoing", false);
            final Map<String, Object> expectResult3 = new HashMap<String, Object>();
            expectResult3.put("c", 2);
            expectResult3.put("cmd", testCmd2);
            expectResult3.put("keepgoing", true);

            assertExecWFItems(
                mkTestItems(testCmd1, testCmd2),
                true,
                Arrays.asList(expectResult1, expectResult2, expectResult3),
                false,
                Arrays.asList((Object) "Failure", true, false),//item1 fails, handler succeeds, item2 fails
                false
            );

        }
    }

    public void testExecuteWorkflowItemsForNodeSetFailureHandlerKeepgoingOnSuccess() throws Exception {

        {
            //test failure 1 item no keepgoing, with failure handler (keepgoing=true)
            final testHandlerWorkflowCmdItem testCmd1 = new testHandlerWorkflowCmdItem();
            testCmd1.failureHandler = null;
            final testWorkflowCmdItem testCmdHandler1 = new testWorkflowCmdItem();
            testCmd1.failureHandler = testCmdHandler1;
            testCmdHandler1.keepgoingOnSuccess = true;
            final testWorkflowCmdItem testCmd2 = new testWorkflowCmdItem();

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", false);
            final Map<String, Object> expectResult2 = new HashMap<String, Object>();
            expectResult2.put("c", 1);
            expectResult2.put("cmd", testCmdHandler1);
            expectResult2.put("keepgoing", false);
            final Map<String, Object> expectResult3 = new HashMap<String, Object>();
            expectResult3.put("c", 2);
            expectResult3.put("cmd", testCmd2);
            expectResult3.put("keepgoing", false);

            assertExecWFItems(
                mkTestItems(testCmd1, testCmd2),
                false,
                Arrays.asList(expectResult1, expectResult2, expectResult3),
                true,
                Arrays.asList((Object) false, true, true),//item1 fails, handler succeeds, item2 succeeds
                false
            );

        }
        {
            //test failure 1 item no keepgoing, with failure handler (keepgoing=true) fails
            final testHandlerWorkflowCmdItem testCmd1 = new testHandlerWorkflowCmdItem();
            testCmd1.failureHandler = null;
            final testWorkflowCmdItem testCmdHandler1 = new testWorkflowCmdItem();
            testCmd1.failureHandler = testCmdHandler1;
            testCmdHandler1.keepgoingOnSuccess = true;
            final testWorkflowCmdItem testCmd2 = new testWorkflowCmdItem();

            final Map<String, Object> expectResult1 = new HashMap<String, Object>();
            expectResult1.put("c", 1);
            expectResult1.put("cmd", testCmd1);
            expectResult1.put("keepgoing", false);
            final Map<String, Object> expectResult2 = new HashMap<String, Object>();
            expectResult2.put("c", 1);
            expectResult2.put("cmd", testCmdHandler1);
            expectResult2.put("keepgoing", false);

            assertExecWFItems(
                mkTestItems(testCmd1, testCmd2),
                false,
                Arrays.asList(expectResult1, expectResult2),
                false,
                Arrays.asList((Object) false, false, "should not be executed"),//item1 fails, handler fails, item2 succeeds
                false
            );

        }

    }


    static class testWorkflowCmdItem implements NodeStepExecutionItem,HandlerExecutionItem {
        private String type;
        private String nodeStepType;
        int flag = -1;
        boolean keepgoingOnSuccess;

        public boolean isKeepgoingOnSuccess() {
            return keepgoingOnSuccess;
        }


        @Override
        public String toString() {
            return "testWorkflowCmdItem{" +
                   "type='" + type + '\'' +
                   ", flag=" + flag +
                   ", keepgoingOnSuccess=" + keepgoingOnSuccess +
                   '}';
        }

        public String getType() {
            return type;
        }

        @Override
        public String getNodeStepType() {
            return type;
        }
    }

    static class testHandlerWorkflowCmdItem implements StepExecutionItem,HasFailureHandler {
        private String type;
        private StepExecutionItem failureHandler;
        int flag = -1;

        @Override
        public String toString() {
            return "testHandlerWorkflowCmdItem{" +
                   "type='" + type + '\'' +
                   ", flag=" + flag +
                   ", failureHandler=" + failureHandler +
                   '}';
        }

        public String getType() {
            return type;
        }

        public StepExecutionItem getFailureHandler() {
            return failureHandler;
        }
    }

    static class testInterpreter implements NodeStepExecutor {
        List<StepExecutionItem> executionItemList = new ArrayList<StepExecutionItem>();
        List<ExecutionContext> executionContextList = new ArrayList<ExecutionContext>();
        List<INodeEntry> nodeEntryList = new ArrayList<INodeEntry>();
        int index = 0;
        List<NodeStepResult> resultList = new ArrayList<NodeStepResult>();
        boolean shouldThrowException = false;

        public NodeStepResult executeNodeStep(ExecutionContext executionContext,
                                                 NodeStepExecutionItem executionItem, INodeEntry iNodeEntry) throws
                                                                                                     NodeStepException {
            executionItemList.add(executionItem);
            executionContextList.add(executionContext);
            nodeEntryList.add(iNodeEntry);
            if (shouldThrowException) {
                throw new NodeStepException("testInterpreter test exception", iNodeEntry.getNodename());
            }
            System.out.println("return index: (" + index + ") in size: " + resultList.size());
            return resultList.get(index++);
        }
    }


    static class testListener implements ExecutionListenerOverride {
        public boolean isTerse() {
            return false;
        }

        public String getLogFormat() {
            return null;
        }

        public void log(int i, String s) {
            System.err.println(i + ": " + s);
        }

        public FailedNodesListener getFailedNodesListener() {
            return null;
        }

        public void beginStepExecution(ExecutionContext context, StepExecutionItem item) {
        }

        public void finishStepExecution(StatusResult result, ExecutionContext context, StepExecutionItem item) {
        }

        public void beginNodeExecution(ExecutionContext context, String[] command, INodeEntry node) {
        }

        public void finishNodeExecution(NodeExecutorResult result, ExecutionContext context, String[] command,
                                        INodeEntry node) {
        }

        public void beginNodeDispatch(ExecutionContext context, StepExecutionItem item) {
        }

        public void beginNodeDispatch(ExecutionContext context, Dispatchable item) {
        }

        public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, StepExecutionItem item) {
        }

        public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, Dispatchable item) {
        }

        public void beginFileCopyFileStream(ExecutionContext context, InputStream input, INodeEntry node) {
        }

        public void beginFileCopyFile(ExecutionContext context, File input, INodeEntry node) {
        }

        public void beginFileCopyScriptContent(ExecutionContext context, String input, INodeEntry node) {
        }

        public void finishFileCopy(String result, ExecutionContext context, INodeEntry node) {
        }

        public void beginExecuteNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node) {
        }

        public void finishExecuteNodeStep(NodeStepResult result, ExecutionContext context, StepExecutionItem item,
                                          INodeEntry node) {
        }

        public BuildListener getBuildListener() {
            return null;
        }

        public ExecutionListenerOverride createOverride() {
            return this;
        }

        public void setTerse(boolean terse) {
        }

        public void setLogFormat(String format) {
        }

        public void setFailedNodesListener(FailedNodesListener listener) {
        }
    }

}
