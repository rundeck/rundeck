/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.execution.workflow;
/*
* StepFirstWorkflowStrategyTests.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/25/11 9:30 AM
* 
*/

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dtolabs.rundeck.core.common.*;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.tools.ant.BuildListener;
import org.junit.Assert;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.ExecutionListenerOverride;
import com.dtolabs.rundeck.core.execution.FailedNodesListener;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandBase;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommandBase;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommandExecutionItem;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;

public class TestStepFirstWorkflowStrategy extends AbstractBaseTest {
    Framework testFramework;
    String testnode;
    private static final String TEST_PROJECT = "StepFirstWorkflowStrategyTests";

    public TestStepFirstWorkflowStrategy(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestStepFirstWorkflowStrategy.class);
    }

    protected void setUp()  {
        super.setUp();
        testFramework = getFrameworkInstance();
        testnode=testFramework.getFrameworkNodeName();
        final IRundeckProject frameworkProject = testFramework.getFrameworkProjectMgr().createFrameworkProject(
            TEST_PROJECT,
            generateProjectResourcesFile(
                    new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml")
            )
        );

    }

    protected void tearDown() throws Exception {
        super.tearDown();

        File projectdir = new File(getFrameworkProjectsBase(), TEST_PROJECT);
        FileUtils.deleteDir(projectdir);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    static class testWorkflowCmdItem implements NodeStepExecutionItem {
        private String type;
        int flag=-1;

        @Override
        public String toString() {
            return "testWorkflowCmdItem{" +
                   "type='" + type + '\'' +
                   ", flag=" + flag +
                   '}';
        }

        @Override
        public String getNodeStepType() {
            return type;
        }

        public String getType() {
            return "NodeDispatch";
        }
    }

    /*static class testWorkflowJobCmdItem extends testWorkflowCmdItem implements IWorkflowJobItem {
        private String jobIdentifier;

        public String getJobIdentifier() {
            return jobIdentifier;
        }
    }*/

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

        @Override
        public void event(String eventType, String message, Map eventMeta) {
            System.err.println(eventType + ": " + message);
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

    static class testInterpreter implements NodeStepExecutor {
        List<StepExecutionItem> executionItemList = new ArrayList<StepExecutionItem>();
        List<ExecutionContext> executionContextList = new ArrayList<ExecutionContext>();
        List<INodeEntry> nodeEntryList = new ArrayList<INodeEntry>();
        int index = 0;
        List<NodeStepResult> resultList = new ArrayList<NodeStepResult>();
        boolean shouldThrowException = false;

        public NodeStepResult executeNodeStep(StepExecutionContext executionContext,
                                                 NodeStepExecutionItem executionItem, INodeEntry iNodeEntry) throws
                                                                                                     NodeStepException {
            executionItemList.add(executionItem);
            executionContextList.add(executionContext);
            nodeEntryList.add(iNodeEntry);
            if (shouldThrowException) {
                throw new NodeStepException("testInterpreter test exception",null,iNodeEntry.getNodename());
            }
            System.out.println("return index: (" + index + ") in size: " + resultList.size());
            return resultList.get(index++);
        }
    }
    static enum Reason implements FailureReason{
        Test
    }
    static class testResult extends NodeStepResultImpl {
        boolean success;
        int flag;
        INodeEntry node;
        testResult(boolean success, int flag) {
            super(null,success?null: TestStepFirstWorkflowStrategy.Reason.Test,success?null:"test failure",null);
            this.success = success;
            this.flag = flag;
        }


        @Override
        public Exception getException() {
            return null;
        }

        public boolean isSuccess() {
            return success;
        }

        @Override
        public String toString() {
            return "testResult{" +
                   "success=" + success +
                   ", flag=" + flag +
                   '}';
        }

        public INodeEntry getNode() {
            return node;
        }
    }

    public void testExecuteWorkflow() throws Exception {
        {
            final IRundeckProject frameworkProject = testFramework.getFrameworkProjectMgr().getFrameworkProject(
                TEST_PROJECT);
            final INodeSet nodes = frameworkProject.getNodeSet();
            assertNotNull(nodes);
            assertEquals(2, nodes.getNodes().size());

        }

        {
            //test empty workflow
            final NodeSet nodeset = new NodeSet();
            final WorkflowImpl workflow = new WorkflowImpl(new ArrayList<StepExecutionItem>(), 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet())
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            interpreterService.registerInstance("exec", interpreterMock);
            interpreterService.registerInstance("script", interpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, interpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, interpreterMock);
//            interpreterService.registerInstance(JobExecutionItem.COMMAND_TYPE, interpreterMock);


            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(0, interpreterMock.executionItemList.size());
        }
        {
            //test undefined workflow item
            final NodeSet nodeset = new NodeSet();
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();
            commands.add(new testWorkflowCmdItem());

            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false, WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset.nodeSelectorWithDefaultAll())
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(nodeset.nodeSelectorWithDefaultAll(),
                                                  testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            interpreterService.registerInstance("exec", interpreterMock);
            interpreterService.registerInstance("script", interpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, interpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, interpreterMock);
//            interpreterService.registerInstance(JobExecutionItem.COMMAND_TYPE, interpreterMock);


            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.out);
            }
            assertFalse(result.isSuccess());
            assertEquals(0, interpreterMock.executionItemList.size());
            assertNotNull("threw exception: " + result.getException(), result.getException());
            assertTrue("threw exception: " + result.getException(),
                       result.getException() instanceof NullPointerException);
            assertEquals("threw exception: " + result.getException(),
                         "provider name was null for Service: WorkflowNodeStep",
                         result.getException().getMessage());
        }

        {
            //test script exec item
            final NodesSelector nodeset = SelectorUtils.singleNode(testFramework.getFrameworkNodeName());
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();
            final StepExecutionItem testWorkflowCmdItem = new ScriptFileCommandBase(){
                @Override
                public String getScript() {
                    return "a command";
                }
            };
            commands.add(testWorkflowCmdItem);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
            interpreterService.registerInstance("exec", failMock);
            interpreterService.registerInstance("script", interpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);
//            interpreterService.registerInstance(JobExecutionItem.COMMAND_TYPE, failMock);

            //set resturn result
            interpreterMock.resultList.add(new NodeStepResultImpl(null));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(1, interpreterMock.executionItemList.size());
            final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem scriptItem = (ScriptFileCommandExecutionItem) executionItem1;
            assertEquals("a command", scriptItem.getScript());
            assertNull(scriptItem.getScriptAsStream());
            assertNull(scriptItem.getServerScriptFilePath());
            assertEquals(1, interpreterMock.executionContextList.size());
            final ExecutionContext executionContext = interpreterMock.executionContextList.get(0);
            assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

            assertNotNull(executionContext.getDataContext());
            assertNotNull(executionContext.getDataContext().get("node"));
            assertEquals(0, executionContext.getLoglevel());
            assertEquals("user1", executionContext.getUser());
            assertEquals("expected " + nodeset + ", but was " + executionContext.getNodeSelector(), nodeset,
                executionContext.getNodeSelector());
        }
        {
            //test command exec item
            final NodesSelector nodeset = SelectorUtils.singleNode(testFramework.getFrameworkNodeName());
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();
            final StepExecutionItem testWorkflowCmdItem = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "command"};
                }
            };

            commands.add(testWorkflowCmdItem);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
            interpreterService.registerInstance("exec", interpreterMock);
            interpreterService.registerInstance("script", failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);
//            interpreterService.registerInstance(JobExecutionItem.COMMAND_TYPE, failMock);

            //set resturn result
            interpreterMock.resultList.add(new NodeStepResultImpl(null));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(1, interpreterMock.executionItemList.size());
            final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof ExecCommandExecutionItem);
            ExecCommandExecutionItem execItem = (ExecCommandExecutionItem) executionItem1;
            assertNotNull(execItem.getCommand());
            assertEquals(2, execItem.getCommand().length);
            assertEquals("a", execItem.getCommand()[0]);
            assertEquals("command", execItem.getCommand()[1]);
            assertEquals(1, interpreterMock.executionContextList.size());
            final ExecutionContext executionContext = interpreterMock.executionContextList.get(0);
            assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

            assertNotNull(executionContext.getDataContext());
            assertNotNull(executionContext.getDataContext().get("node"));
            assertEquals(0, executionContext.getLoglevel());
            assertEquals("user1", executionContext.getUser());
            assertEquals(nodeset, executionContext.getNodeSelector());
        }
    }
    public void testExecuteWorkflowThreeItems() throws Exception{
        {
            //test workflow of three successful items
            final NodesSelector nodeset = SelectorUtils.singleNode(testFramework.getFrameworkNodeName());
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();

            final StepExecutionItem testWorkflowCmdItem = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "2","command"};
                }
            };

            commands.add(testWorkflowCmdItem);

            final StepExecutionItem testWorkflowCmdItemScript = new ScriptFileCommandBase() {
                @Override
                public String getScript() {
                    return "a command";
                }

                @Override
                public String[] getArgs() {
                    return new String[]{"-testargs", "1"};
                }
            };
            commands.add(testWorkflowCmdItemScript);


            final StepExecutionItem testWorkflowCmdItemScript2 = new ScriptFileCommandBase() {
                @Override
                public String getServerScriptFilePath() {
                    return "/some/file/path";
                }

                @Override
                public String[] getArgs() {
                    return new String[]{"-testargs", "2"};
                }
            };
            commands.add(testWorkflowCmdItemScript2);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
//            interpreterService.registerInstance(JobExecutionItem.COMMAND_TYPE, interpreterMock);
            interpreterService.registerInstance("exec", interpreterMock);
            interpreterService.registerInstance("script", interpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);

            //set resturn results
            interpreterMock.resultList.add(new testResult(true, 0));
            interpreterMock.resultList.add(new testResult(true, 1));
            interpreterMock.resultList.add(new testResult(true, 2));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context,executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertNotNull(result.getResultSet());
            final List<StepExecutionResult> test1 = result.getResultSet();
            assertEquals(3, test1.size());
            for (final int i : new int[]{0, 1, 2}) {
                final StepExecutionResult interpreterResult = test1.get(i);
                final DispatcherResult dr = NodeDispatchStepExecutor.extractDispatcherResult(interpreterResult);
                assertEquals(1, dr.getResults().size());
                final NodeStepResult nrs = dr.getResults().values().iterator().next();
                assertTrue("unexpected class: " + nrs.getClass(),
                           nrs instanceof testResult);
                testResult val = (testResult) nrs;
                assertTrue(val.isSuccess());
                assertEquals(i, val.flag);
            }

            assertEquals(3, interpreterMock.executionItemList.size());

            final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof ExecCommandExecutionItem);
            ExecCommandExecutionItem execItem = (ExecCommandExecutionItem) executionItem1;
            assertNotNull(execItem.getCommand());
            assertEquals(3, execItem.getCommand().length);
            assertEquals("a", execItem.getCommand()[0]);
            assertEquals("2", execItem.getCommand()[1]);
            assertEquals("command", execItem.getCommand()[2]);

            final StepExecutionItem item2 = interpreterMock.executionItemList.get(1);
            assertTrue("wrong class: " + item2.getClass().getName(),
                item2 instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem scriptItem = (ScriptFileCommandExecutionItem) item2;
            assertEquals("a command", scriptItem.getScript());
            assertNull(scriptItem.getScriptAsStream());
            assertNull(scriptItem.getServerScriptFilePath());

            final StepExecutionItem item3 = interpreterMock.executionItemList.get(2);
            assertTrue("wrong class: " + item3.getClass().getName(),
                item2 instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem scriptItem2 = (ScriptFileCommandExecutionItem) item3;
            assertNull(scriptItem2.getScript());
            assertNull(scriptItem2.getScriptAsStream());
            assertEquals("/some/file/path", scriptItem2.getServerScriptFilePath());
            assertNotNull(scriptItem2.getArgs());
            assertEquals(2, scriptItem2.getArgs().length);
            assertEquals("-testargs", scriptItem2.getArgs()[0]);
            assertEquals("2", scriptItem2.getArgs()[1]);


            assertEquals(3, interpreterMock.executionContextList.size());

            for (final int i : new int[]{0, 1, 2}) {
                final ExecutionContext executionContext = interpreterMock.executionContextList.get(i);
                assertEquals("item "+i,TEST_PROJECT, executionContext.getFrameworkProject());
                assertNotNull("item " + i, executionContext.getDataContext());
                assertNotNull("item " + i, executionContext.getDataContext().get("node"));
                assertEquals("item " + i,0, executionContext.getLoglevel());
                assertEquals("item " + i,"user1", executionContext.getUser());
                assertEquals("item " + i,nodeset, executionContext.getNodeSelector());

            }
        }
    }
    public void testWorkflowFailNoKeepgoing() throws Exception{
        {
            //test a workflow with a failing item (1), with keepgoing=false
            final NodesSelector nodeset = SelectorUtils.singleNode(testFramework.getFrameworkNodeName());
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();

            final StepExecutionItem testWorkflowCmdItem = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "2", "command"};
                }
            };

            commands.add(testWorkflowCmdItem);

            final StepExecutionItem testWorkflowCmdItemScript = new ScriptFileCommandBase() {
                @Override
                public String getScript() {
                    return "a command";
                }

                @Override
                public String[] getArgs() {
                    return new String[]{"-testargs", "1"};
                }
            };
            commands.add(testWorkflowCmdItemScript);


            final StepExecutionItem testWorkflowCmdItemScript2 = new ScriptFileCommandBase() {
                @Override
                public String getServerScriptFilePath() {
                    return "/some/file/path";
                }

                @Override
                public String[] getArgs() {
                    return new String[]{"-testargs", "2"};
                }
            };
            commands.add(testWorkflowCmdItemScript2);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            workflow.setKeepgoing(false);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
//            interpreterService.registerInstance(JobExecutionItem.COMMAND_TYPE, interpreterMock);
            interpreterService.registerInstance("exec", interpreterMock);
            interpreterService.registerInstance("script", interpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);

            //set resturn results, fail on second item
            interpreterMock.resultList.add(new testResult(true, 0));
            interpreterMock.resultList.add(new testResult(false, 1));
            interpreterMock.resultList.add(new testResult(true, 2));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (null != result.getException()) {
                result.getException().printStackTrace(System.out);
            }
            assertFalse(result.isSuccess());
            assertNull("threw exception: " + result.getException(), result.getException());
            StepExecutionResult result1 = result.getResultSet().get(1);
            final DispatcherResult executionResult = NodeDispatchStepExecutor.extractDispatcherResult(result1);
            assertNotNull(executionResult.getResults());
            assertEquals(1, executionResult.getResults().size());
            assertNotNull(executionResult.getResults().get(testnode));
            final StatusResult testnode1 = executionResult.getResults().get(testnode);
            assertNotNull(testnode1);
            assertTrue(testnode1 instanceof testResult);
            testResult failResult = (testResult) testnode1;
            assertEquals(1, failResult.flag);

            assertNotNull(result.getResultSet());
            final List<StepExecutionResult> test1 = result.getResultSet();
            assertEquals(2, test1.size());
            for (final int i : new int[]{0, 1}) {
                final StepExecutionResult interpreterResult = test1.get(i);
                final DispatcherResult dr = NodeDispatchStepExecutor.extractDispatcherResult(interpreterResult);
                assertEquals(1, dr.getResults().size());
                final NodeStepResult nrs = dr.getResults().values().iterator().next();
                assertTrue("unexpected class: " + nrs.getClass(),
                           nrs instanceof testResult);
                testResult val = (testResult) nrs;
                assertEquals(i, val.flag);
                if(0==i){
                    assertTrue(val.isSuccess());
                }else{
                    assertFalse(val.isSuccess());
                }
            }

            assertEquals(2, interpreterMock.executionItemList.size());

            final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof ExecCommandExecutionItem);
            ExecCommandExecutionItem execItem = (ExecCommandExecutionItem) executionItem1;
            assertNotNull(execItem.getCommand());
            assertEquals(3, execItem.getCommand().length);
            assertEquals("a", execItem.getCommand()[0]);
            assertEquals("2", execItem.getCommand()[1]);
            assertEquals("command", execItem.getCommand()[2]);

            final StepExecutionItem item2 = interpreterMock.executionItemList.get(1);
            assertTrue("wrong class: " + item2.getClass().getName(),
                item2 instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem scriptItem = (ScriptFileCommandExecutionItem) item2;
            assertEquals("a command", scriptItem.getScript());
            assertNull(scriptItem.getScriptAsStream());
            assertNull(scriptItem.getServerScriptFilePath());
            assertNotNull(scriptItem.getArgs());
            assertEquals(2, scriptItem.getArgs().length);
            assertEquals("-testargs", scriptItem.getArgs()[0]);
            assertEquals("1",scriptItem.getArgs()[1]);


            assertEquals(2, interpreterMock.executionContextList.size());

            for (final int i : new int[]{0, 1}) {
                final ExecutionContext executionContext = interpreterMock.executionContextList.get(i);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(nodeset, executionContext.getNodeSelector());
            }
        }
    }
    public void testWorkflowFailYesKeepgoing() throws Exception{
        {
            //test a workflow with a failing item (1), with keepgoing=true
            final NodesSelector nodeset = SelectorUtils.singleNode(testFramework.getFrameworkNodeName());
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();

            final StepExecutionItem testWorkflowCmdItem = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "2", "command"};
                }
            };

            commands.add(testWorkflowCmdItem);

            final StepExecutionItem testWorkflowCmdItemScript = new ScriptFileCommandBase() {
                @Override
                public String getScript() {
                    return "a command";
                }

                @Override
                public String[] getArgs() {
                    return new String[]{"-testargs", "1"};
                }
            };
            commands.add(testWorkflowCmdItemScript);


            final StepExecutionItem testWorkflowCmdItemScript2 = new ScriptFileCommandBase() {
                @Override
                public String getServerScriptFilePath() {
                    return "/some/file/path";
                }

                @Override
                public String[] getArgs() {
                    return new String[]{"-testargs", "2"};
                }
            };
            commands.add(testWorkflowCmdItemScript2);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            workflow.setKeepgoing(true);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
//            interpreterService.registerInstance(JobExecutionItem.COMMAND_TYPE, interpreterMock);
            interpreterService.registerInstance("exec", interpreterMock);
            interpreterService.registerInstance("script", interpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);

            //set resturn results, fail on second item
            interpreterMock.resultList.add(new testResult(true, 0));
            interpreterMock.resultList.add(new testResult(false, 1));
            interpreterMock.resultList.add(new testResult(true, 2));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertFalse(result.isSuccess());
            assertNull("threw exception: " + result.getException(), result.getException());

            assertNotNull(result.getResultSet());
            final List<StepExecutionResult> test1 = result.getResultSet();
            assertEquals(3, test1.size());
            for (final int i : new int[]{0, 1, 2}) {
                final StepExecutionResult interpreterResult = test1.get(i);
                assertTrue(NodeDispatchStepExecutor.isWrappedDispatcherResult(interpreterResult));
                final DispatcherResult dr = NodeDispatchStepExecutor.extractDispatcherResult(interpreterResult);
                assertEquals(1, dr.getResults().size());
                final NodeStepResult nrs = dr.getResults().values().iterator().next();
                assertTrue("unexpected class: " + nrs.getClass(),
                           nrs instanceof testResult);
                testResult val = (testResult) nrs;
                assertEquals(i, val.flag);
                if (1 == i) {
                    assertFalse(val.isSuccess());
                } else {
                    assertTrue(val.isSuccess());
                }
            }

            assertEquals(3, interpreterMock.executionItemList.size());

            final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof ExecCommandExecutionItem);
            ExecCommandExecutionItem execItem = (ExecCommandExecutionItem) executionItem1;
            assertNotNull(execItem.getCommand());
            assertEquals(3, execItem.getCommand().length);
            assertEquals("a", execItem.getCommand()[0]);
            assertEquals("2", execItem.getCommand()[1]);
            assertEquals("command", execItem.getCommand()[2]);

            final StepExecutionItem item2 = interpreterMock.executionItemList.get(1);
            assertTrue("wrong class: " + item2.getClass().getName(),
                item2 instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem scriptItem = (ScriptFileCommandExecutionItem) item2;
            assertEquals("a command", scriptItem.getScript());
            assertNull(scriptItem.getScriptAsStream());
            assertNull(scriptItem.getServerScriptFilePath());
            assertNotNull(scriptItem.getArgs());
            assertEquals(2, scriptItem.getArgs().length);
            assertEquals("-testargs", scriptItem.getArgs()[0]);
            assertEquals("1",scriptItem.getArgs()[1]);

            final StepExecutionItem item3 = interpreterMock.executionItemList.get(2);
            assertTrue("wrong class: " + item2.getClass().getName(),
                item2 instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem scriptItem3 = (ScriptFileCommandExecutionItem) item3;
            assertEquals("/some/file/path", scriptItem3.getServerScriptFilePath());
            assertNull(scriptItem3.getScript());
            assertNull(scriptItem3.getScriptAsStream());
            assertNotNull(scriptItem3.getArgs());
            assertEquals(2, scriptItem3.getArgs().length);
            assertEquals("-testargs", scriptItem3.getArgs()[0]);
            assertEquals("2", scriptItem3.getArgs()[1]);


            assertEquals(3, interpreterMock.executionContextList.size());

            for (final int i : new int[]{0, 1}) {
                final ExecutionContext executionContext = interpreterMock.executionContextList.get(i);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(nodeset, executionContext.getNodeSelector());
            }
        }
    }
    public void testFailureHandlerItemNoKeepgoing() throws Exception{
        {
            //test a workflow with a failing item (1), with keepgoing=false, and a failureHandler
            final boolean KEEPGOING_TEST = false;
            final boolean STEP_0_RESULT = false;
            final boolean STEP_1_RESULT = true;
            final boolean HANDLER_RESULT = true;
            final NodesSelector nodeset = SelectorUtils.singleNode(testFramework.getFrameworkNodeName());
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();

            final StepExecutionItem testHandlerItem = new ScriptFileCommandBase() {
                @Override
                public String getScript() {
                    return "failure handler script";
                }

                @Override
                public String[] getArgs() {
                    return new String[]{"failure","script","args"};
                }
            };
            final StepExecutionItem testWorkflowCmdItem = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "2", "command"};
                }

                @Override
                public StepExecutionItem getFailureHandler() {
                    return testHandlerItem;
                }
            };

            commands.add(testWorkflowCmdItem);

            final StepExecutionItem testWorkflowCmdItemScript = new ScriptFileCommandBase() {
                @Override
                public String getScript() {
                    return "a command";
                }

                @Override
                public String[] getArgs() {
                    return new String[]{"-testargs", "1"};
                }
            };
            commands.add(testWorkflowCmdItemScript);

            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            workflow.setKeepgoing(KEEPGOING_TEST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter handlerInterpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
//            interpreterService.registerInstance(JobExecutionItem.COMMAND_TYPE, interpreterMock);
            interpreterService.registerInstance("exec", interpreterMock);
            interpreterService.registerInstance("script", handlerInterpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);

            //set resturn results, fail on second item
            interpreterMock.resultList.add(new testResult(STEP_0_RESULT, 0));
            interpreterMock.resultList.add(new testResult(STEP_1_RESULT, 1));
            handlerInterpreterMock.resultList.add(new testResult(HANDLER_RESULT, 0));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertFalse(result.isSuccess());
            assertNull("threw exception: " + result.getException(), result.getException());
            StepExecutionResult result1 = result.getResultSet().get(0);
            final DispatcherResult executionResult
                = NodeDispatchStepExecutor.extractDispatcherResult(result1);
            assertNotNull(executionResult.getResults());
            assertEquals(1, executionResult.getResults().size());
            assertNotNull(executionResult.getResults().get(testnode));
            final StatusResult testnode1 = executionResult.getResults().get(testnode);
            assertNotNull(testnode1);
            assertTrue(testnode1 instanceof testResult);
            testResult failResult = (testResult) testnode1;
            assertEquals(0, failResult.flag);

            assertEquals(1, result.getResultSet().size());
            assertNotNull(result.getResultSet());
            final List<StepExecutionResult> test1 = result.getResultSet();
            System.err.println("results: "+test1);
            assertEquals(1, test1.size());
            final int i =0;
            final StepExecutionResult interpreterResult = test1.get(i);
            final DispatcherResult dr = NodeDispatchStepExecutor.extractDispatcherResult(interpreterResult);
            assertEquals(1, dr.getResults().size());
            final NodeStepResult nrs = dr.getResults().values().iterator().next();
            assertTrue("unexpected class: " + nrs.getClass(),
                       nrs instanceof testResult);
            testResult val = (testResult) nrs;
            assertEquals(i, val.flag);
            assertFalse(val.isSuccess());

            assertEquals(1, interpreterMock.executionItemList.size());

            final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof ExecCommandExecutionItem);
            ExecCommandExecutionItem execItem = (ExecCommandExecutionItem) executionItem1;
            assertNotNull(execItem.getCommand());
            assertEquals(3, execItem.getCommand().length);
            assertEquals("a", execItem.getCommand()[0]);
            assertEquals("2", execItem.getCommand()[1]);
            assertEquals("command", execItem.getCommand()[2]);

            assertEquals(1, interpreterMock.executionContextList.size());

            final ExecutionContext executionContext = interpreterMock.executionContextList.get(i);
            assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

            assertNotNull(executionContext.getDataContext());
            assertNotNull(executionContext.getDataContext().get("node"));
            assertEquals(0, executionContext.getLoglevel());
            assertEquals("user1", executionContext.getUser());
            assertEquals(nodeset, executionContext.getNodeSelector());

            //check handler item was executed
            assertEquals(1, handlerInterpreterMock.executionItemList.size());

            final StepExecutionItem executionItemX = handlerInterpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItemX.getClass().getName(),
                executionItemX instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem execItemX = (ScriptFileCommandExecutionItem) executionItemX;
            assertNotNull(execItemX.getScript());
            assertNotNull(execItemX.getArgs());
            assertEquals("failure handler script", execItemX.getScript());
            assertEquals(3, execItemX.getArgs().length);
            assertEquals("failure", execItemX.getArgs()[0]);
            assertEquals("script", execItemX.getArgs()[1]);
            assertEquals("args", execItemX.getArgs()[2]);

            assertEquals(1, handlerInterpreterMock.executionContextList.size());

            final ExecutionContext executionContextX = handlerInterpreterMock.executionContextList.get(i);
            assertEquals(TEST_PROJECT, executionContextX.getFrameworkProject());

            assertNotNull(executionContextX.getDataContext());
            assertNotNull(executionContextX.getDataContext().get("node"));
            assertEquals(0, executionContextX.getLoglevel());
            assertEquals("user1", executionContextX.getUser());
            assertEquals(nodeset, executionContextX.getNodeSelector());
        }
    }

    public void testFailureHandlerItemYesKeepgoing() throws Exception{
        {
            //test a workflow with a failing item (1), with keepgoing=true, and a failureHandler that fails
            final boolean KEEPGOING_TEST = true;
            final boolean STEP_0_RESULT = false;
            final boolean STEP_1_RESULT = true;
            final boolean HANDLER_RESULT = false;
            final NodesSelector nodeset = SelectorUtils.singleNode(testFramework.getFrameworkNodeName());
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();

            final StepExecutionItem testHandlerItem = new ScriptFileCommandBase() {
                @Override
                public String getScript() {
                    return "failure handler script";
                }

                @Override
                public String[] getArgs() {
                    return new String[]{"failure","script","args"};
                }

                @Override
                public String toString() {
                    return "testHandlerItem";
                }
            };
            final StepExecutionItem testWorkflowCmdItem = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "2", "command"};
                }

                @Override
                public StepExecutionItem getFailureHandler() {
                    return testHandlerItem;
                }

                @Override
                public String toString() {
                    return "testWorkflowCmdItem";
                }
            };

            commands.add(testWorkflowCmdItem);

            final StepExecutionItem testWorkflowCmdItem2 = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "3", "command"};
                }

                @Override
                public StepExecutionItem getFailureHandler() {
                    return testHandlerItem;
                }

                @Override
                public String toString() {
                    return "testWorkflowCmdItem2";
                }
            };
            commands.add(testWorkflowCmdItem2);

            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            workflow.setKeepgoing(KEEPGOING_TEST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter handlerInterpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
//            interpreterService.registerInstance(JobExecutionItem.COMMAND_TYPE, interpreterMock);
            interpreterService.registerInstance("exec", interpreterMock);
            interpreterService.registerInstance("script", handlerInterpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);

            //set resturn results
            interpreterMock.resultList.add(new testResult(STEP_0_RESULT, 0));
            interpreterMock.resultList.add(new testResult(STEP_1_RESULT, 1));
            handlerInterpreterMock.resultList.add(new testResult(HANDLER_RESULT, 0));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertFalse(result.isSuccess());
            assertNull("threw exception: " + result.getException(), result.getException());

            assertNotNull(result.getResultSet());
            final List<StepExecutionResult> test1 = result.getResultSet();
            System.out.println("results: "+test1);


            assertEquals(2, interpreterMock.executionItemList.size());
            assertEquals(2, interpreterMock.executionContextList.size());
            //check handler item was executed
            assertEquals(1, handlerInterpreterMock.executionItemList.size());
            assertEquals(1, handlerInterpreterMock.executionContextList.size());


            assertEquals(2, test1.size());

            int resultIndex =0;
            int stepNum=0;
            {
                //first step result
                final StepExecutionResult interpreterResult = test1.get(resultIndex);
                final DispatcherResult dr = NodeDispatchStepExecutor.extractDispatcherResult(interpreterResult);
                assertEquals(1, dr.getResults().size());
                final NodeStepResult nrs = dr.getResults().values().iterator().next();
                assertTrue("unexpected class: " + nrs.getClass(),
                           nrs instanceof testResult);
                testResult val = (testResult) nrs;
                assertEquals(0, val.flag);
                assertFalse(val.isSuccess());


                final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(stepNum);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof ExecCommandExecutionItem);
                ExecCommandExecutionItem execItem = (ExecCommandExecutionItem) executionItem1;
                assertNotNull(execItem.getCommand());
                assertEquals(3, execItem.getCommand().length);
                assertEquals("a", execItem.getCommand()[0]);
                assertEquals("2", execItem.getCommand()[1]);
                assertEquals("command", execItem.getCommand()[2]);


                final ExecutionContext executionContext = interpreterMock.executionContextList.get(stepNum);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(nodeset, executionContext.getNodeSelector());
            }

            resultIndex=1;
//
//            {
//                //failure handler result
//                final StepExecutionResult interpreterResult = test1.get(resultIndex);
//                final DispatcherResult dr = NodeDispatchStepExecutor.extractDispatcherResult(interpreterResult);
//                assertEquals(1, dr.getResults().size());
//                final NodeStepResult nrs = dr.getResults().values().iterator().next();
//                assertTrue("unexpected class: " + nrs.getClass(),
//                           nrs instanceof testResult);
//                testResult val = (testResult) nrs;
//                assertEquals(0, val.flag);
//                assertFalse(val.isSuccess());
//
//                final StepExecutionItem executionItemX = handlerInterpreterMock.executionItemList.get(stepNum);
//                assertTrue("wrong class: " + executionItemX.getClass().getName(),
//                    executionItemX instanceof ScriptFileCommandExecutionItem);
//                ScriptFileCommandExecutionItem execItemX = (ScriptFileCommandExecutionItem) executionItemX;
//                assertNotNull(execItemX.getScript());
//                assertNotNull(execItemX.getArgs());
//                assertEquals("failure handler script", execItemX.getScript());
//                assertEquals(3, execItemX.getArgs().length);
//                assertEquals("failure", execItemX.getArgs()[0]);
//                assertEquals("script", execItemX.getArgs()[1]);
//                assertEquals("args", execItemX.getArgs()[2]);
//
//
//                final ExecutionContext executionContextX = handlerInterpreterMock.executionContextList.get(stepNum);
//                assertEquals(TEST_PROJECT, executionContextX.getFrameworkProject());
//                assertNull(executionContextX.getDataContext());
//                assertEquals(0, executionContextX.getLoglevel());
//                assertEquals("user1", executionContextX.getUser());
//                assertEquals(nodeset, executionContextX.getNodeSelector());
//                assertNull(executionContextX.getArgs());
//            }

//            resultIndex=2;
            stepNum = 1;
            {
                //second step result
                final StepExecutionResult interpreterResult = test1.get(resultIndex);
                final DispatcherResult dr = NodeDispatchStepExecutor.extractDispatcherResult(interpreterResult);
                assertEquals(1, dr.getResults().size());
                final NodeStepResult nrs = dr.getResults().values().iterator().next();
                assertTrue("unexpected class: " + nrs.getClass(),
                           nrs instanceof testResult);
                testResult val = (testResult) nrs;
                assertEquals(1, val.flag);
                assertTrue(val.isSuccess());

                final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(stepNum);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof ExecCommandExecutionItem);
                ExecCommandExecutionItem execItem = (ExecCommandExecutionItem) executionItem1;
                assertNotNull(execItem.getCommand());
                assertEquals(3, execItem.getCommand().length);
                assertEquals("a", execItem.getCommand()[0]);
                assertEquals("3", execItem.getCommand()[1]);
                assertEquals("command", execItem.getCommand()[2]);

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(stepNum);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(nodeset, executionContext.getNodeSelector());
            }
        }
    }

    public void testFailureHandlerItemYesKeepgoingHandlerSuccess() throws Exception {
        {
            //test a workflow with a failing item (1), with keepgoing=true, and a failureHandler that succeeds
            final boolean KEEPGOING_TEST = true;
            final boolean STEP_0_RESULT = false;
            final boolean STEP_1_RESULT = true;
            final boolean HANDLER_RESULT = true;
            final NodesSelector nodeset = SelectorUtils.singleNode(testFramework.getFrameworkNodeName());
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();

            final StepExecutionItem testHandlerItem = new ScriptFileCommandBase() {
                @Override
                public String getScript() {
                    return "failure handler script";
                }

                @Override
                public String[] getArgs() {
                    return new String[]{"failure","script","args"};
                }

                @Override
                public String toString() {
                    return "testHandlerItem";
                }
            };
            final StepExecutionItem testWorkflowCmdItem = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "2", "command"};
                }

                @Override
                public StepExecutionItem getFailureHandler() {
                    return testHandlerItem;
                }

                @Override
                public String toString() {
                    return "testWorkflowCmdItem";
                }
            };

            commands.add(testWorkflowCmdItem);

            final StepExecutionItem testWorkflowCmdItem2 = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "3", "command"};
                }

                @Override
                public StepExecutionItem getFailureHandler() {
                    return testHandlerItem;
                }

                @Override
                public String toString() {
                    return "testWorkflowCmdItem2";
                }
            };
            commands.add(testWorkflowCmdItem2);

            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            workflow.setKeepgoing(KEEPGOING_TEST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter handlerInterpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
//            interpreterService.registerInstance(JobExecutionItem.COMMAND_TYPE, interpreterMock);
            interpreterService.registerInstance("exec", interpreterMock);
            interpreterService.registerInstance("script", handlerInterpreterMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);

            //set resturn results
            interpreterMock.resultList.add(new testResult(STEP_0_RESULT, 0));
            interpreterMock.resultList.add(new testResult(STEP_1_RESULT, 1));
            handlerInterpreterMock.resultList.add(new testResult(HANDLER_RESULT, 0));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertTrue(result.isSuccess());
            assertNull("threw exception: " + result.getException(), result.getException());

            assertNotNull(result.getResultSet());
            final List<StepExecutionResult> test1 = result.getResultSet();
            System.err.println("results: "+test1);

            assertEquals(2, test1.size());

            assertEquals(2, interpreterMock.executionItemList.size());
            assertEquals(2, interpreterMock.executionContextList.size());
            //check handler item was executed
            assertEquals(1, handlerInterpreterMock.executionItemList.size());
            assertEquals(1, handlerInterpreterMock.executionContextList.size());


            int resultIndex =0;
            int stepNum=0;
            {
                //failure handler result
                final StepExecutionResult interpreterResult = test1.get(resultIndex);
                final DispatcherResult dr = NodeDispatchStepExecutor.extractDispatcherResult(interpreterResult);
                assertEquals(1, dr.getResults().size());
                final NodeStepResult nrs = dr.getResults().values().iterator().next();
                assertTrue("unexpected class: " + nrs.getClass(),
                           nrs instanceof testResult);
                testResult val = (testResult) nrs;
                assertEquals(0, val.flag);
                assertTrue(val.isSuccess());

                final StepExecutionItem executionItemX = handlerInterpreterMock.executionItemList.get(stepNum);
                assertTrue("wrong class: " + executionItemX.getClass().getName(),
                    executionItemX instanceof ScriptFileCommandExecutionItem);
                ScriptFileCommandExecutionItem execItemX = (ScriptFileCommandExecutionItem) executionItemX;
                assertNotNull(execItemX.getScript());
                assertNotNull(execItemX.getArgs());
                assertEquals("failure handler script", execItemX.getScript());
                assertEquals(3, execItemX.getArgs().length);
                assertEquals("failure", execItemX.getArgs()[0]);
                assertEquals("script", execItemX.getArgs()[1]);
                assertEquals("args", execItemX.getArgs()[2]);


                final ExecutionContext executionContextX = handlerInterpreterMock.executionContextList.get(stepNum);
                assertEquals(TEST_PROJECT, executionContextX.getFrameworkProject());

                assertNotNull(executionContextX.getDataContext());
                assertNotNull(executionContextX.getDataContext().get("node"));
                assertEquals(0, executionContextX.getLoglevel());
                assertEquals("user1", executionContextX.getUser());
                assertEquals(nodeset, executionContextX.getNodeSelector());
            }

            resultIndex=1;
            stepNum = 1;
            {
                //second step result
                final StepExecutionResult interpreterResult = test1.get(resultIndex);
                final DispatcherResult dr = NodeDispatchStepExecutor.extractDispatcherResult(interpreterResult);
                assertEquals(1, dr.getResults().size());
                final NodeStepResult nrs = dr.getResults().values().iterator().next();
                assertTrue("unexpected class: " + nrs.getClass(),
                           nrs instanceof testResult);
                testResult val = (testResult) nrs;
                assertEquals(1, val.flag);
                assertTrue(val.isSuccess());

                final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(stepNum);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof ExecCommandExecutionItem);
                ExecCommandExecutionItem execItem = (ExecCommandExecutionItem) executionItem1;
                assertNotNull(execItem.getCommand());
                assertEquals(3, execItem.getCommand().length);
                assertEquals("a", execItem.getCommand()[0]);
                assertEquals("3", execItem.getCommand()[1]);
                assertEquals("command", execItem.getCommand()[2]);

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(stepNum);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(nodeset, executionContext.getNodeSelector());
            }
        }

    }
    public void testGenericItem() throws Exception{

        {
            //test jobref item
            final NodesSelector nodeset = SelectorUtils.singleNode(testFramework.getFrameworkNodeName());
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();
            final testWorkflowCmdItem item = new testWorkflowCmdItem();
            item.type = "my-type";
            commands.add(item);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
            interpreterService.registerInstance("my-type", interpreterMock);
            interpreterService.registerInstance("exec", failMock);
            interpreterService.registerInstance("script", failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);

            //set resturn result
            interpreterMock.resultList.add(new NodeStepResultImpl(null));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(1, interpreterMock.executionItemList.size());
            final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof testWorkflowCmdItem);
            testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
            assertNotNull(execItem.getNodeStepType());
            assertEquals("my-type", execItem.getNodeStepType());
            assertEquals(1, interpreterMock.executionContextList.size());
            final ExecutionContext executionContext = interpreterMock.executionContextList.get(0);
            assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

            assertNotNull(executionContext.getDataContext());
            assertNotNull(executionContext.getDataContext().get("node"));
            assertEquals(0, executionContext.getLoglevel());
            assertEquals("user1", executionContext.getUser());
            assertEquals(nodeset, executionContext.getNodeSelector());
        }
    }

    public void testMultipleNodes() throws Exception{

        {
            //test jobref item
            final NodeSet nodeset = new NodeSet();
            nodeset.createInclude().setName(".*");
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();
            final testWorkflowCmdItem item = new testWorkflowCmdItem();
            item.type = "my-type";
            commands.add(item);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
            interpreterService.registerInstance("my-type", interpreterMock);
            interpreterService.registerInstance("exec", failMock);
            interpreterService.registerInstance("script", failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);

            //set resturn result node 1
            interpreterMock.resultList.add(new NodeStepResultImpl(null));
            //set resturn result node 2
            interpreterMock.resultList.add(new NodeStepResultImpl(null));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(2, interpreterMock.executionItemList.size());
            assertEquals(2, interpreterMock.executionContextList.size());
            {
                final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getNodeStepType());
                assertEquals("my-type", execItem.getNodeStepType());

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(0);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(SelectorUtils.singleNode("test1"), executionContext.getNodeSelector());
            }
            {

                final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(1);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getNodeStepType());
                assertEquals("my-type", execItem.getNodeStepType());
                
                final ExecutionContext executionContext = interpreterMock.executionContextList.get(1);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(SelectorUtils.singleNode("testnode2"), executionContext.getNodeSelector());
            }
        }
    }
    public void testMultipleItemsAndNodes() throws Exception{

        {
            //test jobref item
            final NodeSet nodeset = new NodeSet();
            nodeset.createInclude().setName(".*");
            final ArrayList<StepExecutionItem> commands = new ArrayList<StepExecutionItem>();
            final testWorkflowCmdItem item = new testWorkflowCmdItem();
            item.flag=0;
            item.type = "my-type";
            commands.add(item);
            final testWorkflowCmdItem item2 = new testWorkflowCmdItem();
            item2.flag = 1;
            item2.type = "my-type";
            commands.add(item2);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final StepExecutionContext context =
                new ExecutionContextImpl.Builder()
                    .frameworkProject(TEST_PROJECT)
                    .user("user1")
                    .nodeSelector(nodeset)
                    .executionListener(new testListener())
                    .framework(testFramework)
                    .nodes(NodeFilter.filterNodes(
                                   nodeset,
                                   testFramework.getFrameworkProjectMgr().getFrameworkProject(TEST_PROJECT).getNodeSet()
                           ))
                    .build();

            //setup testInterpreter for all command types
            final NodeStepExecutionService interpreterService = NodeStepExecutionService.getInstanceForFramework(
                testFramework);
            testInterpreter interpreterMock = new testInterpreter();
            testInterpreter failMock = new testInterpreter();
            failMock.shouldThrowException = true;
            interpreterService.registerInstance("my-type", interpreterMock);
            interpreterService.registerInstance("exec", failMock);
            interpreterService.registerInstance("script", failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_NODE_FIRST, failMock);
            interpreterService.registerInstance(WorkflowExecutionItem.COMMAND_TYPE_STEP_FIRST, failMock);

            //set resturn result node 1 step 1
            interpreterMock.resultList.add(new NodeStepResultImpl(null));
            //set resturn result node 2 step 1
            interpreterMock.resultList.add(new NodeStepResultImpl(null));
            //set resturn result node 1 step 2
            interpreterMock.resultList.add(new NodeStepResultImpl(null));
            //set resturn result node 2 step 2
            interpreterMock.resultList.add(new NodeStepResultImpl(null));

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(4, interpreterMock.executionItemList.size());
            assertEquals(4, interpreterMock.executionContextList.size());
            {//node 1 step 1
                final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getNodeStepType());
                assertEquals("my-type", execItem.getNodeStepType());
                assertEquals(0, execItem.flag);

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(0);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(SelectorUtils.singleNode("test1"), executionContext.getNodeSelector());
            }
            {//node 2 step 1

                final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(1);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getNodeStepType());
                assertEquals("my-type", execItem.getNodeStepType());
                assertEquals(0, execItem.flag);

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(1);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(SelectorUtils.singleNode("testnode2"), executionContext.getNodeSelector());
            }
            {//node 1 step 2

                final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(2);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getNodeStepType());
                assertEquals("my-type", execItem.getNodeStepType());
                assertEquals(1, execItem.flag);

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(2);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(SelectorUtils.singleNode("test1"), executionContext.getNodeSelector());
            }
            {//node 2 step 2

                final StepExecutionItem executionItem1 = interpreterMock.executionItemList.get(3);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getNodeStepType());
                assertEquals("my-type", execItem.getNodeStepType());
                assertEquals(1, execItem.flag);

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(3);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());

                assertNotNull(executionContext.getDataContext());
                assertNotNull(executionContext.getDataContext().get("node"));
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(SelectorUtils.singleNode("testnode2"), executionContext.getNodeSelector());
            }
        }
    }
    
    public void testCreatePrintableDataContext() {
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        
        String otherKey = "other";
        Map<String, String> otherData = new HashMap<String, String>();
        dataContext.put(otherKey, otherData);
        
        Map<String, String> secureData = new HashMap<String, String>();
        String secureKey = "secureKey";
        secureData.put(secureKey, "secureValue");
        dataContext.put(StepFirstWorkflowStrategy.SECURE_OPTION_KEY, secureData);
        
        Map<String, String> regularData = new HashMap<String, String>();
        String insecureKey = "insecureKey";
        regularData.put(insecureKey, "insecureValue");
        regularData.put(secureKey, "secureValue");
        dataContext.put(StepFirstWorkflowStrategy.OPTION_KEY, regularData);
                
        StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
        Map<String, Map<String, String>> result = strategy.createPrintableDataContext(dataContext);
        
        Assert.assertSame("Expected other data to be present", otherData, result.get(otherKey));
        
        Map<String, String> resultSecureData = result.get(StepFirstWorkflowStrategy.SECURE_OPTION_KEY);
        Assert.assertEquals("Expected secure value to be replaced", StepFirstWorkflowStrategy.SECURE_OPTION_VALUE, resultSecureData.get(secureKey));
        
        Map<String, String> resultRegularData = result.get(StepFirstWorkflowStrategy.OPTION_KEY);
        Assert.assertEquals("Expected secure value to be replaced", StepFirstWorkflowStrategy.SECURE_OPTION_VALUE, resultRegularData.get(secureKey));
        Assert.assertEquals("Expected insecure value to be untouched", regularData.get(insecureKey), resultRegularData.get(insecureKey));
    }
    
    public void testCreatePrintableDataContextNoDataContext() {
        StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
        Map<String, Map<String, String>> result = strategy.createPrintableDataContext(null);
        
        Assert.assertTrue("Expected empty data context", result.isEmpty());
    }
    
    public void testCreatePrintableDataContextEmptyDataContext() {
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        
        StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
        Map<String, Map<String, String>> result = strategy.createPrintableDataContext(dataContext);
        
        Assert.assertTrue("Expected empty data context", result.isEmpty());
    }
    
    public void testCreatePrintableDataContextNoSecureData() {
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        
        String otherKey = "other";
        Map<String, String> otherData = new HashMap<String, String>();
        dataContext.put(otherKey, otherData);
        
        Map<String, String> regularData = new HashMap<String, String>();
        String insecureKey = "insecureKey";
        regularData.put(insecureKey, "insecureValue");
        dataContext.put(StepFirstWorkflowStrategy.OPTION_KEY, regularData);
                
        StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
        Map<String, Map<String, String>> result = strategy.createPrintableDataContext(dataContext);
        
        Assert.assertSame("Expected other data to be present", otherData, result.get(otherKey));
                
        Map<String, String> resultRegularData = result.get(StepFirstWorkflowStrategy.OPTION_KEY);
        Assert.assertEquals("Expected insecure value to be untouched", regularData.get(insecureKey), resultRegularData.get(insecureKey));
    }
    
    public void testCreatePrintableDataContextNoRegularData() {
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        
        String otherKey = "other";
        Map<String, String> otherData = new HashMap<String, String>();
        dataContext.put(otherKey, otherData);
        
        Map<String, String> secureData = new HashMap<String, String>();
        String secureKey = "secureKey";
        secureData.put(secureKey, "secureValue");
        dataContext.put(StepFirstWorkflowStrategy.SECURE_OPTION_KEY, secureData);
                
        StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
        Map<String, Map<String, String>> result = strategy.createPrintableDataContext(dataContext);
        
        Assert.assertSame("Expected other data to be present", otherData, result.get(otherKey));
        
        Map<String, String> resultSecureData = result.get(StepFirstWorkflowStrategy.SECURE_OPTION_KEY);
        Assert.assertEquals("Expected secure value to be replaced", StepFirstWorkflowStrategy.SECURE_OPTION_VALUE, resultSecureData.get(secureKey));
    }
    
    @SuppressWarnings("unchecked")
    public void testExecuteWorkflowUsesPrintableDataContext() {
        ExecutionListener listener = Mockito.mock(ExecutionListener.class);
        StepExecutionContext context = Mockito.mock(StepExecutionContext.class);
        Mockito.when(context.getExecutionListener()).thenReturn(listener);
        
        String printableContextToString = "this is hopefully some string that won't appear elsewhere";
        Map<String, Map<String, String>> printableContext = Mockito.mock(Map.class);
        Mockito.when(printableContext.toString()).thenReturn(printableContextToString);
        
        String dataContextToString = "this is another magic string that hopefully won't appear elsewhere";
        Map<String, Map<String, String>> dataContext = Mockito.mock(Map.class);
        Mockito.when(dataContext.toString()).thenReturn(dataContextToString);
        Mockito.when(context.getDataContext()).thenReturn(dataContext);
        
        StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
        strategy = Mockito.spy(strategy);
        Mockito.doReturn(printableContext).when(strategy).createPrintableDataContext(Mockito.same(dataContext));
        
        WorkflowExecutionItem item = Mockito.mock(WorkflowExecutionItem.class);
        strategy.executeWorkflowImpl(context, item);
        
        ArgumentCaptor<String> logLineCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(listener, Mockito.atLeastOnce()).log(Mockito.anyInt(), logLineCaptor.capture());
        
        for (String line : logLineCaptor.getAllValues()) {
            if (line.startsWith(StepFirstWorkflowStrategy.DATA_CONTEXT_PREFIX)) {
                Assert.assertTrue("Expected printable data context string.", line.contains(printableContextToString));
                Assert.assertFalse("Not expecting raw data context string.", line.contains(dataContextToString));
            }
        }
    }
}
