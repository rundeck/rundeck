/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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

package com.dtolabs.rundeck.core.cli;
/*
* TestExecTool.java
* 
* User: alex
* Created: Apr 8, 2008 12:22:33 PM
* $Id$
*/


import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommandExecutionItem;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestExecTool extends AbstractBaseTest {
    ExecTool main;
    /**
     * script file to use to test dispatch -s which requires valid file
     */
    File testScriptFile;
    String[] nodeKeys = {
        "hostname",
        "entity-name",
        "tags",
        "os-name",
        "os-family",
        "os-arch",
        "os-version",
    };
    private static final String TEST_EXEC_TOOL_PROJ2 = "TestExecTool2";
    private static final String TEST_EXEC_TOOL_PROJECT = "TestExecTool";

    public TestExecTool(String name) {
        super(name);
        /**
         * Create a nodes.properties file
         */

    }

    public void setUp() {
        super.setUp();
        FrameworkProject d = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(
            TEST_EXEC_TOOL_PROJECT);
        File projectEtcDir = new File(d.getBaseDir(), "etc");
        //copy test nodes xml file to test dir
        try {
            final File testNodes = new File("src/test/resources/com/dtolabs/rundeck/core/cli/test-dispatch-nodes.xml");
            final File projectNodes = new File(projectEtcDir, "resources.xml");
            FileUtils.copyFileStreams(testNodes, projectNodes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        testScriptFile = new File(d.getBaseDir(), "test.sh");
        try {
            testScriptFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FrameworkProject d2 = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(
            TEST_EXEC_TOOL_PROJ2);

    }

    public void tearDown() throws Exception {
        super.tearDown();
        FrameworkProject d = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(
            TEST_EXEC_TOOL_PROJECT);
        FileUtils.deleteDir(d.getBaseDir());
        getFrameworkInstance().getFrameworkProjectMgr().remove(TEST_EXEC_TOOL_PROJECT);
        FrameworkProject d2 = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(
            TEST_EXEC_TOOL_PROJ2);
        FileUtils.deleteDir(d2.getBaseDir());


        getFrameworkInstance().getFrameworkProjectMgr().remove(TEST_EXEC_TOOL_PROJ2);
//        ExecutionServiceFactory.resetDefaultExecutorClasses();
        getFrameworkInstance().setService(NodeStepExecutionService.SERVICE_NAME, null);
    }

    public static Test suite() {
        return new TestSuite(TestExecTool.class);
    }


    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    public void testParseArgs() throws Exception {

        {
            //test missing -p option when multiple projects exist
            ExecTool main = newExecTool();

            try {
                main.parseArgs(
                    new String[]{"-K", "-C", "2", "-I", "hostname1", "-X", "tags=baloney", "-Q", "--", "shell",
                        "command",
                        "string"});
                fail("should not complete");
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
                assertEquals("project parameter not specified", e.getMessage());
            }
        }
    }

    public void testParseNodeDispatchArgs() throws Exception {
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-I", "tags=priority1"});
            assertNotNull(main.cli);
            assertTrue(main.cli.hasOption("I"));
            assertNotNull(main.cli.getOptionValue("I"));
            assertNotNull(main.cli.getOptionValues("I"));
            assertEquals("tags=priority1", main.cli.getOptionValue("I"));
            assertEquals(1, main.cli.getOptionValues("I").length);

            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            assertEquals("should be empty", 0, exmap.size());
            assertEquals("incorrect size", 1, incmap.size());
            assertTrue("doesn't contain correct entry", incmap.containsKey("tags"));
            assertEquals("doesn't contain correct entry", "priority1", incmap.get("tags"));
        }
        //use -I flag without key name, which should default to 'hostname' (first allowed key)
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-I", "tags=priority1", "-I", "floof"});
            assertNotNull(main.cli);
            assertTrue(main.cli.hasOption("I"));
            assertNotNull(main.cli.getOptionValue("I"));
            assertNotNull(main.cli.getOptionValues("I"));
            assertEquals("tags=priority1", main.cli.getOptionValue("I"));
            assertEquals(2, main.cli.getOptionValues("I").length);

            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            assertEquals("should be empty", 0, exmap.size());
            assertEquals("incorrect size", 2, incmap.size());
            assertTrue("doesn't contain correct entry", incmap.containsKey("tags"));
            assertEquals("doesn't contain correct entry", "priority1", incmap.get("tags"));
            assertTrue("doesn't contain correct entry", incmap.containsKey("hostname"));
            assertEquals("doesn't contain correct entry", "floof", incmap.get("hostname"));
        }
        //use multiple allowed keys
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-I", "tags=priority1", "-I", "os-name=floof"});

            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            assertEquals("should be empty", 0, exmap.size());
            assertEquals("incorrect size", 2, incmap.size());
            assertTrue("doesn't contain correct entry", incmap.containsKey("tags"));
            assertEquals("doesn't contain correct entry", "priority1", incmap.get("tags"));
            assertTrue("doesn't contain correct entry", incmap.containsKey("os-name"));
            assertEquals("doesn't contain correct entry", "floof", incmap.get("os-name"));
        }
        /**
         *use an invalid key


         {
         ExecTool main = newExecTool();
         main.parseArgs(new String[]{"-I", "tags=priority1", "-I", "potato=floof"});

         Map exmap = main.parseExcludeArgs(nodeKeys);
         assertEquals("should be empty", 0, exmap.size());

         try {
         Map incmap = main.parseIncludeArgs(nodeKeys);
         fail("parseIncludeArgs should throw exception");
         } catch (IllegalArgumentException e) {
         assertNotNull(e);
         }
         }
         */

        //use mix of include and exclude
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-X", "strongbad", "-I", "os-name=Testux"});

            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            assertEquals("incorrect size", 1, exmap.size());
            assertEquals("incorrect size", 1, incmap.size());
            assertTrue("doesn't contain hostname entry", exmap.containsKey("hostname"));
            assertEquals("doesn't contain correct value", "strongbad", exmap.get("hostname"));
            assertTrue("doesn't contain os-name entry", incmap.containsKey("os-name"));
            assertEquals("doesn't contain correct value", "Testux", incmap.get("os-name"));
        }
    }

    public void testFilterNodes() {
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            final Collection c = main.filterNodes().getNodes();
            assertEquals("wrong size", 4, c.size());
        }
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-X", "homestar", "-I", "os-name=Testux"});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            NodeSet nodeset = main.createNodeSet(incmap, exmap);
            assertTrue(nodeset.getExclude().isDominant());
            assertFalse(nodeset.getInclude().isDominant());
            final Collection c = main.filterNodes().getNodes();
            assertEquals("wrong size", 1, c.size());
        }

        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-X", "strongbad,homestar",
                "-I", "os-family=fakeos"});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            final Collection c = main.filterNodes().getNodes();
            assertEquals("wrong size", 1, c.size());
        }
    }



    public void testDefaultNodeFormatter() {
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            final Collection c = main.filterNodes().getNodes();
            final String result = new ExecTool.DefaultNodeFormatter().formatResults(c).toString();
            System.out.println("TEST-DEBUG: result='" + result + "'");
            assertNotNull(result);
            assertEquals("doesn't contain correct result", "cheat homestar strongbad test1", result);
        }
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-X", "homestar", "-I", "os-name=Testux"});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            NodeSet nodeset = main.createNodeSet(incmap, exmap);
            assertTrue(nodeset.getExclude().isDominant());
            assertFalse(nodeset.getInclude().isDominant());
            final Collection c = main.filterNodes().getNodes();
            assertEquals("wrong size", 1, c.size());
            final String result = new ExecTool.DefaultNodeFormatter().formatResults(c).toString();
            assertNotNull(result);
            assertEquals("doesn't contain correct result", "cheat", result);
        }

        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-X", "strongbad,homestar",
                "-I", "os-family=fakeos"});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            final Collection c = main.filterNodes().getNodes();
            assertEquals("wrong size", 1, c.size());
            final String result = new ExecTool.DefaultNodeFormatter().formatResults(c).toString();
            assertNotNull(result);
            assertEquals("doesn't contain correct result", "cheat", result);
        }
    }

    static class TestFormatter implements ExecTool.NodeFormatter{
        Collection nodes;
        public StringBuffer formatNodes(Collection nodes) throws Exception {
            this.nodes=nodes;
            return new StringBuffer();
        }
    }
    public void testListAction() {
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-v"});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            final Collection c = main.filterNodes().getNodes();
            final TestFormatter formatter = new TestFormatter();
            main.setNodeFormatter(formatter);
            main.listAction();
            assertNotNull(formatter.nodes);
            assertEquals(4, formatter.nodes.size());
        }
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-v", "-X", "homestar", "-I", "os-name=Testux"});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            NodeSet nodeset = main.createNodeSet(incmap, exmap);
            assertTrue(nodeset.getExclude().isDominant());
            assertFalse(nodeset.getInclude().isDominant());
            final Collection c = main.filterNodes().getNodes();
            final TestFormatter formatter = new TestFormatter();
            main.setNodeFormatter(formatter);
            main.listAction();
            assertNotNull(formatter.nodes);
            assertEquals(1, formatter.nodes.size());
        }

        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-v", "-X", "strongbad,homestar",
                "-I", "os-family=fakeos"});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            final Collection c = main.filterNodes().getNodes();
            final TestFormatter formatter = new TestFormatter();
            main.setNodeFormatter(formatter);
            main.listAction();
            assertNotNull(formatter.nodes);
            assertEquals(1, formatter.nodes.size());
        }
    }

    private ExecTool newExecTool() {
        return new ExecTool(getFrameworkInstance());
    }


    /*public void testScriptFileActionArgs() throws Exception {

        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", "TestExecTool", "-s", testScriptFile.getAbsolutePath()});
            assertNull("incorrect args", main.getArgsDeferred());
        }
        {
            ExecTool main = newExecTool();
            main.parseArgs(
                new String[]{"-p", "TestExecTool", "-s", testScriptFile.getAbsolutePath(), "--", "test", "args"});
            assertNotNull("incorrect args", main.getArgsDeferred());
            String[] args = main.getArgsDeferred();
            assertEquals("incorrect args count", 2, args.length);
            assertEquals("incorrect args string", testScriptFile.getAbsolutePath() + " test args",
                main.argsDeferredString);
        }
        {
            ExecTool main = newExecTool();
            main.parseArgs(
                new String[]{"-p", "TestExecTool", "-s", testScriptFile.getAbsolutePath(), "--", "test", "args",
                    "with a space"});
            assertNotNull("incorrect args", main.getArgsDeferred());
            String[] args = main.getArgsDeferred();
            assertEquals("incorrect args count", 3, args.length);
            assertEquals("incorrect args string", testScriptFile.getAbsolutePath() + " test args 'with a space'",
                main.argsDeferredString);
        }
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", "TestExecTool", "-S", "--", "test", "args"});
            main.setInlineScriptContent("test content");
            assertNotNull("incorrect args", main.getArgsDeferred());
            String[] args = main.getArgsDeferred();
            assertEquals("incorrect args count", 2, args.length);
            assertEquals("incorrect args string", main.getScriptpath() + " test args",
                main.argsDeferredString);
        }
    }*/

    /**
     * Stub to set as Executor class for StepExecutionItem types, used for testing.
     */
    /*public static class testExecutor1 implements Executor {
        static StepExecutionItem testItem;
        static ExecutionListener testListener;
        static boolean executeItemCalled=false;
        static Framework testFramework;
        static ExecutionResult returnResult=null;
        static ExecutionService testExecutionService;
        public testExecutor1() {
        }
        public ExecutionResult executeItem(StepExecutionItem item, ExecutionListener listener,
                                           final ExecutionService executionService,
                                           final Framework framework) throws ExecutionException {
            testItem=item;
            testListener=listener;
            executeItemCalled=true;
            testFramework = framework;
            testExecutionService=executionService;
            return returnResult;
        }
        static void reset(){
            testItem=null;
            testListener=null;
            executeItemCalled=false;
            testFramework=null;
            testExecutionService=null;
        }

    }*/
    public static class testExecutor1 implements NodeStepExecutor {
        static StepExecutionItem testItem;
        static ExecutionContext testContext;
        static ExecutionListener testListener;
        static boolean executeItemCalled = false;
        static Framework testFramework;
        static NodeStepResult returnResult = null;
        Framework framework;

        public testExecutor1(Framework framework) {
            this.framework = framework;
        }

        public NodeStepResult executeNodeStep(StepExecutionContext context, NodeStepExecutionItem item, INodeEntry node) throws
                                                                                                             NodeStepException {
            testContext=context;
            testItem = item;
            testListener = context.getExecutionListener();
            executeItemCalled = true;
            testFramework = framework;
            return returnResult;
        }

        static void reset() {
            testContext=null;
            testItem = null;
            testListener = null;
            executeItemCalled = false;
            testFramework = null;
        }
    }

    static class noopDispatcher implements CentralDispatcher{
        public QueuedItemResult queueDispatcherScript(IDispatchedScript dispatch) throws CentralDispatcherException {
            return null;
        }

        public QueuedItemResult queueDispatcherJob(IDispatchedJob job) throws CentralDispatcherException {
            return null;
        }

        public Collection<QueuedItem> listDispatcherQueue(final String project) throws CentralDispatcherException {
            return null;
        }
        public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
            return null;
        }

        public DispatcherResult killDispatcherExecution(String id) throws CentralDispatcherException {
            return null;
        }

        public ExecutionFollowResult followDispatcherExecution(String id, ExecutionFollowRequest request,
                                                               ExecutionFollowReceiver receiver) throws
            CentralDispatcherException {
            return null;
        }

        public Collection<IStoredJob> listStoredJobs(IStoredJobsQuery query, OutputStream output,
                                                     JobDefinitionFileFormat format) throws CentralDispatcherException {
            return null;
        }

        public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest request, File input,
                                                         JobDefinitionFileFormat format) throws
            CentralDispatcherException {
            return null;
        }

        public void reportExecutionStatus(String project, String title, String status, int failedNodeCount,
                                          int successNodeCount, String tags, String script, String summary, Date start,
                                          Date end) throws CentralDispatcherException {
        }

        public Collection<DeleteJobResult> deleteStoredJobs(Collection<String> jobIds) throws CentralDispatcherException {
            return null;
        }

        public ExecutionDetail getExecution(String execId) throws CentralDispatcherException {
            return null;
        }
    }
    static class testDispatcher extends noopDispatcher{
        boolean wascalled;
        String project;
        String name;
        String status;
        int failedNodeCount;
        int successNodeCount;
        String tags;
        String script;
        String summary;
        Date start;
        Date end;

        @Override
        public void reportExecutionStatus(String project, String title, String status, int failedNodeCount,
                                          int successNodeCount, String tags, String script, String summary, Date start,
                                          Date end) throws CentralDispatcherException {
            wascalled=true;
            this.project=project;
            this.name= title;
            this.status = status;
            this.failedNodeCount = failedNodeCount;
            this.successNodeCount=successNodeCount;
            this.tags=tags;
            this.script = script;
            this.summary=summary;
            this.start=start;
            this.end=end;
        }
    }

    public void testRunActionShouldLogResult() throws Exception {
        //set up test Executors
//        ExecutionServiceFactory.setDefaultExecutorClass(DispatchedScriptExecutionItem.class, testExecutor1.class);
        final NodeStepExecutionService cis = NodeStepExecutionService.getInstanceForFramework(
            getFrameworkInstance());
        cis.registerClass("exec", testExecutor1.class);


        //set return result
        testExecutor1.returnResult = new NodeStepResultImpl(null) {
            @Override
            public String toString() {
                return "test1ResultString";
            }
        };

        final Framework framework = getFrameworkInstance();

        final testDispatcher test1 = new testDispatcher();
        framework.setCentralDispatcherMgr(test1);

        { //test dispatch shell script
            System.err.println("testRunActionShouldLogResult start");
            ExecTool main = new ExecTool(framework);
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "--", "uptime", "for", "ever"});

            main.runAction();
            System.err.println("testRunActionShouldLogResult selector: " + main.getNodeSelector());
            assertTrue(test1.wascalled);
            assertEquals(TEST_EXEC_TOOL_PROJECT, test1.project);
            assertEquals("dispatch", test1.name);
            assertEquals("succeeded", test1.status);
            assertEquals(0, test1.failedNodeCount);

            assertEquals(1, test1.successNodeCount);
            assertEquals("", test1.tags);
            assertEquals("dispatch -p " + TEST_EXEC_TOOL_PROJECT + " -- uptime for ever", test1.script);
            assertNotNull(test1.start);
            assertNotNull(test1.end);


            testExecutor1.reset();
        }
    }

    public void testRunActionShouldLogResultFailure() throws Exception {
        //set up test Executors
//        ExecutionServiceFactory.setDefaultExecutorClass(DispatchedScriptExecutionItem.class, testExecutor1.class);
        final NodeStepExecutionService cis = NodeStepExecutionService.getInstanceForFramework(
            getFrameworkInstance());
        cis.registerClass("exec", testExecutor1.class);
        //set return result
        testExecutor1.returnResult = new NodeStepResultImpl(null,null,null,null) {
            public String toString() {
                return "test failure result";
            }
        };

        final Framework framework = getFrameworkInstance();

        final testDispatcher test1 = new testDispatcher();
        framework.setCentralDispatcherMgr(test1);

        { //test dispatch shell script
            ExecTool main = new ExecTool(framework);
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "--", "uptime", "for", "ever"});

            try {
                main.runAction();
                fail("should have thrown exception");
            } catch (Exception e) {
            }
            assertTrue(test1.wascalled);
            assertEquals(TEST_EXEC_TOOL_PROJECT, test1.project);
            assertEquals("dispatch", test1.name);
            assertEquals("failed", test1.status);
            assertEquals(1, test1.failedNodeCount);
            assertEquals(0, test1.successNodeCount);
            assertEquals("", test1.tags);
            assertEquals("dispatch -p " + TEST_EXEC_TOOL_PROJECT + " -- uptime for ever", test1.script);
            assertNotNull(test1.start);
            assertNotNull(test1.end);


            testExecutor1.reset();
        }
    }
    public void testRunAction() throws Exception{
        //set up test Executors
//        ExecutionServiceFactory.setDefaultExecutorClass(DispatchedScriptExecutionItem.class, testExecutor1.class);
        final NodeStepExecutionService cis = NodeStepExecutionService.getInstanceForFramework(
            getFrameworkInstance());
        cis.registerClass("exec", testExecutor1.class);
        cis.registerClass("script", testExecutor1.class);
        testExecutor1.returnResult=null;

        final Framework framework = getFrameworkInstance();

        framework.setCentralDispatcherMgr(new noopDispatcher());

        {//test null result
            ExecTool main = new ExecTool(framework);
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-s", testScriptFile.getAbsolutePath()});

            try {
                main.runAction();
                fail("run shouldn't succeed");
            } catch (DispatcherException e) {
                assertNotNull(e);
                e.printStackTrace(System.err);
            }
            assertTrue("executeItem not called", testExecutor1.executeItemCalled);
            assertNotNull("missing execitem", testExecutor1.testItem);
            assertNotNull("missing execListener", testExecutor1.testListener);
            assertNotNull("missing testFramework", testExecutor1.testFramework);
            testExecutor1.reset();
        }

        //set return result
        testExecutor1.returnResult=new NodeStepResultImpl(null){
            public String toString() {
                return "testResult1";
            }
        };

        { //test dispatch shell script
            ExecTool main = new ExecTool(framework);
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "--", "uptime","for","ever"});

            main.runAction();
            assertNotNull("missing execitem", testExecutor1.testItem);
            assertNotNull("missing execListener", testExecutor1.testListener);
            assertTrue("executeItem not called", testExecutor1.executeItemCalled);
            assertNotNull("missing testFramework", testExecutor1.testFramework);
            assertNotNull("missing testFramework", testExecutor1.testContext);

            assertTrue(testExecutor1.testItem instanceof ExecCommandExecutionItem);
            ExecCommandExecutionItem item1 = (ExecCommandExecutionItem) testExecutor1.testItem;
            assertEquals(TEST_EXEC_TOOL_PROJECT, testExecutor1.testContext.getFrameworkProject());
            assertNotNull("should not be null", item1.getCommand());
            assertEquals("should not be null",3, item1.getCommand().length);
            assertEquals("should not be null","uptime", item1.getCommand()[0]);
            assertEquals("should not be null","for", item1.getCommand()[1]);
            assertEquals("should not be null","ever", item1.getCommand()[2]);

            testExecutor1.reset();
        }
        { //now test the script detail: script file path
            ExecTool main = new ExecTool(framework);
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-s", testScriptFile.getAbsolutePath()});

            main.runAction();
            assertNotNull("missing execitem", testExecutor1.testItem);
            assertNotNull("missing execListener", testExecutor1.testListener);
            assertTrue("executeItem not called", testExecutor1.executeItemCalled);
            assertNotNull("missing testFramework", testExecutor1.testFramework);
            assertNotNull("missing testFramework", testExecutor1.testContext);

            assertTrue(testExecutor1.testItem instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem item1 = (ScriptFileCommandExecutionItem) testExecutor1.testItem;
            assertEquals(TEST_EXEC_TOOL_PROJECT, testExecutor1.testContext.getFrameworkProject());
            assertEquals(testScriptFile.getAbsolutePath(), item1.getServerScriptFilePath());
            assertNull(item1.getScript());
            assertNotNull(item1.getScriptAsStream());

            testExecutor1.reset();
        }
        {//: script file path with args
            ExecTool main = new ExecTool(framework);
            main.parseArgs(
                new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-s", testScriptFile.getAbsolutePath(), "--", "test", "args"});


            main.runAction();
            assertNotNull("missing execitem", testExecutor1.testItem);
            assertNotNull("missing execListener", testExecutor1.testListener);
            assertTrue("executeItem not called", testExecutor1.executeItemCalled);
            assertNotNull("missing testFramework", testExecutor1.testFramework);
            assertNotNull("missing testFramework", testExecutor1.testContext);

            assertTrue(testExecutor1.testItem instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem item1 = (ScriptFileCommandExecutionItem) testExecutor1.testItem;
            assertEquals(TEST_EXEC_TOOL_PROJECT, testExecutor1.testContext.getFrameworkProject());
            assertEquals(testScriptFile.getAbsolutePath(), item1.getServerScriptFilePath());
            assertNotNull(item1.getArgs());
            String[] args = item1.getArgs();
            assertEquals("incorrect args count", 2, args.length);
            assertEquals("incorrect args count", "test", args[0]);
            assertEquals("incorrect args count", "args", args[1]);
            assertNull(item1.getScript());
            assertNotNull(item1.getScriptAsStream());

            testExecutor1.reset();

        }
        { //: script file path: with args with a space
            ExecTool main = new ExecTool(framework);
            main.parseArgs(
                new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-s", testScriptFile.getAbsolutePath(), "--", "test", "args",
                    "with a space"});

            main.runAction();
            assertNotNull("missing execitem", testExecutor1.testItem);
            assertNotNull("missing execListener", testExecutor1.testListener);
            assertTrue("executeItem not called", testExecutor1.executeItemCalled);
            assertNotNull("missing testFramework", testExecutor1.testFramework);
            assertNotNull("missing testFramework", testExecutor1.testContext);

            assertTrue(testExecutor1.testItem instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem item1 = (ScriptFileCommandExecutionItem) testExecutor1.testItem;
            assertEquals(TEST_EXEC_TOOL_PROJECT, testExecutor1.testContext.getFrameworkProject());
            assertEquals(testScriptFile.getAbsolutePath(), item1.getServerScriptFilePath());
            assertNotNull(item1.getArgs());
            String[] args = item1.getArgs();
            assertEquals("incorrect args count", 3, args.length);
            assertEquals("test", args[0]);
            assertEquals("args", args[1]);
            assertEquals("with a space", args[2]);
            assertNull(item1.getScript());
            assertNotNull(item1.getScriptAsStream());

            testExecutor1.reset();
        }
        { //inline script content
            ExecTool main = new ExecTool(framework);
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-S", "--", "test", "args"});
            main.setInlineScriptContent("test content");


            main.runAction();
            assertNotNull("missing execitem", testExecutor1.testItem);
            assertNotNull("missing execListener", testExecutor1.testListener);
            assertTrue("executeItem not called", testExecutor1.executeItemCalled);
            assertNotNull("missing testFramework", testExecutor1.testFramework);
            assertNotNull("missing testFramework", testExecutor1.testContext);

            assertTrue(testExecutor1.testItem instanceof ScriptFileCommandExecutionItem);
            ScriptFileCommandExecutionItem item1 = (ScriptFileCommandExecutionItem) testExecutor1.testItem;
            assertEquals(TEST_EXEC_TOOL_PROJECT, testExecutor1.testContext.getFrameworkProject());
            assertNotNull(item1.getServerScriptFilePath());
            assertNotNull(item1.getArgs());
            String[] args = item1.getArgs();
            assertEquals("incorrect args count", 2, args.length);
            assertEquals("test", args[0]);
            assertEquals("args", args[1]);
            assertNotNull(item1.getScript());
            assertEquals("test content", item1.getScript());
            assertNotNull(item1.getScriptAsStream());

            testExecutor1.reset();
        }
    }

    public void testGenerateArgline() throws Exception {
        assertEquals("invalid", "test 1 2", CLIUtils.generateArgline("test", new String[]{"1", "2"}));
        assertEquals("invalid", "test 1 2 '3 4'", CLIUtils.generateArgline("test", new String[]{"1", "2", "3 4"}));
        assertEquals("invalid", "test 1 2 '\"3 4\"'", CLIUtils.generateArgline("test",
            new String[]{"1", "2", "\"3 4\""}));
        assertEquals("invalid", "test 1 2 \"34\"", CLIUtils.generateArgline("test", new String[]{"1", "2", "\"34\""}));
        assertEquals("invalid", "test 1 2 '3 4'", CLIUtils.generateArgline("test", new String[]{"1", "2", "'3 4'"}));
        //test empty and null values
        assertEquals("invalid", "test", CLIUtils.generateArgline("test", null));
        assertEquals("invalid", "test", CLIUtils.generateArgline("test", new String[0]));
    }

    /**
     * test data using unix line endings
     */
    private final String testData1 = "A Test input file\n"
                                     + "\n"
                                     + "Extra lines\n"
                                     + "Extra lines\n"
                                     + "Extra lines";
    /**
     * Test data using windows line endings
     */
    private final String testData2 = "A Test input file\r\n"
                                     + "\r\n"
                                     + "Extra lines\r\n"
                                     + "Extra lines\r\n"
                                     + "Extra lines";
    /**
     * Test data using other line endings
     */
    private final String testData3 = "A Test input file\r"
                                     + "\r"
                                     + "Extra lines\r"
                                     + "Extra lines\r"
                                     + "Extra lines";
    private final List<String> testStrings = Arrays.asList(testData1, testData2, testData3);


    /**
     * Test the writeInputToFile method.  InputStream should be written to match the input
     *
     * @throws Exception
     */
    public void testWriteInputToFile() throws Exception {
        final org.apache.tools.ant.util.FileUtils utils = org.apache.tools.ant.util.FileUtils.getFileUtils();
        int index = 0;
        for (String testData : testStrings) {
            index++;
            ExecTool main = newExecTool();
            File t = File.createTempFile(TEST_EXEC_TOOL_PROJECT, ".txt");
            t.deleteOnExit();
            FileOutputStream fos=new FileOutputStream(t);
            try{
	            OutputStreamWriter osw = new OutputStreamWriter(fos);
	            osw.write(testData);
	            osw.flush();
	            osw.close();
            }finally{
            	fos.close();
            }

            InputStream ins = new FileInputStream(t);
            try{
	            File temp = main.writeInputToFile(ins);
	            //compare file contents
	            assertTrue("File contents were not the same (data " + index + ")", utils.contentEquals(t, temp, true));
            }finally{
            	ins.close();
            }
        }
    }

    public void testCreateNodeSet() throws Exception {
        {
            final ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT});
            final NodeSet set = main.createFilterNodeSelector();
            assertNotNull(set);
            assertNotNull(set.getInclude());
            assertTrue(set.getInclude().isBlank() );
            assertNotNull(set.getExclude());
            assertTrue(set.getExclude().isBlank());
            assertEquals(1,set.getThreadCount());
            assertEquals(false,set.isKeepgoing());
        }
        {
            final ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT,"-K"});
            final NodeSet set = main.createFilterNodeSelector();
            assertNotNull(set);
            assertNotNull(set.getInclude());
            assertTrue(set.getInclude().isBlank() );
            assertNotNull(set.getExclude());
            assertTrue(set.getExclude().isBlank());
            assertEquals(1,set.getThreadCount());
            assertEquals(true,set.isKeepgoing());
        }
        {
            final ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT,"-C","2"});
            final NodeSet set = main.createFilterNodeSelector();
            assertNotNull(set);
            assertNotNull(set.getInclude());
            assertTrue(set.getInclude().isBlank() );
            assertNotNull(set.getExclude());
            assertTrue(set.getExclude().isBlank());
            assertEquals(2,set.getThreadCount());
            assertEquals(false,set.isKeepgoing());
        }
        {
            final ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT,"-C","2","-I","ahostname","-X","tags=blaoen","-I","os-family=test","-X","os-version=3"});
            final NodeSet set = main.createFilterNodeSelector();
            assertNotNull(set);
            assertNotNull(set.getInclude());
            assertFalse(set.getInclude().isBlank() );
            assertEquals("ahostname",set.getInclude().getHostname() );
            assertEquals("test",set.getInclude().getOsfamily() );
            assertNotNull(set.getExclude());
            assertFalse(set.getExclude().isBlank());
            assertEquals("blaoen",set.getExclude().getTags());
            assertEquals("3",set.getExclude().getOsversion());
            assertEquals(2,set.getThreadCount());
            assertEquals(false,set.isKeepgoing());
        }
        {
            //test precedence setting, Include dominant in first position
            final ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT,"-C","2","-I","ahostname","-X","tags=blaoen"});
            final NodeSet set = main.createFilterNodeSelector();
            assertNotNull(set);
            assertNotNull(set.getInclude());
            assertFalse(set.getInclude().isBlank() );
            assertTrue(set.getInclude().isDominant() );
            assertNotNull(set.getExclude());
            assertFalse(set.getExclude().isBlank());
            assertFalse(set.getExclude().isDominant());
        }
        {
            //test precedence setting, Exclude dominant in first position
            final ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT,"-C","2","-X","ahostname","-I","tags=blaoen"});
            final NodeSet set = main.createFilterNodeSelector();
            assertNotNull(set);
            assertNotNull(set.getInclude());
            assertFalse(set.getInclude().isBlank() );
            assertFalse(set.getInclude().isDominant() );
            assertNotNull(set.getExclude());
            assertFalse(set.getExclude().isBlank());
            assertTrue(set.getExclude().isDominant());
        }
        {
            //test precedence setting, Include dominant explicitly
            final ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT,"--filter-exclude-precedence","false","-C","2","-X","ahostname","-I","tags=blaoen"});
            final NodeSet set = main.createFilterNodeSelector();
            assertNotNull(set);
            assertNotNull(set.getInclude());
            assertFalse(set.getInclude().isBlank() );
            assertTrue(set.getInclude().isDominant() );
            assertNotNull(set.getExclude());
            assertFalse(set.getExclude().isBlank());
            assertFalse(set.getExclude().isDominant());
        }
        {
            //test precedence setting, Exclude dominant explicitly
            final ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT,"--filter-exclude-precedence","true","-C","2","-I","ahostname","-X","tags=blaoen"});
            final NodeSet set = main.createFilterNodeSelector();
            assertNotNull(set);
            assertNotNull(set.getInclude());
            assertFalse(set.getInclude().isBlank() );
            assertFalse(set.getInclude().isDominant() );
            assertNotNull(set.getExclude());
            assertFalse(set.getExclude().isBlank());
            assertTrue(set.getExclude().isDominant());
        }
    }

    final QueuedItemResult queuedItemSuccess = new QueuedItemResult() {
        public boolean isSuccessful() {
            return true;
        }

        public String getMessage() {
            return null;
        }

        public QueuedItem getItem() {
            return testitem1;
        }

    };
    final QueuedItem testitem1 = new QueuedItem() {

        public String getId() {
            return "testid1";
        }

        public String getUrl() {
            return "testurl1";
        }

        public String getName() {
            return "testname1";
        }
    };
    final QueuedItemResult queuedItemFailed = new QueuedItemResult() {
        public boolean isSuccessful() {
            return false;
        }

        public String getMessage() {
            return null;
        }

        public QueuedItem getItem() {
            return testitem1;
        }

    };

    private class testCentralDispatcher implements CentralDispatcher {
        boolean queueCommandCalled = false;
        boolean queueScriptCalled = false;
        boolean listDispatcherCalled = false;
        boolean killCalled = false;
        boolean listStoredJobsCalled = false;
        boolean loadJobsCalled = false;
        boolean queueDispatcherJobCalled = false;
        IDispatchedScript passedinScript;
        String passedinId;

        public QueuedItemResult queueDispatcherJob(IDispatchedJob job) throws CentralDispatcherException {
            queueDispatcherJobCalled=true;
            return null;
        }

        public QueuedItemResult queueDispatcherScript(IDispatchedScript dispatch) throws
            CentralDispatcherException {
            queueScriptCalled = true;
            passedinScript = dispatch;
            return queuedItemSuccess;
        }

        public Collection<QueuedItem> listDispatcherQueue(final String project) throws CentralDispatcherException {
            listDispatcherCalled = true;
            return null;
        }
        public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
            listDispatcherCalled = true;
            return null;
        }

        public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest request, File input,
                                                         JobDefinitionFileFormat format) throws
            CentralDispatcherException {
            loadJobsCalled=true;
            return null;
        }

        public DispatcherResult killDispatcherExecution(String id) throws CentralDispatcherException {
            killCalled = true;
            passedinId = id;
            return null;
        }

        public ExecutionFollowResult followDispatcherExecution(String id, ExecutionFollowRequest request,
                                                               ExecutionFollowReceiver receiver) throws
            CentralDispatcherException {
            return null;
        }

        public void assertQueueScriptOnlyCalled() {
            assertFalse("queueDispatcherExecution should not have been called: "+this, queueCommandCalled);
            assertTrue("queueDispatcherScript should have been called: " + this, queueScriptCalled);
            assertNotNull("Passed in IDispatchedScript was null: " + this, passedinScript);
            assertFalse("listDispatcherQueue should not have been called: " + this, listDispatcherCalled);
            assertFalse("killDispatcherExecution should not have been called: " + this, killCalled);
            assertFalse("listStoredJobs should not have been called: " + this, listStoredJobsCalled);
            assertFalse("loadJobs should not have been called: " + this, loadJobsCalled);
        }
        public Collection<IStoredJob> listStoredJobs(IStoredJobsQuery query, OutputStream output,
                                                     JobDefinitionFileFormat format) throws
            CentralDispatcherException {
            listStoredJobsCalled=true;
            return null;
        }

        public void reportExecutionStatus(String project, String title, String status, int totalNodeCount,
                                          int successNodeCount, String tags, String script, String summary, Date start,
                                          Date end) throws CentralDispatcherException {
        }

        public Collection<DeleteJobResult> deleteStoredJobs(Collection<String> jobIds) throws CentralDispatcherException {
            return null;
        }

        public ExecutionDetail getExecution(String execId) throws CentralDispatcherException {
            return null;
        }

        @Override
        public String toString() {
            return "testCentralDispatcher{" +
                   "killCalled=" + killCalled +
                   ", queueCommandCalled=" + queueCommandCalled +
                   ", queueScriptCalled=" + queueScriptCalled +
                   ", listDispatcherCalled=" + listDispatcherCalled +
                   ", passedinId='" + passedinId + '\'' +
                   ", passedinScript=" + passedinScript +
                   '}';
        }
    }

    public void testQueueOption() throws Exception {
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "--noqueue"});
            assertTrue(main.isArgNoQueue());
        }
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-" + ExecTool.NO_QUEUE_FLAG});
            assertTrue(main.isArgNoQueue());
        }

        //test old -Q/--queue have no effect
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-Q" });
            assertFalse(main.isArgNoQueue());
        }
        {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "--queue" });
            assertFalse(main.isArgNoQueue());
        }
        //test action calls
        final Framework framework = getFrameworkInstance();

        {
            ExecTool main = newExecTool();
            main.setFramework(framework);
            final testCentralDispatcher test = new testCentralDispatcher();
            framework.setCentralDispatcherMgr(test);

            //exec the dispatch

            main.run(new String[]{"-p", "testProject", "-Q", "--", "shell", "command", "string"});
            test.assertQueueScriptOnlyCalled();
            assertNotNull("args should not be null", test.passedinScript.getArgs());
            assertEquals("wrong args size",3, test.passedinScript.getArgs().length);
            assertEquals("wrong args entry","shell", test.passedinScript.getArgs()[0]);
            assertEquals("wrong args entry","command", test.passedinScript.getArgs()[1]);
            assertEquals("wrong args entry","string", test.passedinScript.getArgs()[2]);
            assertNull(test.passedinScript.getScript());
            assertNull(test.passedinScript.getServerScriptFilePath());
            assertNull(test.passedinScript.getScriptAsStream());
            assertEquals("testProject", test.passedinScript.getFrameworkProject());
        }
        {
            ExecTool main = newExecTool();
            main.setFramework(framework);
            final testCentralDispatcher test = new testCentralDispatcher();
            framework.setCentralDispatcherMgr(test);

            //exec the dispatch

            main.run(new String[]{"-p", "testProject", "-Q", "--", "shell", "space string"});
            test.assertQueueScriptOnlyCalled();
            assertNotNull("args should not be null", test.passedinScript.getArgs());
            assertEquals("wrong args size", 2, test.passedinScript.getArgs().length);
            assertEquals("wrong args entry", "shell", test.passedinScript.getArgs()[0]);
            assertEquals("wrong args entry", "space string", test.passedinScript.getArgs()[1]);
            assertNull(test.passedinScript.getScript());
            assertNull(test.passedinScript.getServerScriptFilePath());
            assertNull(test.passedinScript.getScriptAsStream());
            assertEquals("testProject", test.passedinScript.getFrameworkProject());
        }
        {
            //test script path input available as InputStream when queueing dispatch

            ExecTool main = newExecTool();
            main.setFramework(framework);
            final testCentralDispatcher test = new testCentralDispatcher();
            framework.setCentralDispatcherMgr(test);

            //exec the dispatch

            main.run(new String[]{"-p", "testProject", "-Q", "-s",
                "src/test/resources/com/dtolabs/rundeck/core/cli/test-dispatch-script.txt"});
            test.assertQueueScriptOnlyCalled();
            assertNull("unexpected value: ", test.passedinScript.getArgs());
            assertNull("unexpected value: "+ test.passedinScript.getScript(), test.passedinScript.getScript());
            assertEquals("unexpected value: " + test.passedinScript.getServerScriptFilePath(),
                new File("src/test/resources/com/dtolabs/rundeck/core/cli/test-dispatch-script.txt").getAbsolutePath(), test.passedinScript.getServerScriptFilePath());
            assertNull(test.passedinScript.getScriptAsStream());
            assertEquals("testProject", test.passedinScript.getFrameworkProject());
        }

        {
            //test the node filter arguments

            ExecTool main = newExecTool();
            main.setFramework(framework);
            final testCentralDispatcher test = new testCentralDispatcher();
            framework.setCentralDispatcherMgr(test);

            //exec the dispatch

            main.run(
                new String[]{"-p", "testProject", "-K", "-C", "2", "-I", "hostname1", "-X", "tags=baloney", "-Q", "--",
                    "shell", "command", "string"});
            test.assertQueueScriptOnlyCalled();
            assertNotNull(test.passedinScript.getNodeSet());
            assertNotNull(test.passedinScript.getNodeSet().getInclude());
            assertNotNull(test.passedinScript.getNodeSet().getExclude());
            assertFalse(test.passedinScript.getNodeSet().getInclude().isBlank());
            assertFalse(test.passedinScript.getNodeSet().getExclude().isBlank());
            assertEquals("hostname1", test.passedinScript.getNodeSet().getInclude().getHostname());
            assertEquals("baloney", test.passedinScript.getNodeSet().getExclude().getTags());
            assertEquals(2, test.passedinScript.getNodeSet().getThreadCount());
            assertEquals(true, test.passedinScript.getNodeSet().isKeepgoing());

        }

    }
}
