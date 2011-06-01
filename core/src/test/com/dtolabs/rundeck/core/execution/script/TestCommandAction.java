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

package com.dtolabs.rundeck.core.execution.script;
/*
* TestCommandAction.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 5, 2010 1:33:16 PM
* $Id$
*/

import com.dtolabs.rundeck.core.NodesetEmptyException;
import com.dtolabs.rundeck.core.cli.NodeCallableFactory;
import com.dtolabs.rundeck.core.cli.NodeDispatcher;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.dispatcher.DispatchedScriptImpl;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.execution.FailedNodesListener;
import com.dtolabs.rundeck.core.utils.NodeSet;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class TestCommandAction extends AbstractBaseTest {
    CommandAction commandAction;
    FrameworkProject project;
    private static final String TEST_PROJ = "TestCommandAction";
    private static final String TEST_NODES_XML =
        "src/test/com/dtolabs/rundeck/core/execution/script/test.nodes.xml";

    public TestCommandAction(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestCommandAction.class);
    }

    protected void setUp() {
        super.setUp();
        project = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(
            TEST_PROJ);
    }

    protected void tearDown() throws Exception {
        FrameworkProject d = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(
            TEST_PROJ);
        FileUtils.deleteDir(d.getBaseDir());
        getFrameworkInstance().getFrameworkProjectMgr().remove(TEST_PROJ);

    }


    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testCreateCommandProxy() throws Exception {

        Framework fw = getFrameworkInstance();
        NodeSet nodeset = new NodeSet();
        {
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null,
                new String[]{"id"}, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            assertTrue("command action should return true", action.isCommandAction());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);

        }
        {
//            main.parseArgs(new String[]{"-p", TEST_PROJ, "--", "id", "&&", "hostname"});
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null,
                new String[]{"id", "&&", "hostname"}, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            assertTrue("command action should return true", action.isCommandAction());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily("unix");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);

        }
        {
//            main.parseArgs(new String[]{"-p", TEST_PROJ, "--", "id"});
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null,
                new String[]{"id"}, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            assertTrue("command action should return true", action.isCommandAction());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily("windows");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);

        }
        {
//            main.parseArgs(new String[]{"-p", TEST_PROJ, "--", "id", "potato", "hell"});
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null,
                new String[]{"id", "potato", "hell"}, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            assertTrue("command action should return true", action.isCommandAction());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily("windows");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);

        }
        {
//            main.parseArgs(new String[]{"-p", TEST_PROJ, "--", "echo", "test belief"});
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null,
                new String[]{"echo", "test belief"}, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            assertTrue("command action should return true", action.isCommandAction());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily("windows");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);

        }
    }

    public void testDoAction() throws Exception {

        Framework fw = getFrameworkInstance();
        {
            NodeSet nodeset = new NodeSet();
            nodeset.setSingleNodeName("DNENODE");
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null,
                new String[]{"id"}, 0);
            CommandAction action = new CommandAction(fw, dsc, null);

            final boolean wascalled[]={false};
            action.setNodeDispatcher(new NodeDispatcher() {
                public void executeNodedispatch(Project project, Framework fwk, Collection<INodeEntry> nodes, int threadcount,
                                                boolean keepgoing, FailedNodesListener failedListener,
                                                NodeCallableFactory factory) {
                    wascalled[0] = true;
                }
            });
            try {
                action.doAction();
                fail("should not succeed.");
            } catch (Exception e) {
                e.printStackTrace(System.err);
                assertTrue(e instanceof NodesetEmptyException);
            }
            assertFalse(wascalled[0]);

        }


        File destNodesFile = new File(project.getEtcDir(), "resources.xml");
        File testNodesFile = new File(TEST_NODES_XML);
        try {
            FileUtils.copyFileStreams(testNodesFile, destNodesFile);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        {
            NodeSet nodeset = new NodeSet();
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null,
                new String[]{"id"}, 0);
            CommandAction action = new CommandAction(fw, dsc, null);

            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            final boolean wascalled[] = {false};
            action.setNodeDispatcher(new NodeDispatcher() {
                public void executeNodedispatch(Project project, Framework fwk, Collection<INodeEntry> nodes, int threadcount,
                                                boolean keepgoing, FailedNodesListener failedListener,
                                                NodeCallableFactory factory) {
                    wascalled[0] = true;
                }
            });
            action.doAction();
            assertTrue(wascalled[0]);

        }
    }
}