/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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

package com.dtolabs.rundeck.execution;
/*
* WorkflowActionTests.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 19, 2010 11:24:30 AM
* $Id$
*/

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.Nodes;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.utils.NodeSet;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.CoreException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.tools.ant.BuildListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WorkflowActionTests extends TestCase {
    WorkflowAction workflowAction;
    Framework testFramework;
    private static final String TEST_PROJECT = "TestProject";

    public WorkflowActionTests(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WorkflowActionTests.class);
    }

    protected void setUp() throws Exception {
        File rdeckbase = new File("target/test-rdeck-base");
        rdeckbase.mkdirs();
        new File(rdeckbase, "projects").mkdir();
        new File(rdeckbase, "etc").mkdir();
        File props = new File(rdeckbase, "etc/framework.properties");
        Properties fwkprops=new Properties();
        final File rdeckhome = new File("target/test-rdeck-home");
        rdeckhome.mkdir();
        new File(rdeckhome, "lib/extensions").mkdirs();
        fwkprops.setProperty("rdeck.home", rdeckhome.getAbsolutePath());
        fwkprops.setProperty("ant.home", new File(rdeckhome, "ant-home").getAbsolutePath());
        fwkprops.setProperty("framework.authentication.class", "com.dtolabs.rundeck.core.authentication.NoAuthentication");
        fwkprops.setProperty("framework.authorization.class", "com.dtolabs.rundeck.core.authorization.NoAuthorization");
        fwkprops.setProperty("framework.centraldispatcher.classname", "com.dtolabs.rundeck.core.dispatcher.NoCentralDispatcher");
        fwkprops.setProperty("framework.node.name", "testnode1");
        fwkprops.setProperty("framework.resources.file.name", "resources.xml");
        fwkprops.setProperty("framework.nodes.file.name", "resources.xml");
        final FileOutputStream fileOutputStream = new FileOutputStream(props);
        fwkprops.store(fileOutputStream, "test");
        fileOutputStream.flush();
        fileOutputStream.close();

        testFramework = Framework.getInstance("target/test-rdeck-base");

        //create test depot
        File projprops = new File(rdeckbase, "etc/project.properties");
        projprops.createNewFile();
        new File(rdeckbase, "projects/" + TEST_PROJECT + "/etc").mkdirs();
        File projectNodes = new File(rdeckbase, "projects/" + TEST_PROJECT + "/etc/resources.xml");
        final File testnodes = new File("test/unit/com/dtolabs/rundeck/execution/test-nodes.xml");
        FileUtils.copyFileStreams(testnodes, projectNodes);
        final FrameworkProject frameworkProject = testFramework.getFrameworkProjectMgr().createFrameworkProject(
            TEST_PROJECT);
        //create resources.xml with node data

    }

    protected void tearDown() throws Exception {
        File rdeckbase = new File("target/test-rdeck-base");
        rdeckbase.delete();
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    static class testExecutionService implements ExecutionService {
        List<ExecutionItem> executionItemList = new ArrayList<ExecutionItem>();
        int count = 0;


        public ExecutionResult executeItem(ExecutionItem executionItem) throws ExecutionException {
            this.executionItemList.add(executionItem);
            count++;
            return BaseExecutionResult.createSuccess("TestResult" + count);
        }
    }

    static class failExecutor implements Executor {
        ExecutionItem item;
        ExecutionListener listener;
        ExecutionService service;
        Framework framework;

        public ExecutionResult executeItem(ExecutionItem item, ExecutionListener listener, ExecutionService service,
                                           Framework framework) {
            this.item = item;
            this.listener = listener;
            this.service = service;
            this.framework = framework;
            return BaseExecutionResult.createFailure(new Exception("failed"));
        }
    }

    static interface testExecutionItem extends ExecutionItem {

    }

    static class testWorkflowCmdItem implements IWorkflowCmdItem {
        String returnProperty;
        String ifString;
        String unlessString;
        String equalsString;
        String project;
        String argString;
        String user;
        Boolean nodeKeepgoing = Boolean.FALSE;
        Integer nodeThreadcount;
        String adhocRemoteString;
        String adhocLocalString;
        String adhocFilepath;
        Boolean adhocExecution = Boolean.FALSE;

        public String getReturnProperty() {
            return returnProperty;
        }

        public String getIfString() {
            return ifString;
        }

        public String getUnlessString() {
            return unlessString;
        }

        public String getEqualsString() {
            return equalsString;
        }

        public String getProject() {
            return project;
        }

        public String getArgString() {
            return argString;
        }

        public String getUser() {
            return user;
        }

        public Boolean getNodeKeepgoing() {
            return nodeKeepgoing;
        }

        public Integer getNodeThreadcount() {
            return nodeThreadcount;
        }

        public String getAdhocRemoteString() {
            return adhocRemoteString;
        }

        public String getAdhocLocalString() {
            return adhocLocalString;
        }

        public String getAdhocFilepath() {
            return adhocFilepath;
        }

        public Boolean getAdhocExecution() {
            return adhocExecution;
        }

        @Override
        public String toString() {
            return "testWorkflowCmdItem{" +
                   "returnProperty='" + returnProperty + '\'' +
                   ", ifString='" + ifString + '\'' +
                   ", unlessString='" + unlessString + '\'' +
                   ", equalsString='" + equalsString + '\'' +
                   ", project='" + project + '\'' +
                   ", argString='" + argString + '\'' +
                   ", user='" + user + '\'' +
                   ", nodeKeepgoing=" + nodeKeepgoing +
                   ", nodeThreadcount=" + nodeThreadcount +
                   ", adhocRemoteString='" + adhocRemoteString + '\'' +
                   ", adhocLocalString='" + adhocLocalString + '\'' +
                   ", adhocFilepath='" + adhocFilepath + '\'' +
                   ", adhocExecution=" + adhocExecution +
                   '}';
        }
    }

    static class testWorkflowJobCmdItem extends testWorkflowCmdItem implements IWorkflowJobItem {
        private String jobIdentifier;

        public String getJobIdentifier() {
            return jobIdentifier;
        }
    }

    public void testExecuteWorkflow() throws Exception {
        {
            assertEquals("testnode1", testFramework.getFrameworkNodeName());
            final FrameworkProject frameworkProject = testFramework.getFrameworkProjectMgr().getFrameworkProject(
                TEST_PROJECT);
            final Nodes nodes = frameworkProject.getNodes();
            assertNotNull(nodes);
            assertEquals(2, nodes.countNodes());

        }

        {
            //test empty workflow
            testExecutionService executionService = new testExecutionService();
            final NodeSet nodeset = new NodeSet();
            final WorkflowImpl workflow = new WorkflowImpl(new ArrayList<IWorkflowCmdItem>(), 1, false, WorkflowAction.NODE_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "user1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(0, executionService.executionItemList.size());
        }
        {
            //test empty workflow
            testExecutionService executionService = new testExecutionService();
            final NodeSet nodeset = new NodeSet();
            final WorkflowImpl workflow = new WorkflowImpl(new ArrayList<IWorkflowCmdItem>(), 1, false, WorkflowAction.STEP_FIRST);
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "user1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            if (!result.isSuccess() && null != result.getException()) {
                result.getException().printStackTrace(System.err);
            }
            assertNull("threw exception: " + result.getException(), result.getException());
            assertTrue(result.isSuccess());
            assertEquals(0, executionService.executionItemList.size());
        }

        {
            //test item with undefined content
            testExecutionService executionService = new testExecutionService();
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();

            workflowCmdItemArrayList.add(new testWorkflowCmdItem());

            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, false, WorkflowAction.NODE_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "user1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertEquals(0, executionService.executionItemList.size());
            final Exception thrown = result.getException();
            if (!result.isSuccess() && null != thrown) {
                thrown.printStackTrace(System.err);
            }
            assertFalse("should have failed: " + result, result.isSuccess());
            assertNotNull("should have failed: " + result, thrown);
            assertTrue("unexpected exception type: " + thrown.getClass().getName(),
                thrown instanceof CoreException);
            Throwable cause1 = thrown.getCause();
            assertTrue("unexpected exception type: " + cause1.getClass().getName(),
                cause1 instanceof WorkflowAction.WorkflowStepFailureException);
            assertTrue("unexpected cause exception type: " + cause1.getCause(),
                cause1.getCause() instanceof IllegalArgumentException);
        }
        {
            //test item with undefined content
            testExecutionService executionService = new testExecutionService();
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();

            workflowCmdItemArrayList.add(new testWorkflowCmdItem());

            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, false, WorkflowAction.STEP_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "user1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());
            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertEquals(0, executionService.executionItemList.size());
            assertFalse("should have failed: " + result, result.isSuccess());
            assertNotNull("should have failed: " + result, result.getException());
            assertTrue("unexpected exception type" + result.getException(),
                result.getException() instanceof WorkflowAction.WorkflowStepFailureException);
            assertTrue("unexpected cause exception type: " + result.getException().getCause(),
                result.getException().getCause() instanceof IllegalArgumentException);
        }



        {
            //test item with simple script exec content
            testExecutionService executionService = new testExecutionService();
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();

            final testWorkflowCmdItem testWorkflowCmdItem = new testWorkflowCmdItem();
            testWorkflowCmdItem.adhocExecution = true;
            testWorkflowCmdItem.adhocRemoteString = "a command";
            testWorkflowCmdItem.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItem);

            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, false, WorkflowAction.STEP_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertTrue("should have succeeded: " + result, result.isSuccess());
            assertTrue("incorrect result" + result, result.getResultObject() instanceof List);
            List l = (List) result.getResultObject();
            assertEquals("incorrect result" + result, "TestResult1", l.get(0));
            assertNull("should not have failed" + result, result.getException());
            assertEquals(1, executionService.executionItemList.size());
            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem eitem = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(0);
            assertNotNull("wrong exec item property", eitem.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, eitem.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", eitem.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command", eitem.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", eitem.getDispatchedScript().getFrameworkProject());

        }
        {
            //test item with simple script exec content
            testExecutionService executionService = new testExecutionService();
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();

            final testWorkflowCmdItem testWorkflowCmdItem = new testWorkflowCmdItem();
            testWorkflowCmdItem.adhocExecution = true;
            testWorkflowCmdItem.adhocRemoteString = "a command";
            testWorkflowCmdItem.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItem);

            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, false, WorkflowAction.NODE_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertTrue("should have succeeded: " + result, result.isSuccess());
            assertTrue("incorrect result" + result, result.getResultObject() instanceof List);
            List l = (List) result.getResultObject();
            assertEquals("incorrect result" + result, "TestResult1", l.get(0));
            assertNull("should not have failed" + result, result.getException());
            assertEquals(1, executionService.executionItemList.size());
            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem eitem = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(0);
            assertNotNull("wrong exec item property", eitem.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, eitem.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", eitem.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command", eitem.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", eitem.getDispatchedScript().getFrameworkProject());

        }

        {
            //test item with simple job exec content
            testExecutionService executionService = new testExecutionService();
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();

            final testWorkflowJobCmdItem testWorkflowCmdItem = new testWorkflowJobCmdItem();
            testWorkflowCmdItem.jobIdentifier = "/some/job/name";
            workflowCmdItemArrayList.add(testWorkflowCmdItem);

            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, false, WorkflowAction.STEP_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertTrue("should have succeeded: " + result, result.isSuccess());
            assertTrue("incorrect result" + result, result.getResultObject() instanceof List);
            List l = (List) result.getResultObject();
            assertEquals("incorrect result" + result, "TestResult1", l.get(0));
            assertNull("should not have failed" + result, result.getException());
            assertEquals(1, executionService.executionItemList.size());
            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof JobExecutionItem);
            JobExecutionItem eitem = (JobExecutionItem) executionService.executionItemList.get(0);
            assertEquals("wrong exec item property", "/some/job/name", eitem.getJobIdentifier());

        }

        {
            //test item with simple job exec content
            testExecutionService executionService = new testExecutionService();
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();

            final testWorkflowJobCmdItem testWorkflowCmdItem = new testWorkflowJobCmdItem();
            testWorkflowCmdItem.jobIdentifier = "/some/job/name";
            workflowCmdItemArrayList.add(testWorkflowCmdItem);

            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, false, WorkflowAction.NODE_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertTrue("should have succeeded: " + result, result.isSuccess());
            assertTrue("incorrect result" + result, result.getResultObject() instanceof List);
            List l = (List) result.getResultObject();
            assertEquals("incorrect result" + result, "TestResult1", l.get(0));
            assertNull("should not have failed" + result, result.getException());
            assertEquals(1, executionService.executionItemList.size());
            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof JobExecutionItem);
            JobExecutionItem eitem = (JobExecutionItem) executionService.executionItemList.get(0);
            assertEquals("wrong exec item property", "/some/job/name", eitem.getJobIdentifier());

        }


        {
            //test a workflow of three successful items.
            testExecutionService executionService = new testExecutionService();
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();
            final testWorkflowCmdItem testWorkflowCmdItemCmd = new testWorkflowCmdItem();
            testWorkflowCmdItemCmd.adhocExecution = true;
            testWorkflowCmdItemCmd.adhocRemoteString = "a 2 command";
            testWorkflowCmdItemCmd.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemCmd);

            final testWorkflowCmdItem testWorkflowCmdItemScript = new testWorkflowCmdItem();
            testWorkflowCmdItemScript.adhocExecution = true;
            testWorkflowCmdItemScript.adhocRemoteString = "a command";
            testWorkflowCmdItemScript.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript);

            final testWorkflowJobCmdItem testWorkflowCmdItemJob = new testWorkflowJobCmdItem();
            testWorkflowCmdItemJob.jobIdentifier = "/some/job/name";
            workflowCmdItemArrayList.add(testWorkflowCmdItemJob);

            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, false, WorkflowAction.STEP_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertTrue("should have succeeded: " + result, result.isSuccess());
            assertTrue("incorrect result" + result, result.getResultObject() instanceof List);
            List l = (List) result.getResultObject();
            assertEquals(3, l.size());
            assertEquals("incorrect result" + result, "TestResult1", l.get(0));
            assertEquals("incorrect result" + result, "TestResult2", l.get(1));
            assertEquals("incorrect result" + result, "TestResult3", l.get(2));
            assertNull("should not have failed" + result, result.getException());
            assertEquals(3, executionService.executionItemList.size());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem1 = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(0);
            assertNotNull("wrong exec item property", scptitem1.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 3, scptitem1.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem1.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "2", scptitem1.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "command", scptitem1.getDispatchedScript().getArgs()[2]);
            assertEquals("wrong exec item property", "aProject", scptitem1.getDispatchedScript().getFrameworkProject());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(1) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(1);
            assertNotNull("wrong exec item property", scptitem.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command", scptitem.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", scptitem.getDispatchedScript().getFrameworkProject());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(2) instanceof JobExecutionItem);
            JobExecutionItem jobitem = (JobExecutionItem) executionService.executionItemList.get(2);
            assertEquals("wrong exec item property", "/some/job/name", jobitem.getJobIdentifier());

        }
        {
            //test a workflow of two successful items.
            testExecutionService executionService = new testExecutionService();
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();

            final testWorkflowCmdItem testWorkflowCmdItemScript = new testWorkflowCmdItem();
            testWorkflowCmdItemScript.adhocExecution = true;
            testWorkflowCmdItemScript.adhocRemoteString = "a command";
            testWorkflowCmdItemScript.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript);

            final testWorkflowJobCmdItem testWorkflowCmdItemJob = new testWorkflowJobCmdItem();
            testWorkflowCmdItemJob.jobIdentifier = "/some/job/name";
            workflowCmdItemArrayList.add(testWorkflowCmdItemJob);

            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, false, WorkflowAction.NODE_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertTrue("should have succeeded: " + result, result.isSuccess());
            assertTrue("incorrect result" + result, result.getResultObject() instanceof List);
            List l = (List) result.getResultObject();
            assertEquals(2, l.size());
            assertEquals("incorrect result" + result, "TestResult1", l.get(0));
            assertEquals("incorrect result" + result, "TestResult2", l.get(1));
            assertNull("should not have failed" + result, result.getException());
            assertEquals(2, executionService.executionItemList.size());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(0);
            assertNotNull("wrong exec item property", scptitem.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command", scptitem.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", scptitem.getDispatchedScript().getFrameworkProject());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(1) instanceof JobExecutionItem);
            JobExecutionItem jobitem = (JobExecutionItem) executionService.executionItemList.get(1);
            assertEquals("wrong exec item property", "/some/job/name", jobitem.getJobIdentifier());

        }
        {
            //test a workflow with a failing item (1), with keepgoing=false
            final boolean testKeepgoing = false;
            testExecutionService executionService = new testExecutionService() {
                @Override
                public ExecutionResult executeItem(ExecutionItem executionItem) throws ExecutionException {
                    ExecutionResult result = super.executeItem(executionItem);
                    if (1 == count) {
                        throw new ExecutionException("Test Failure 1");
                    }
                    return result;
                }
            };
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();

            final testWorkflowCmdItem testWorkflowCmdItemScript = new testWorkflowCmdItem();
            testWorkflowCmdItemScript.adhocExecution = true;
            testWorkflowCmdItemScript.adhocRemoteString = "a command";
            testWorkflowCmdItemScript.project = TEST_PROJECT;
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript);

            final testWorkflowJobCmdItem testWorkflowCmdItemJob = new testWorkflowJobCmdItem();
            testWorkflowCmdItemJob.jobIdentifier = "/some/job/name";
            workflowCmdItemArrayList.add(testWorkflowCmdItemJob);


            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, testKeepgoing,
                WorkflowAction.NODE_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertFalse("should have failed: " + result, result.isSuccess());
            assertNull("incorrect result" + result, result.getResultObject());
            final Exception thrown = result.getException();
            assertNotNull("expected exception" + result, thrown);
            assertTrue("expected exception" + result,
                thrown instanceof CoreException);
            Throwable cause1 = thrown.getCause();
            assertTrue("expected exception" + result,
                cause1 instanceof WorkflowAction.WorkflowStepFailureException);
            assertTrue("expected exception" + result, cause1.getCause() instanceof ExecutionException);
            assertEquals("unexpected exception message" + result, "Test Failure 1",
                cause1.getCause().getMessage());


            assertEquals(1, executionService.executionItemList.size());
            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem =
                (DispatchedScriptExecutionItem) executionService.executionItemList.get(0);
            assertEquals("wrong exec item property", TEST_PROJECT,
                scptitem.getDispatchedScript().getFrameworkProject());
            assertNotNull("wrong exec item property", scptitem.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command", scptitem.getDispatchedScript().getArgs()[1]);

        }

        {
            //test a workflow with a failing item (2), with keepgoing=false
            final boolean testKeepgoing = false;
            testExecutionService executionService = new testExecutionService() {
                @Override
                public ExecutionResult executeItem(ExecutionItem executionItem) throws ExecutionException {
                    ExecutionResult result = super.executeItem(executionItem);
                    if (2 == count) {
                        throw new ExecutionException("Test Failure " + count);
                    }
                    return result;

                }
            };
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();

            final testWorkflowCmdItem testWorkflowCmdItemScript = new testWorkflowCmdItem();
            testWorkflowCmdItemScript.adhocExecution = true;
            testWorkflowCmdItemScript.adhocRemoteString = "a command";
            testWorkflowCmdItemScript.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript);

            final testWorkflowCmdItem testWorkflowCmdItemScript2 = new testWorkflowCmdItem();
            testWorkflowCmdItemScript2.adhocExecution = true;
            testWorkflowCmdItemScript2.adhocRemoteString = "a command2";
            testWorkflowCmdItemScript2.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript2);


            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, testKeepgoing,
                WorkflowAction.STEP_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertFalse("should have failed: " + result, result.isSuccess());
            assertNull("incorrect result" + result, result.getResultObject());
            assertNotNull("expected exception" + result, result.getException());
            assertTrue("expected exception" + result,
                result.getException() instanceof WorkflowAction.WorkflowStepFailureException);
            assertTrue("expected exception" + result, result.getException().getCause() instanceof ExecutionException);
            assertEquals("unexpected exception message" + result, "Test Failure 2",
                result.getException().getCause().getMessage());


            assertEquals(2, executionService.executionItemList.size());


            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(0);
            assertNotNull("wrong exec item property", scptitem.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command", scptitem.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", scptitem.getDispatchedScript().getFrameworkProject());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(1) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem2 = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(1);
            assertNotNull("wrong exec item property", scptitem2.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem2.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem2.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command2", scptitem2.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", scptitem2.getDispatchedScript().getFrameworkProject());

        }
        {
            //test a workflow with a failing item (2), with keepgoing=false (NODEFIRST)
            final boolean testKeepgoing = false;
            testExecutionService executionService = new testExecutionService() {
                @Override
                public ExecutionResult executeItem(ExecutionItem executionItem) throws ExecutionException {
                    ExecutionResult result = super.executeItem(executionItem);
                    if (2 == count) {
                        throw new ExecutionException("Test Failure " + count);
                    }
                    return result;

                }
            };
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();


            final testWorkflowCmdItem testWorkflowCmdItemScript = new testWorkflowCmdItem();
            testWorkflowCmdItemScript.adhocExecution = true;
            testWorkflowCmdItemScript.adhocRemoteString = "a command";
            testWorkflowCmdItemScript.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript);

            final testWorkflowCmdItem testWorkflowCmdItemScript2 = new testWorkflowCmdItem();
            testWorkflowCmdItemScript2.adhocExecution = true;
            testWorkflowCmdItemScript2.adhocRemoteString = "a command2";
            testWorkflowCmdItemScript2.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript2);

            final testWorkflowJobCmdItem testWorkflowCmdItemJob = new testWorkflowJobCmdItem();
            testWorkflowCmdItemJob.jobIdentifier = "/some/job/name";
            workflowCmdItemArrayList.add(testWorkflowCmdItemJob);


            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, testKeepgoing,
                WorkflowAction.NODE_FIRST);
            assertFalse(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertFalse("should have failed: " + result, result.isSuccess());
            assertNull("incorrect result" + result, result.getResultObject());
            final Exception thrown = result.getException();
            assertNotNull("expected exception" + result, thrown);
            assertTrue("expected exception" + result,
                thrown instanceof CoreException);
            Throwable cause1 = thrown.getCause();
            assertTrue("expected exception" + result,
                cause1 instanceof WorkflowAction.WorkflowStepFailureException);
            assertTrue("expected exception" + result, cause1.getCause() instanceof ExecutionException);
            assertEquals("unexpected exception message" + result, "Test Failure 2",
                cause1.getCause().getMessage());


            assertEquals(2, executionService.executionItemList.size());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(0);
            assertNotNull("wrong exec item property", scptitem.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command", scptitem.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", scptitem.getDispatchedScript().getFrameworkProject());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(1) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem2 = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(1);
            assertNotNull("wrong exec item property", scptitem2.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem2.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem2.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command2", scptitem2.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", scptitem2.getDispatchedScript().getFrameworkProject());

        }

        {
            //test a workflow with a failing item (2), with keepgoing=true
            final boolean testKeepgoing = true;
            testExecutionService executionService = new testExecutionService() {
                @Override
                public ExecutionResult executeItem(ExecutionItem executionItem) throws ExecutionException {
                    ExecutionResult result = super.executeItem(executionItem);
                    if (2 == count) {
                        throw new ExecutionException("Test Failure " + count);
                    }
                    return result;

                }
            };
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();
            final testWorkflowCmdItem testWorkflowCmdItemScript = new testWorkflowCmdItem();
            testWorkflowCmdItemScript.adhocExecution = true;
            testWorkflowCmdItemScript.adhocRemoteString = "a command";
            testWorkflowCmdItemScript.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript);

            final testWorkflowCmdItem testWorkflowCmdItemScript2 = new testWorkflowCmdItem();
            testWorkflowCmdItemScript2.adhocExecution = true;
            testWorkflowCmdItemScript2.adhocRemoteString = "a command2";
            testWorkflowCmdItemScript2.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript2);

            final testWorkflowJobCmdItem testWorkflowCmdItemJob = new testWorkflowJobCmdItem();
            testWorkflowCmdItemJob.jobIdentifier = "/some/job/name";
            workflowCmdItemArrayList.add(testWorkflowCmdItemJob);


            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, testKeepgoing,
                WorkflowAction.STEP_FIRST);
            assertTrue(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertFalse("should have failed: " + result, result.isSuccess());
            assertNull("incorrect result" + result, result.getResultObject());
            assertNotNull("expected exception" + result, result.getException());
            assertTrue("expected exception" + result,
                result.getException() instanceof WorkflowAction.WorkflowFailureException);
            assertTrue("unexpected exception message" + result, result.getException().getMessage().startsWith(
                "Some steps in the workflow failed: "));


            assertEquals(3, executionService.executionItemList.size());


            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(0);
            assertNotNull("wrong exec item property", scptitem.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command", scptitem.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", scptitem.getDispatchedScript().getFrameworkProject());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(1) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem2 = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(1);
            assertNotNull("wrong exec item property", scptitem2.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem2.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem2.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command2", scptitem2.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", scptitem2.getDispatchedScript().getFrameworkProject());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(2) instanceof JobExecutionItem);
            JobExecutionItem jobitem = (JobExecutionItem) executionService.executionItemList.get(2);
            assertEquals("wrong exec item property", "/some/job/name", jobitem.getJobIdentifier());
        }
        {
            //test a workflow with a failing item (2), with keepgoing=true (NODEFIRST)
            final boolean testKeepgoing = true;
            testExecutionService executionService = new testExecutionService() {
                @Override
                public ExecutionResult executeItem(ExecutionItem executionItem) throws ExecutionException {
                    ExecutionResult result = super.executeItem(executionItem);
                    if (2 == count) {
                        throw new ExecutionException("Test Failure " + count);
                    }
                    return result;

                }
            };
            final NodeSet nodeset = new NodeSet();
            final ArrayList<IWorkflowCmdItem> workflowCmdItemArrayList = new ArrayList<IWorkflowCmdItem>();
            final testWorkflowCmdItem testWorkflowCmdItemScript = new testWorkflowCmdItem();
            testWorkflowCmdItemScript.adhocExecution = true;
            testWorkflowCmdItemScript.adhocRemoteString = "a command";
            testWorkflowCmdItemScript.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript);

            final testWorkflowCmdItem testWorkflowCmdItemScript2 = new testWorkflowCmdItem();
            testWorkflowCmdItemScript2.adhocExecution = true;
            testWorkflowCmdItemScript2.adhocRemoteString = "a command2";
            testWorkflowCmdItemScript2.project = "aProject";
            workflowCmdItemArrayList.add(testWorkflowCmdItemScript2);

            final testWorkflowJobCmdItem testWorkflowCmdItemJob = new testWorkflowJobCmdItem();
            testWorkflowCmdItemJob.jobIdentifier = "/some/job/name";
            workflowCmdItemArrayList.add(testWorkflowCmdItemJob);


            final WorkflowImpl workflow = new WorkflowImpl(workflowCmdItemArrayList, 1, testKeepgoing,
                WorkflowAction.NODE_FIRST);
            assertTrue(workflow.isKeepgoing());
            final WorkflowExecutionItemImpl executionItem = new WorkflowExecutionItemImpl(workflow,
                nodeset,
                "User1", 1, TEST_PROJECT, null);
            workflowAction = new WorkflowAction(testFramework, executionService, executionItem, new testListener());

            final ExecutionResult result = workflowAction.executeWorkflow();
            assertNotNull(result);
            assertFalse("should have failed: " + result, result.isSuccess());
            assertNull("incorrect result" + result, result.getResultObject());
            final Exception thrown = result.getException();
            assertNotNull("expected exception" + result, thrown);
            assertTrue("expected exception" + result,
                thrown instanceof CoreException);
            Throwable cause1 = thrown.getCause();
            assertTrue("expected exception" + result,
                cause1 instanceof WorkflowAction.WorkflowFailureException);
            assertTrue("unexpected exception message" + result, cause1.getMessage().startsWith(
                "Some steps in the workflow failed: "));


            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(0) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(0);
            assertNotNull("wrong exec item property", scptitem.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command", scptitem.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", scptitem.getDispatchedScript().getFrameworkProject());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(1) instanceof DispatchedScriptExecutionItem);
            DispatchedScriptExecutionItem scptitem2 = (DispatchedScriptExecutionItem) executionService.executionItemList
                .get(1);
            assertNotNull("wrong exec item property", scptitem2.getDispatchedScript().getArgs());
            assertEquals("wrong exec item property", 2, scptitem2.getDispatchedScript().getArgs().length);
            assertEquals("wrong exec item property", "a", scptitem2.getDispatchedScript().getArgs()[0]);
            assertEquals("wrong exec item property", "command2", scptitem2.getDispatchedScript().getArgs()[1]);
            assertEquals("wrong exec item property", "aProject", scptitem2.getDispatchedScript().getFrameworkProject());

            assertTrue("wrong exec item type for workflow item",
                executionService.executionItemList.get(2) instanceof JobExecutionItem);
            JobExecutionItem jobitem = (JobExecutionItem) executionService.executionItemList.get(2);
            assertEquals("wrong exec item property", "/some/job/name", jobitem.getJobIdentifier());
        }
    }

    static class testListener implements ExecutionListener {
        public void log(int i, String s) {
            System.err.println(i + ": " + s);
        }

        public FailedNodesListener getFailedNodesListener() {
            return null;
        }

        public BuildListener getBuildListener() {
            return null;
        }
    }


}