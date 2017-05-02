/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
 */

/*
* TestExecNodeStepExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 3:19 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * TestExecNodeStepExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestExecNodeStepExecutor extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestExecNodeStepExecutor";

    public TestExecNodeStepExecutor(String name) {
        super(name);
    }

    public void setUp() {
        super.setUp();

        final Framework frameworkInstance = getFrameworkInstance();
        final IRundeckProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
            PROJ_NAME);

        generateProjectResourcesFile(
                new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                frameworkProject
        );

    }

    public void tearDown() throws Exception {
        super.tearDown();
        File projectdir = new File(getFrameworkProjectsBase(), PROJ_NAME);
        FileUtils.deleteDir(projectdir);
    }

    public static class testNodeExecutor implements NodeExecutor{
        ExecutionContext testContext;
        String[] testCommand;
        INodeEntry testNode;
        NodeExecutorResult testResult;
        public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) {
            this.testContext=context;
            this.testCommand=command;
            this.testNode=node;
            return testResult;
        }
    }

    public void testInterpretCommand() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ExecNodeStepExecutor interpret = new ExecNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        testNodeExecutor testexec = new testNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost", "test1");
        final StepExecutionContext context = ExecutionContextImpl.builder()
            .frameworkProject(PROJ_NAME)
            .framework(frameworkInstance)
            .user("blah")
            .threadCount(1)
            .build();

        final String[] strings = {"test", "command"};

        ExecCommand command = new ExecCommandBase() {
            public String[] getCommand() {

                return strings;
            }
        };
        {
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
            //returning null from command
            assertNull(interpreterResult);
//            assertEquals(context, testexec.testContext);
            assertTrue(Arrays.deepEquals(strings, testexec.testCommand));
            assertEquals(test1, testexec.testNode);
        }
        testexec.testResult= NodeExecutorResultImpl.createSuccess(test1);

        {
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
            //returning null from command
            assertTrue(interpreterResult.isSuccess());
            assertTrue(interpreterResult instanceof NodeExecutorResult);
            NodeExecutorResult result = (NodeExecutorResult) interpreterResult;
            assertEquals(0, result.getResultCode());
            assertEquals(test1, result.getNode());

//            assertEquals(context, testexec.testContext);
            assertTrue(Arrays.deepEquals(strings, testexec.testCommand));
            assertEquals(test1, testexec.testNode);
        }

    }

    /**
     * Test use of remote node that uses jsch-ssh
     */
    public void testInterpretCommandRemote() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ExecNodeStepExecutor interpret = new ExecNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        testNodeExecutor testexec = new testNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("jsch-ssh", testexec);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost2","testnode2");
        final StepExecutionContext context = ExecutionContextImpl.builder()
            .frameworkProject(PROJ_NAME)
            .framework(frameworkInstance)
            .user("blah")
            .threadCount(1)
            .build();
        final String[] strings = {"test", "command"};

        ExecCommand command = new ExecCommandBase() {
            public String[] getCommand() {

                return strings;
            }
        };
        {
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
            //returning null from command
            assertNull(interpreterResult);
//            assertEquals(context, testexec.testContext);
            assertTrue(Arrays.deepEquals(strings, testexec.testCommand));
            assertEquals(test1, testexec.testNode);
        }
        testexec.testResult = NodeExecutorResultImpl.createSuccess(test1);

        {
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
            //returning null from command
            assertTrue(interpreterResult.isSuccess());
            assertTrue(interpreterResult instanceof NodeExecutorResult);
            NodeExecutorResult result = (NodeExecutorResult) interpreterResult;
            assertEquals(0, result.getResultCode());
            assertEquals(test1, result.getNode());

//            assertEquals(context, testexec.testContext);
            assertTrue(Arrays.deepEquals(strings, testexec.testCommand));
            assertEquals(test1, testexec.testNode);
        }

    }

    public void testIgnoreErrorOnErrorHandlerWithKeepgoing() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ExecNodeStepExecutor interpret = new ExecNodeStepExecutor(frameworkInstance);
        ExecutionListener listener = new DummyExecutionListener();

        //setup nodeexecutor for local node
        testNodeExecutor testexec = new testNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost", "test1");
        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .threadCount(1)
                .executionListener(listener)
                .build();
        context.getExecutionListener();
        final String[] strings = {"test", "command"};

        //command with error handler
        ExecCommand command = new ExecCommandBase() {
            public String[] getCommand() {
                return strings;
            }
            @Override
            public StepExecutionItem getFailureHandler() {
                return new HandlerExecutionItem() {
                    @Override
                    public boolean isKeepgoingOnSuccess() {
                        return true;
                    }
                    @Override
                    public String getType() {
                        return null;
                    }
                    @Override
                    public String getLabel() {
                        return null;
                    }
                };
            }
        };
        {
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
            assertTrue(((DummyExecutionListener)context.getExecutionListener()).isIgnoreErr());
        }

    }

    public void testNotIgnoreErrorOnErrorHandlerWithKeepgoingFalse() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ExecNodeStepExecutor interpret = new ExecNodeStepExecutor(frameworkInstance);
        ExecutionListener listener = new DummyExecutionListener();

        //setup nodeexecutor for local node
        testNodeExecutor testexec = new testNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost", "test1");
        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .threadCount(1)
                .executionListener(listener)
                .build();
        context.getExecutionListener();
        final String[] strings = {"test", "command"};

        //command with error handler
        ExecCommand command = new ExecCommandBase() {
            public String[] getCommand() {
                return strings;
            }
            @Override
            public StepExecutionItem getFailureHandler() {
                return new HandlerExecutionItem() {
                    @Override
                    public boolean isKeepgoingOnSuccess() {
                        return false;
                    }
                    @Override
                    public String getType() {
                        return null;
                    }
                    @Override
                    public String getLabel() {
                        return null;
                    }
                };
            }
        };
        {
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
            assertFalse(((DummyExecutionListener)context.getExecutionListener()).isIgnoreErr());
        }

    }

    public void testNotIgnoreErrorWithoutErrorHandler() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ExecNodeStepExecutor interpret = new ExecNodeStepExecutor(frameworkInstance);
        ExecutionListener listener = new DummyExecutionListener();

        //setup nodeexecutor for local node
        testNodeExecutor testexec = new testNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost", "test1");
        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(PROJ_NAME)
                .framework(frameworkInstance)
                .user("blah")
                .threadCount(1)
                .executionListener(listener)
                .build();
        context.getExecutionListener();
        final String[] strings = {"test", "command"};

        //command with error handler
        ExecCommand command = new ExecCommandBase() {
            public String[] getCommand() {
                return strings;
            }
        };
        {
            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);
            assertFalse(((DummyExecutionListener)context.getExecutionListener()).isIgnoreErr());
        }

    }

    class DummyExecutionListener implements ExecutionListener {
        boolean ignoreErr = false;
        @Override
        public void ignoreErrors(boolean ignore) {
            ignoreErr=ignore;
        }
        @Override
        public boolean isTerse() {
            return false;
        }

        @Override
        public String getLogFormat() {
            return null;
        }

        @Override
        public void log(int level, String message) {

        }

        @Override
        public void event(String eventType, String message, Map eventMeta) {

        }

        @Override
        public FailedNodesListener getFailedNodesListener() {
            return null;
        }

        @Override
        public void beginNodeExecution(ExecutionContext context, String[] command, INodeEntry node) {

        }

        @Override
        public void finishNodeExecution(NodeExecutorResult result, ExecutionContext context, String[] command, INodeEntry node) {

        }

        @Override
        public void beginNodeDispatch(ExecutionContext context, StepExecutionItem item) {

        }

        @Override
        public void beginNodeDispatch(ExecutionContext context, Dispatchable item) {

        }

        @Override
        public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, StepExecutionItem item) {

        }

        @Override
        public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, Dispatchable item) {

        }

        @Override
        public void beginFileCopyFileStream(ExecutionContext context, InputStream input, INodeEntry node) {

        }

        @Override
        public void beginFileCopyFile(ExecutionContext context, File input, INodeEntry node) {

        }

        @Override
        public void beginFileCopyFile(ExecutionContext context, List<File> files, INodeEntry node) {

        }

        @Override
        public void beginFileCopyScriptContent(ExecutionContext context, String input, INodeEntry node) {

        }

        @Override
        public void finishFileCopy(String result, ExecutionContext context, INodeEntry node) {

        }

        @Override
        public void finishMultiFileCopy(String[] result, ExecutionContext context, INodeEntry node) {

        }

        @Override
        public ExecutionListenerOverride createOverride() {
            return null;
        }
        public boolean isIgnoreErr(){return ignoreErr;}
    }

}
