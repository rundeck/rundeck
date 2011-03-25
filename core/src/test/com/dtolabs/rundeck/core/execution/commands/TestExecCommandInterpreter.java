/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* TestExecCommandInterpreter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 3:19 PM
* 
*/
package com.dtolabs.rundeck.core.execution.commands;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * TestExecCommandInterpreter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestExecCommandInterpreter extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestExecCommandInterpreter";

    public TestExecCommandInterpreter(String name) {
        super(name);
    }

    public void setUp() {
        super.setUp();

        final Framework frameworkInstance = getFrameworkInstance();
        final FrameworkProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
            PROJ_NAME);
        File resourcesfile = new File(frameworkProject.getNodesResourceFilePath());
        //copy test nodes to resources file
        try {
            FileUtils.copyFileStreams(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                resourcesfile);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }

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
        public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) throws
            ExecutionException {
            this.testContext=context;
            this.testCommand=command;
            this.testNode=node;
            return testResult;
        }
    }

    public void testInterpretCommand() throws Exception {
        ExecCommandInterpreter interpret = new ExecCommandInterpreter(getFrameworkInstance());

        //setup nodeexecutor for local node
        testNodeExecutor testexec = new testNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("test1");
        final ExecutionContext context = new ExecutionContext() {
            public String getFrameworkProject() {
                return PROJ_NAME;
            }

            public String getUser() {
                return "blah";
            }

            public NodeSet getNodeSet() {

                return null;
            }

            public String[] getArgs() {
                return new String[0];
            }

            public int getLoglevel() {
                return 0;
            }

            public Map<String, Map<String, String>> getDataContext() {
                return null;
            }

            public ExecutionListener getExecutionListener() {
                return null;
            }
        };
        final String[] strings = {"test", "command"};

        ExecCommand command = new ExecCommand() {
            public String[] getCommand() {

                return strings;
            }
        };
        {
            final InterpreterResult interpreterResult = interpret.interpretCommand(context, command, test1);
            //returning null from command
            assertNull(interpreterResult);
            assertEquals(context, testexec.testContext);
            assertEquals(strings, testexec.testCommand);
            assertEquals(test1, testexec.testNode);
        }
        testexec.testResult=new NodeExecutorResult() {
            public int getResultCode() {
                return -2;
            }

            public boolean isSuccess() {
                return true;
            }
        };

        {
            final InterpreterResult interpreterResult = interpret.interpretCommand(context, command, test1);
            //returning null from command
            assertTrue(interpreterResult.isSuccess());
            assertTrue(interpreterResult instanceof NodeExecutorResult);
            NodeExecutorResult result = (NodeExecutorResult) interpreterResult;
            assertEquals(-2, result.getResultCode());

            assertEquals(context, testexec.testContext);
            assertEquals(strings, testexec.testCommand);
            assertEquals(test1, testexec.testNode);
        }

    }

    /**
     * Test use of remote node that uses jsch-ssh
     */
    public void testInterpretCommandRemote() throws Exception {
        ExecCommandInterpreter interpret = new ExecCommandInterpreter(getFrameworkInstance());

        //setup nodeexecutor for local node
        testNodeExecutor testexec = new testNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("jsch-ssh", testexec);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testnode2");
        final ExecutionContext context = new ExecutionContext() {
            public String getFrameworkProject() {
                return PROJ_NAME;
            }

            public String getUser() {
                return "blah";
            }

            public NodeSet getNodeSet() {

                return null;
            }

            public String[] getArgs() {
                return new String[0];
            }

            public int getLoglevel() {
                return 0;
            }

            public Map<String, Map<String, String>> getDataContext() {
                return null;
            }

            public ExecutionListener getExecutionListener() {
                return null;
            }
        };
        final String[] strings = {"test", "command"};

        ExecCommand command = new ExecCommand() {
            public String[] getCommand() {

                return strings;
            }
        };
        {
            final InterpreterResult interpreterResult = interpret.interpretCommand(context, command, test1);
            //returning null from command
            assertNull(interpreterResult);
            assertEquals(context, testexec.testContext);
            assertEquals(strings, testexec.testCommand);
            assertEquals(test1, testexec.testNode);
        }
        testexec.testResult = new NodeExecutorResult() {
            public int getResultCode() {
                return -2;
            }

            public boolean isSuccess() {
                return true;
            }
        };

        {
            final InterpreterResult interpreterResult = interpret.interpretCommand(context, command, test1);
            //returning null from command
            assertTrue(interpreterResult.isSuccess());
            assertTrue(interpreterResult instanceof NodeExecutorResult);
            NodeExecutorResult result = (NodeExecutorResult) interpreterResult;
            assertEquals(-2, result.getResultCode());

            assertEquals(context, testexec.testContext);
            assertEquals(strings, testexec.testCommand);
            assertEquals(test1, testexec.testNode);
        }

    }

}
