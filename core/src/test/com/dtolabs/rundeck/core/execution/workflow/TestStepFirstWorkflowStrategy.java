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

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.Nodes;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.commands.*;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;
import junit.framework.*;
import org.apache.tools.ant.BuildListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
        final FrameworkProject frameworkProject = testFramework.getFrameworkProjectMgr().createFrameworkProject(
            TEST_PROJECT);
        File resourcesfile = new File(frameworkProject.getNodesResourceFilePath());
        //copy test nodes to resources file
        try {
            FileUtils.copyFileStreams(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
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

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    static class testWorkflowCmdItem implements ExecutionItem {
        private String type;
        int flag=-1;

        @Override
        public String toString() {
            return "testWorkflowCmdItem{" +
                   "type='" + type + '\'' +
                   ", flag=" + flag +
                   '}';
        }

        public String getType() {
            return type;
        }
    }

    /*static class testWorkflowJobCmdItem extends testWorkflowCmdItem implements IWorkflowJobItem {
        private String jobIdentifier;

        public String getJobIdentifier() {
            return jobIdentifier;
        }
    }*/

    static class testListener implements ExecutionListener {
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

        public void beginExecution(ExecutionContext context, ExecutionItem item) {
        }

        public void finishExecution(ExecutionResult result, ExecutionContext context, ExecutionItem item) {
        }

        public void beginNodeExecution(ExecutionContext context, String[] command, INodeEntry node) {
        }

        public void finishNodeExecution(NodeExecutorResult result, ExecutionContext context, String[] command,
                                        INodeEntry node) {
        }

        public void beginNodeDispatch(ExecutionContext context, ExecutionItem item) {
        }

        public void beginNodeDispatch(ExecutionContext context, Dispatchable item) {
        }

        public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, ExecutionItem item) {
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

        public void beginInterpretCommand(ExecutionContext context, ExecutionItem item, INodeEntry node) {
        }

        public void finishInterpretCommand(InterpreterResult result, ExecutionContext context, ExecutionItem item,
                                           INodeEntry node) {
        }

        public BuildListener getBuildListener() {
            return null;
        }
    }

    static class testInterpreter implements CommandInterpreter {
        List<ExecutionItem> executionItemList = new ArrayList<ExecutionItem>();
        List<ExecutionContext> executionContextList = new ArrayList<ExecutionContext>();
        List<INodeEntry> nodeEntryList = new ArrayList<INodeEntry>();
        int index = 0;
        List<InterpreterResult> resultList = new ArrayList<InterpreterResult>();
        boolean shouldThrowException = false;

        public InterpreterResult interpretCommand(ExecutionContext executionContext,
                                                  ExecutionItem executionItem, INodeEntry iNodeEntry) throws
            InterpreterException {
            executionItemList.add(executionItem);
            executionContextList.add(executionContext);
            nodeEntryList.add(iNodeEntry);
            if (shouldThrowException) {
                throw new InterpreterException("testInterpreter test exception");
            }
            return resultList.get(index++);
        }
    }
    static class testResult implements InterpreterResult{
        boolean success;
        int flag;

        testResult(boolean success, int flag) {
            this.success = success;
            this.flag = flag;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    public void testExecuteWorkflow() throws Exception {
        {
            final FrameworkProject frameworkProject = testFramework.getFrameworkProjectMgr().getFrameworkProject(
                TEST_PROJECT);
            final Nodes nodes = frameworkProject.getNodes();
            assertNotNull(nodes);
            assertEquals(2, nodes.countNodes());

        }

        {
            //test empty workflow
            final NodeSet nodeset = new NodeSet();
            final WorkflowImpl workflow = new WorkflowImpl(new ArrayList<ExecutionItem>(), 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final com.dtolabs.rundeck.core.execution.ExecutionContext context =
                com.dtolabs.rundeck.core.execution.ExecutionContextImpl.createExecutionContextImpl(
                    TEST_PROJECT,
                    "user1",
                    nodeset,
                    null,
                    0,
                    null,
                    new testListener(),testFramework);

            //setup testInterpreter for all command types
            final CommandInterpreterService interpreterService = CommandInterpreterService.getInstanceForFramework(
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
            final ArrayList<ExecutionItem> commands = new ArrayList<ExecutionItem>();
            commands.add(new testWorkflowCmdItem());

            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false, WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final com.dtolabs.rundeck.core.execution.ExecutionContext context =
                com.dtolabs.rundeck.core.execution.ExecutionContextImpl.createExecutionContextImpl(
                    TEST_PROJECT,
                    "user1",
                    nodeset,
                    null,
                    0,
                    null,
                    new testListener(), testFramework);

            //setup testInterpreter for all command types
            final CommandInterpreterService interpreterService = CommandInterpreterService.getInstanceForFramework(
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
            assertFalse(result.isSuccess());
            assertEquals(0, interpreterMock.executionItemList.size());
            assertNotNull("threw exception: " + result.getException(), result.getException());
            assertTrue("threw exception: " + result.getException(),
                result.getException() instanceof WorkflowStepFailureException);
            assertEquals("threw exception: " + result.getException(), "Step 1 of the workflow threw an exception: Failed dispatching to node test1: com.dtolabs.rundeck.core.execution.service.MissingProviderException: provider name was null provider: null for Service: CommandInterpreter",                result.getException().getMessage());
        }

        {
            //test script exec item
            final NodeSet nodeset = new NodeSet(testFramework.getFrameworkNodeName());
            final ArrayList<ExecutionItem> commands = new ArrayList<ExecutionItem>();
            final ExecutionItem testWorkflowCmdItem = new ScriptFileCommandBase(){
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
            final com.dtolabs.rundeck.core.execution.ExecutionContext context =
                com.dtolabs.rundeck.core.execution.ExecutionContextImpl.createExecutionContextImpl(
                    TEST_PROJECT,
                    "user1",
                    nodeset,
                    null,
                    0,
                    null,
                    new testListener(),testFramework);

            //setup testInterpreter for all command types
            final CommandInterpreterService interpreterService = CommandInterpreterService.getInstanceForFramework(
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
            interpreterMock.resultList.add(new InterpreterResult() {
                public boolean isSuccess() {
                    return true;
                }
            });

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(1, interpreterMock.executionItemList.size());
            final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem scriptItem = (ScriptFileCommandExecutionItem) executionItem1;
            assertEquals("a command", scriptItem.getScript());
            assertNull(scriptItem.getScriptAsStream());
            assertNull(scriptItem.getServerScriptFilePath());
            assertEquals(1, interpreterMock.executionContextList.size());
            final ExecutionContext executionContext = interpreterMock.executionContextList.get(0);
            assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());
            assertNull(executionContext.getArgs());
            assertNull(executionContext.getDataContext());
            assertEquals(0, executionContext.getLoglevel());
            assertEquals("user1", executionContext.getUser());
            assertEquals("expected " + nodeset + ", but was " + executionContext.getNodeSet(), nodeset,
                executionContext.getNodeSet());
        }
        {
            //test command exec item
            final NodeSet nodeset = new NodeSet(testFramework.getFrameworkNodeName());
            final ArrayList<ExecutionItem> commands = new ArrayList<ExecutionItem>();
            final ExecutionItem testWorkflowCmdItem = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a","command"};
                }
            };

            commands.add(testWorkflowCmdItem);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final com.dtolabs.rundeck.core.execution.ExecutionContext context =
                com.dtolabs.rundeck.core.execution.ExecutionContextImpl.createExecutionContextImpl(
                    TEST_PROJECT,
                    "user1",
                    nodeset,
                    null,
                    0,
                    null,
                    new testListener(),testFramework);

            //setup testInterpreter for all command types
            final CommandInterpreterService interpreterService = CommandInterpreterService.getInstanceForFramework(
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
            interpreterMock.resultList.add(new InterpreterResult() {
                public boolean isSuccess() {
                    return true;
                }
            });

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(1, interpreterMock.executionItemList.size());
            final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
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
            assertNull(executionContext.getArgs());
            assertNull(executionContext.getDataContext());
            assertEquals(0, executionContext.getLoglevel());
            assertEquals("user1", executionContext.getUser());
            assertEquals(nodeset, executionContext.getNodeSet());
        }
        {
            //test workflow of three successful items
            final NodeSet nodeset = new NodeSet(testFramework.getFrameworkNodeName());
            final ArrayList<ExecutionItem> commands = new ArrayList<ExecutionItem>();

            final ExecutionItem testWorkflowCmdItem = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "2","command"};
                }
            };

            commands.add(testWorkflowCmdItem);

            final ExecutionItem testWorkflowCmdItemScript = new ScriptFileCommandBase() {
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


            final ExecutionItem testWorkflowCmdItemScript2 = new ScriptFileCommandBase() {
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
            final com.dtolabs.rundeck.core.execution.ExecutionContext context =
                com.dtolabs.rundeck.core.execution.ExecutionContextImpl.createExecutionContextImpl(
                    TEST_PROJECT,
                    "user1",
                    nodeset,
                    new String[]{"test","args"},
                    0,
                    null,
                    new testListener(),testFramework);

            //setup testInterpreter for all command types
            final CommandInterpreterService interpreterService = CommandInterpreterService.getInstanceForFramework(
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
            assertEquals(1, result.getResultSet().size());
            assertNotNull(result.getResultSet());
            assertNotNull("missing key "+testnode+": " + result.getResultSet().keySet(), result.getResultSet().get(testnode));
            final List<StatusResult> test1 = result.getResultSet().get(testnode);
            assertEquals(3, test1.size());
            for (final int i : new int[]{0, 1, 2}) {
                final StatusResult interpreterResult = test1.get(i);
                assertTrue(interpreterResult instanceof testResult);
                testResult val = (testResult) interpreterResult;
                assertTrue(val.isSuccess());
                assertEquals(i, val.flag);
            }

            assertEquals(3, interpreterMock.executionItemList.size());

            final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof ExecCommandExecutionItem);
            ExecCommandExecutionItem execItem = (ExecCommandExecutionItem) executionItem1;
            assertNotNull(execItem.getCommand());
            assertEquals(3, execItem.getCommand().length);
            assertEquals("a", execItem.getCommand()[0]);
            assertEquals("2", execItem.getCommand()[1]);
            assertEquals("command", execItem.getCommand()[2]);

            final ExecutionItem item2 = interpreterMock.executionItemList.get(1);
            assertTrue("wrong class: " + item2.getClass().getName(),
                item2 instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem scriptItem = (ScriptFileCommandExecutionItem) item2;
            assertEquals("a command", scriptItem.getScript());
            assertNull(scriptItem.getScriptAsStream());
            assertNull(scriptItem.getServerScriptFilePath());

            final ExecutionItem item3 = interpreterMock.executionItemList.get(2);
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
                assertNull("item " + i, executionContext.getDataContext());
                assertEquals("item " + i,0, executionContext.getLoglevel());
                assertEquals("item " + i,"user1", executionContext.getUser());
                assertEquals("item " + i,nodeset, executionContext.getNodeSet());
                assertNotNull("item " + i,executionContext.getArgs());
                assertEquals("item " + i,2, executionContext.getArgs().length);
                assertEquals("item " + i,"test", executionContext.getArgs()[0]);
                assertEquals("item " + i,"args", executionContext.getArgs()[1]);

            }
        }
        {
            //test a workflow with a failing item (1), with keepgoing=false
            final NodeSet nodeset = new NodeSet(testFramework.getFrameworkNodeName());
            final ArrayList<ExecutionItem> commands = new ArrayList<ExecutionItem>();

            final ExecutionItem testWorkflowCmdItem = new ExecCommandBase() {
                @Override
                public String[] getCommand() {
                    return new String[]{"a", "2", "command"};
                }
            };

            commands.add(testWorkflowCmdItem);

            final ExecutionItem testWorkflowCmdItemScript = new ScriptFileCommandBase() {
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


            final ExecutionItem testWorkflowCmdItemScript2 = new ScriptFileCommandBase() {
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
            final com.dtolabs.rundeck.core.execution.ExecutionContext context =
                com.dtolabs.rundeck.core.execution.ExecutionContextImpl.createExecutionContextImpl(
                    TEST_PROJECT,
                    "user1",
                    nodeset,
                    null,
                    0,
                    null,
                    new testListener(),testFramework);

            //setup testInterpreter for all command types
            final CommandInterpreterService interpreterService = CommandInterpreterService.getInstanceForFramework(
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
            assertNotNull("threw exception: " + result.getException(), result.getException());
            assertTrue("threw exception: " + result.getException(), result.getException() instanceof WorkflowStepFailureException);
            WorkflowStepFailureException wfsfe = (WorkflowStepFailureException) result.getException();
            assertEquals(2, wfsfe.getWorkflowStep());
            assertNotNull(wfsfe.getExecutionResult());
            final ExecutionResult executionResult = wfsfe.getExecutionResult();
            assertNotNull(executionResult.getResultObject());
            assertNotNull(executionResult.getResultObject().getResults());
            assertEquals(1, executionResult.getResultObject().getResults().size());
            assertNotNull(executionResult.getResultObject().getResults().get(testnode));
            final StatusResult testnode1 = executionResult.getResultObject().getResults().get(testnode);
            assertNotNull(testnode1);
            assertTrue(testnode1 instanceof testResult);
            testResult failResult = (testResult) testnode1;
            assertEquals(1, failResult.flag);

            assertEquals(1, result.getResultSet().size());
            assertNotNull(result.getResultSet());
            assertNotNull("missing key"+testnode+": " + result.getResultSet().keySet(), result.getResultSet().get(testnode));
            final List<StatusResult> test1 = result.getResultSet().get(testnode);
            assertEquals(2, test1.size());
            for (final int i : new int[]{0, 1}) {
                final StatusResult interpreterResult = test1.get(i);
                assertTrue(interpreterResult instanceof testResult);
                testResult val = (testResult) interpreterResult;
                assertEquals(i, val.flag);
                if(0==i){
                    assertTrue(val.isSuccess());
                }else{
                    assertFalse(val.isSuccess());
                }
            }

            assertEquals(2, interpreterMock.executionItemList.size());

            final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof ExecCommandExecutionItem);
            ExecCommandExecutionItem execItem = (ExecCommandExecutionItem) executionItem1;
            assertNotNull(execItem.getCommand());
            assertEquals(3, execItem.getCommand().length);
            assertEquals("a", execItem.getCommand()[0]);
            assertEquals("2", execItem.getCommand()[1]);
            assertEquals("command", execItem.getCommand()[2]);

            final ExecutionItem item2 = interpreterMock.executionItemList.get(1);
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
                assertNull(executionContext.getDataContext());
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(nodeset, executionContext.getNodeSet());
                assertNull(executionContext.getArgs());
            }
        }
    }
    public void testGenericItem(){

        {
            //test jobref item
            final NodeSet nodeset = new NodeSet(testFramework.getFrameworkNodeName());
            final ArrayList<ExecutionItem> commands = new ArrayList<ExecutionItem>();
            final testWorkflowCmdItem item = new testWorkflowCmdItem();
            item.type = "my-type";
            commands.add(item);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final com.dtolabs.rundeck.core.execution.ExecutionContext context =
                com.dtolabs.rundeck.core.execution.ExecutionContextImpl.createExecutionContextImpl(
                    TEST_PROJECT,
                    "user1",
                    nodeset,
                    null,
                    0,
                    null,
                    new testListener(),
                    testFramework);

            //setup testInterpreter for all command types
            final CommandInterpreterService interpreterService = CommandInterpreterService.getInstanceForFramework(
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
            interpreterMock.resultList.add(new InterpreterResult() {
                public boolean isSuccess() {
                    return true;
                }
            });

            final WorkflowExecutionResult result = strategy.executeWorkflow(context, executionItem);

            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(1, interpreterMock.executionItemList.size());
            final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
            assertTrue("wrong class: " + executionItem1.getClass().getName(),
                executionItem1 instanceof testWorkflowCmdItem);
            testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
            assertNotNull(execItem.getType());
            assertEquals("my-type", execItem.getType());
            assertEquals(1, interpreterMock.executionContextList.size());
            final ExecutionContext executionContext = interpreterMock.executionContextList.get(0);
            assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());
            assertNull(executionContext.getArgs());
            assertNull(executionContext.getDataContext());
            assertEquals(0, executionContext.getLoglevel());
            assertEquals("user1", executionContext.getUser());
            assertEquals(nodeset, executionContext.getNodeSet());
        }
    }

    public void testMultipleNodes() {

        {
            //test jobref item
            final NodeSet nodeset = new NodeSet();
            nodeset.createInclude().setName(".*");
            final ArrayList<ExecutionItem> commands = new ArrayList<ExecutionItem>();
            final testWorkflowCmdItem item = new testWorkflowCmdItem();
            item.type = "my-type";
            commands.add(item);
            final WorkflowImpl workflow = new WorkflowImpl(commands, 1, false,
                WorkflowStrategy.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow);
            final StepFirstWorkflowStrategy strategy = new StepFirstWorkflowStrategy(testFramework);
            final com.dtolabs.rundeck.core.execution.ExecutionContext context =
                com.dtolabs.rundeck.core.execution.ExecutionContextImpl.createExecutionContextImpl(
                    TEST_PROJECT,
                    "user1",
                    nodeset,
                    null,
                    0,
                    null,
                    new testListener(),
                    testFramework);

            //setup testInterpreter for all command types
            final CommandInterpreterService interpreterService = CommandInterpreterService.getInstanceForFramework(
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
            interpreterMock.resultList.add(new InterpreterResult() {
                public boolean isSuccess() {
                    return true;
                }
            });
            //set resturn result node 2
            interpreterMock.resultList.add(new InterpreterResult() {
                public boolean isSuccess() {
                    return true;
                }
            });

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
                final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getType());
                assertEquals("my-type", execItem.getType());

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(0);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());
                assertNull(executionContext.getArgs());
                assertNull(executionContext.getDataContext());
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(new NodeSet("test1"), executionContext.getNodeSet());
            }
            {

                final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(1);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getType());
                assertEquals("my-type", execItem.getType());
                
                final ExecutionContext executionContext = interpreterMock.executionContextList.get(1);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());
                assertNull(executionContext.getArgs());
                assertNull(executionContext.getDataContext());
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(new NodeSet("testnode2"), executionContext.getNodeSet());
            }
        }
    }
    public void testMultipleItemsAndNodes() {

        {
            //test jobref item
            final NodeSet nodeset = new NodeSet();
            nodeset.createInclude().setName(".*");
            final ArrayList<ExecutionItem> commands = new ArrayList<ExecutionItem>();
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
            final com.dtolabs.rundeck.core.execution.ExecutionContext context =
                com.dtolabs.rundeck.core.execution.ExecutionContextImpl.createExecutionContextImpl(
                    TEST_PROJECT,
                    "user1",
                    nodeset,
                    null,
                    0,
                    null,
                    new testListener(),
                    testFramework);

            //setup testInterpreter for all command types
            final CommandInterpreterService interpreterService = CommandInterpreterService.getInstanceForFramework(
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
            interpreterMock.resultList.add(new InterpreterResult() {
                public boolean isSuccess() {
                    return true;
                }
            });
            //set resturn result node 2 step 1
            interpreterMock.resultList.add(new InterpreterResult() {
                public boolean isSuccess() {
                    return true;
                }
            });
            //set resturn result node 1 step 2
            interpreterMock.resultList.add(new InterpreterResult() {
                public boolean isSuccess() {
                    return true;
                }
            });
            //set resturn result node 2 step 2
            interpreterMock.resultList.add(new InterpreterResult() {
                public boolean isSuccess() {
                    return true;
                }
            });

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
                final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(0);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getType());
                assertEquals("my-type", execItem.getType());
                assertEquals(0, execItem.flag);

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(0);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());
                assertNull(executionContext.getArgs());
                assertNull(executionContext.getDataContext());
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(new NodeSet("test1"), executionContext.getNodeSet());
            }
            {//node 2 step 1

                final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(1);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getType());
                assertEquals("my-type", execItem.getType());
                assertEquals(0, execItem.flag);

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(1);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());
                assertNull(executionContext.getArgs());
                assertNull(executionContext.getDataContext());
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(new NodeSet("testnode2"), executionContext.getNodeSet());
            }
            {//node 1 step 2

                final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(2);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getType());
                assertEquals("my-type", execItem.getType());
                assertEquals(1, execItem.flag);

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(2);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());
                assertNull(executionContext.getArgs());
                assertNull(executionContext.getDataContext());
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(new NodeSet("test1"), executionContext.getNodeSet());
            }
            {//node 2 step 2

                final ExecutionItem executionItem1 = interpreterMock.executionItemList.get(3);
                assertTrue("wrong class: " + executionItem1.getClass().getName(),
                    executionItem1 instanceof testWorkflowCmdItem);
                testWorkflowCmdItem execItem = (testWorkflowCmdItem) executionItem1;
                assertNotNull(execItem.getType());
                assertEquals("my-type", execItem.getType());
                assertEquals(1, execItem.flag);

                final ExecutionContext executionContext = interpreterMock.executionContextList.get(3);
                assertEquals(TEST_PROJECT, executionContext.getFrameworkProject());
                assertNull(executionContext.getArgs());
                assertNull(executionContext.getDataContext());
                assertEquals(0, executionContext.getLoglevel());
                assertEquals("user1", executionContext.getUser());
                assertEquals(new NodeSet("testnode2"), executionContext.getNodeSet());
            }
        }
    }
}