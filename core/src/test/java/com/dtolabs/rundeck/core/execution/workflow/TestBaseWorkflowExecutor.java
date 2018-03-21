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
* TestBaseWorkflowStrategy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 9/11/12 2:15 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResultImpl;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.*;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommand;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandBase;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;
import org.apache.tools.ant.BuildListener;

import java.io.File;
import java.io.InputStream;
import java.util.*;


/**
 * TestBaseWorkflowStrategy is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestBaseWorkflowExecutor extends AbstractBaseTest {

    public static final String TEST_PROJECT = "TestBaseWorkflowStrategy";
    Framework testFramework;
    String testnode;

    public TestBaseWorkflowExecutor(String name) {
        super(name);
    }

    protected void setUp() {
        super.setUp();
        testFramework = getFrameworkInstance();
        testnode = testFramework.getFrameworkNodeName();
        final IRundeckProject frameworkProject = testFramework.getFrameworkProjectMgr().createFrameworkProject(
            TEST_PROJECT);
        generateProjectResourcesFile(
                new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                frameworkProject
        );

    }

    protected void tearDown() throws Exception {
        super.tearDown();

    }

    public void testIgnoreErrorOnErrorHandlerWithKeepgoing() throws Exception {
        BaseWorkflowExecutor instance = new BaseWorkflowExecutor(testFramework) {
            @Override
            public WorkflowExecutionResult executeWorkflowImpl(StepExecutionContext executionContext, WorkflowExecutionItem item) {
                return null;
            }
        };
        ExecutionListener listener = new DummyExecutionListener();
        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(TEST_PROJECT)
                .framework(testFramework)
                .executionListener(listener)
                .user("blah")
                .threadCount(1)
                .build();
        final String[] strings = {"test", "command"};
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


        instance.executeWFItem(context,new HashMap<Integer, StepExecutionResult>(),0,command);
        assertTrue((((DummyExecutionListener)context.getExecutionListener()).isIgnoreErr()));
    }

    public void testNoIgnoreErrorOnErrorHandlerWithKeepgoingFalse() throws Exception {
        BaseWorkflowExecutor instance = new BaseWorkflowExecutor(testFramework) {
            @Override
            public WorkflowExecutionResult executeWorkflowImpl(StepExecutionContext executionContext, WorkflowExecutionItem item) {
                return null;
            }
        };
        ExecutionListener listener = new DummyExecutionListener();
        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(TEST_PROJECT)
                .framework(testFramework)
                .executionListener(listener)
                .user("blah")
                .threadCount(1)
                .build();
        final String[] strings = {"test", "command"};
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


        instance.executeWFItem(context,new HashMap<Integer, StepExecutionResult>(),0,command);
        assertFalse((((DummyExecutionListener)context.getExecutionListener()).isIgnoreErr()));
    }

    public void testNoIgnoreErrorWithoutHandler() throws Exception {
        BaseWorkflowExecutor instance = new BaseWorkflowExecutor(testFramework) {
            @Override
            public WorkflowExecutionResult executeWorkflowImpl(StepExecutionContext executionContext, WorkflowExecutionItem item) {
                return null;
            }
        };
        ExecutionListener listener = new DummyExecutionListener();
        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(TEST_PROJECT)
                .framework(testFramework)
                .executionListener(listener)
                .user("blah")
                .threadCount(1)
                .build();
        final String[] strings = {"test", "command"};
        ExecCommand command = new ExecCommandBase() {
            public String[] getCommand() {

                return strings;
            }
        };


        instance.executeWFItem(context,new HashMap<Integer, StepExecutionResult>(),0,command);
        assertFalse((((DummyExecutionListener)context.getExecutionListener()).isIgnoreErr()));
    }

    class DummyExecutionListener implements ExecutionListener {
        boolean ignoreErr = false;
        @Override
        public void ignoreErrors(boolean ignore) {
            ignoreErr=ignore;
        }

        @Override
        public void log(int level, String message) {

        }

        @Override
        public void log(final int level, final String message, final Map eventMeta) {

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
