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


import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
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
import java.util.*;

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
        FrameworkProject d = getFrameworkInstance().getFilesystemFrameworkProjectManager().createFSFrameworkProject(
                TEST_EXEC_TOOL_PROJECT
        );
        assert d.getBaseDir().isDirectory();
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
        IRundeckProject d2 = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(
            TEST_EXEC_TOOL_PROJ2);

    }

    public void tearDown() throws Exception {
        super.tearDown();
        FrameworkProject d = getFrameworkInstance().getFilesystemFrameworkProjectManager().createFSFrameworkProject(
                TEST_EXEC_TOOL_PROJECT
        );
        FileUtils.deleteDir(d.getBaseDir());
        getFrameworkInstance().getFrameworkProjectMgr().removeFrameworkProject(TEST_EXEC_TOOL_PROJECT);
        FrameworkProject d2 = getFrameworkInstance().getFilesystemFrameworkProjectManager().createFSFrameworkProject(
                TEST_EXEC_TOOL_PROJ2
        );
        FileUtils.deleteDir(d2.getBaseDir());


        getFrameworkInstance().getFrameworkProjectMgr().removeFrameworkProject(TEST_EXEC_TOOL_PROJ2);
//        ExecutionServiceFactory.resetDefaultExecutorClasses();
        getFrameworkInstance().setService(NodeStepExecutionService.SERVICE_NAME, null);
    }

    public static Test suite() {
        return new TestSuite(TestExecTool.class);
    }


    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    public void testParseArgsNoProject_multipleExist() throws Exception {

            //test missing -p option when multiple projects exist
            ExecTool main = newExecTool();
        testCentralDispatcher centralDispatcher = new testCentralDispatcher();
        centralDispatcher.projectNames = Arrays.asList("project1", "project2");
        main.setCentralDispatcher(centralDispatcher);
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
    public void testParseArgsNoProject_singleExists() throws Exception {

            //test missing -p option when multiple projects exist
            ExecTool main = newExecTool();
        testCentralDispatcher centralDispatcher = new testCentralDispatcher();
        centralDispatcher.projectNames = Arrays.asList("project1");
        main.setCentralDispatcher(centralDispatcher);

        main.parseArgs(
            new String[]{"-K", "-C", "2", "-I", "hostname1", "-X", "tags=baloney", "-Q", "--", "shell",
                "command",
                "string"});
    }
    public void testParseArgsNoProject_noneExists() throws Exception {

            //test missing -p option when multiple projects exist
            ExecTool main = newExecTool();
        testCentralDispatcher centralDispatcher = new testCentralDispatcher();
        centralDispatcher.projectNames = Arrays.asList();
        main.setCentralDispatcher(centralDispatcher);

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

    public void testFilterNodesDefaultAll() throws CentralDispatcherException {
        ExecTool main = newExecTool();
        NodeSetImpl nodeSet = new NodeSetImpl();
        nodeSet.putNode(new NodeEntryImpl("node1"));
        nodeSet.putNode(new NodeEntryImpl("node2"));
        nodeSet.putNode(new NodeEntryImpl("node3"));
        nodeSet.putNode(new NodeEntryImpl("node4"));

        testCentralDispatcher centralDispatcher = getFilterNodesDispatcher(main, nodeSet);

        main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT});
        Map exmap = main.parseExcludeArgs(nodeKeys);
        Map incmap = main.parseIncludeArgs(nodeKeys);
        final Collection c = main.filterNodes().getNodes();

        assertEquals("wrong size", 4, c.size());
        assertEquals(TEST_EXEC_TOOL_PROJECT, centralDispatcher.nodesProject);
        assertEquals(".*", centralDispatcher.nodesFilter);
    }

    private testCentralDispatcher getFilterNodesDispatcher(
            final ExecTool main,
            final NodeSetImpl nodeSet
    )
    {
        testCentralDispatcher centralDispatcher = new testCentralDispatcher();
        centralDispatcher.hasFilteredNodes = true;
        centralDispatcher.filteredNodes = nodeSet;
        main.setCentralDispatcher(centralDispatcher);
        return centralDispatcher;
    }

    public void testFilterNodesSimple1() throws CentralDispatcherException {
        ExecTool main = newExecTool();
        NodeSetImpl nodeSet = new NodeSetImpl();
        nodeSet.putNode(new NodeEntryImpl("node1"));
        testCentralDispatcher centralDispatcher = getFilterNodesDispatcher(main, nodeSet);
        main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-X", "homestar", "-I", "os-name=Testux"});
        Map exmap = main.parseExcludeArgs(nodeKeys);
        Map incmap = main.parseIncludeArgs(nodeKeys);
        NodeSet nodeset = main.createNodeSet(incmap, exmap);
        assertTrue(nodeset.getExclude().isDominant());
        assertFalse(nodeset.getInclude().isDominant());
        final Collection c = main.filterNodes().getNodes();
        assertEquals("wrong size", 1, c.size());
        assertEquals(TEST_EXEC_TOOL_PROJECT, centralDispatcher.nodesProject);
        assertEquals("osname: Testux !hostname: homestar", centralDispatcher.nodesFilter);
    }

    public void testFilterNodes3() throws CentralDispatcherException {
        ExecTool main = newExecTool();
        NodeSetImpl nodeSet = new NodeSetImpl();
        nodeSet.putNode(new NodeEntryImpl("node1"));
        testCentralDispatcher centralDispatcher = getFilterNodesDispatcher(main, nodeSet);
        main.parseArgs(
                new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-X", "strongbad,homestar",
                        "-I", "os-family=fakeos"}
        );
        Map exmap = main.parseExcludeArgs(nodeKeys);
        Map incmap = main.parseIncludeArgs(nodeKeys);
        final Collection c = main.filterNodes().getNodes();
        assertEquals("wrong size", 1, c.size());
        assertEquals(TEST_EXEC_TOOL_PROJECT, centralDispatcher.nodesProject);
        assertEquals("osfamily: fakeos !hostname: strongbad,homestar", centralDispatcher.nodesFilter);
    }



    public void testDefaultNodeFormatter() throws CentralDispatcherException {
            ExecTool main = newExecTool();
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
        NodeSetImpl nodeSet = new NodeSetImpl();
        nodeSet.putNode(new NodeEntryImpl("cheat"));
        nodeSet.putNode(new NodeEntryImpl("homestar"));
        nodeSet.putNode(new NodeEntryImpl("strongbad"));
        nodeSet.putNode(new NodeEntryImpl("test1"));
        testCentralDispatcher centralDispatcher = getFilterNodesDispatcher(main, nodeSet);
            final Collection c = main.filterNodes().getNodes();
            final String result = new ExecTool.DefaultNodeFormatter().formatResults(c).toString();
            assertNotNull(result);
            assertEquals("doesn't contain correct result", "cheat homestar strongbad test1", result);
        assertEquals(TEST_EXEC_TOOL_PROJECT,centralDispatcher.nodesProject);
        assertEquals(".*",centralDispatcher.nodesFilter);
        }
    public void testDefaultNodeFormatter2() throws CentralDispatcherException {
            ExecTool main = newExecTool();
        NodeSetImpl nodeSet = new NodeSetImpl();
        nodeSet.putNode(new NodeEntryImpl("cheat"));
        testCentralDispatcher centralDispatcher = getFilterNodesDispatcher(main, nodeSet);
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
        assertEquals(TEST_EXEC_TOOL_PROJECT,centralDispatcher.nodesProject);
        assertEquals("osname: Testux !hostname: homestar",centralDispatcher.nodesFilter);
        }

    public void testDefaultNodeFormatter3() throws CentralDispatcherException {
            ExecTool main = newExecTool();
        NodeSetImpl nodeSet = new NodeSetImpl();
        nodeSet.putNode(new NodeEntryImpl("cheat"));
        testCentralDispatcher centralDispatcher = getFilterNodesDispatcher(main, nodeSet);
            main.parseArgs(new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-X", "strongbad,homestar",
                "-I", "os-family=fakeos"});
            Map exmap = main.parseExcludeArgs(nodeKeys);
            Map incmap = main.parseIncludeArgs(nodeKeys);
            final Collection c = main.filterNodes().getNodes();
            assertEquals("wrong size", 1, c.size());
            final String result = new ExecTool.DefaultNodeFormatter().formatResults(c).toString();
            assertNotNull(result);
            assertEquals("doesn't contain correct result", "cheat", result);
        assertEquals(TEST_EXEC_TOOL_PROJECT,centralDispatcher.nodesProject);
        assertEquals("osfamily: fakeos !hostname: strongbad,homestar",centralDispatcher.nodesFilter);
    }

    static class TestFormatter implements ExecTool.NodeFormatter{
        Collection nodes;
        public StringBuffer formatNodes(Collection nodes) throws Exception {
            this.nodes=nodes;
            return new StringBuffer();
        }
    }

    public void testListActionAll() throws CentralDispatcherException {
        ExecTool main = newExecTool();
        NodeSetImpl nodeSet = new NodeSetImpl();
        nodeSet.putNode(new NodeEntryImpl("node1"));
        nodeSet.putNode(new NodeEntryImpl("node2"));
        nodeSet.putNode(new NodeEntryImpl("node3"));
        nodeSet.putNode(new NodeEntryImpl("node4"));
        testCentralDispatcher centralDispatcher = getFilterNodesDispatcher(main, nodeSet);
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

    public void testListActionFiltered1() throws CentralDispatcherException {
        ExecTool main = newExecTool();
        NodeSetImpl nodeSet = new NodeSetImpl();
        nodeSet.putNode(new NodeEntryImpl("node1"));
        testCentralDispatcher centralDispatcher = getFilterNodesDispatcher(main, nodeSet);
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
        assertEquals(TEST_EXEC_TOOL_PROJECT, centralDispatcher.nodesProject);
        assertEquals("osname: Testux !hostname: homestar", centralDispatcher.nodesFilter);
    }

    public void testListAction3() throws CentralDispatcherException {
        ExecTool main = newExecTool();
        NodeSetImpl nodeSet = new NodeSetImpl();
        nodeSet.putNode(new NodeEntryImpl("node1"));
        testCentralDispatcher centralDispatcher = getFilterNodesDispatcher(main, nodeSet);

        main.parseArgs(
                new String[]{"-p", TEST_EXEC_TOOL_PROJECT, "-v", "-X", "strongbad,homestar",
                        "-I", "os-family=fakeos"}
        );
        Map exmap = main.parseExcludeArgs(nodeKeys);
        Map incmap = main.parseIncludeArgs(nodeKeys);
        final Collection c = main.filterNodes().getNodes();
        final TestFormatter formatter = new TestFormatter();
        main.setNodeFormatter(formatter);
        main.listAction();
        assertNotNull(formatter.nodes);
        assertEquals(1, formatter.nodes.size());
        assertEquals(TEST_EXEC_TOOL_PROJECT, centralDispatcher.nodesProject);
        assertEquals("osfamily: fakeos !hostname: strongbad,homestar", centralDispatcher.nodesFilter);
    }

    private ExecTool newExecTool() {
        return new ExecTool(BaseTool.createDefaultDispatcherConfig());
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
        boolean listDispatcherPagedCalled = false;
        boolean killCalled = false;
        boolean listStoredJobsCalled = false;
        boolean loadJobsCalled = false;
        boolean queueDispatcherJobCalled = false;
        IDispatchedScript passedinScript;
        String passedinId;
        boolean hasFilteredNodes=false;
        INodeSet filteredNodes;
        String nodesFilter;
        String nodesProject;
        List<String> projectNames;

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

        @Override
        public PagedResult<QueuedItem> listDispatcherQueue(final String project, final Paging paging)
                throws CentralDispatcherException
        {
            listDispatcherPagedCalled = true;
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
        public void createProject(final String project, final Properties projectProperties)
                throws CentralDispatcherException
        {

            fail("unexpected call to createProject");
        }

        @Override
        public INodeSet filterProjectNodes(final String project, final String filter)
                throws CentralDispatcherException
        {
            this.nodesFilter=filter;
            this.nodesProject=project;
            if(!hasFilteredNodes) {
                fail("unexpected call to filterProjectNodes");
            }
            return filteredNodes;
        }

        @Override
        public List<String> listProjectNames() throws CentralDispatcherException {
            return projectNames;
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

    public void testQueueOptionArgs() throws Exception {
        //test action calls
            ExecTool main = newExecTool();
            final testCentralDispatcher test = new testCentralDispatcher();
            main.setCentralDispatcher(test);

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
    public void testQueueOptionArgsSpaces() throws Exception {
            ExecTool main = newExecTool();
            final testCentralDispatcher test = new testCentralDispatcher();
            main.setCentralDispatcher(test);

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
    public void testQueueOptionScriptFile() throws Exception {
            //test script path input available as InputStream when queueing dispatch

            ExecTool main = newExecTool();
            final testCentralDispatcher test = new testCentralDispatcher();
            main.setCentralDispatcher(test);

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
    public void testQueueOptionScriptFileArgs() throws Exception {
            //test script path input available as InputStream when queueing dispatch

        ExecTool main = newExecTool();
        final testCentralDispatcher test = new testCentralDispatcher();
        main.setCentralDispatcher(test);

        //exec the dispatch

        main.run(new String[]{"-p", "testProject", "-s",
            "src/test/resources/com/dtolabs/rundeck/core/cli/test-dispatch-script.txt","--","arg1","arg2"});
        test.assertQueueScriptOnlyCalled();
        assertNotNull("unexpected value: ", test.passedinScript.getArgs());
        assertEquals("unexpected value: ",Arrays.asList("arg1","arg2"), Arrays.asList(test.passedinScript.getArgs()));
        assertNull("unexpected value: "+ test.passedinScript.getScript(), test.passedinScript.getScript());
        assertEquals("unexpected value: " + test.passedinScript.getServerScriptFilePath(),
            new File("src/test/resources/com/dtolabs/rundeck/core/cli/test-dispatch-script.txt").getAbsolutePath(), test.passedinScript.getServerScriptFilePath());
        assertNull(test.passedinScript.getScriptAsStream());
        assertEquals("testProject", test.passedinScript.getFrameworkProject());
    }

    public void testQueueOptionNodeFilters() throws Exception {
            //test the node filter arguments

            ExecTool main = newExecTool();
            final testCentralDispatcher test = new testCentralDispatcher();
            main.setCentralDispatcher(test);

            //exec the dispatch

            main.run(
                new String[]{"-p", "testProject", "-K", "-C", "2", "-I", "hostname1", "-X", "tags=baloney", "-Q", "--",
                    "shell", "command", "string"});
            test.assertQueueScriptOnlyCalled();
            assertNotNull(test.passedinScript.getNodeFilter());
            assertEquals("hostname: hostname1 !tags: baloney", test.passedinScript.getNodeFilter());
            assertEquals(2, test.passedinScript.getNodeThreadcount());
            assertEquals(Boolean.TRUE, test.passedinScript.isKeepgoing());

    }
}
