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
* TestScriptfileAction.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 5, 2010 12:41:00 PM
* $Id$
*/

import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.cli.ExecTool;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.dispatcher.DispatchedScriptImpl;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.utils.NodeSet;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Sequential;

import java.io.File;
import java.io.IOException;

public class TestScriptfileAction extends AbstractBaseTest {
    ScriptfileAction scriptfileAction;
    private File testScriptFile;

    public TestScriptfileAction(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestScriptfileAction.class);
    }

    protected void setUp() {
        super.setUp();
        FrameworkProject d = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject("TestScriptfileAction");
        File projectEtcDir = new File(d.getBaseDir(), "etc");

        testScriptFile = new File(d.getBaseDir(), "test.sh");
        try {
            testScriptFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void tearDown() throws Exception {
        FrameworkProject d = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(
            "TestScriptfileAction");
        FileUtils.deleteDir(d.getBaseDir());
        getFrameworkInstance().getFrameworkProjectMgr().remove("TestScriptfileAction");
        
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    
    public void testCreateRemoteCommandProxy() throws Exception {
        Framework fw = getFrameworkInstance();
        NodeSet nodeset = new NodeSet();
        {
//            main.parseArgs(new String[]{"-p", "TestCtlExec", "-s", testScriptFile.getAbsolutePath()});

            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, "TestCtlExec", null, null, testScriptFile.getAbsolutePath(),
                null, 0);
            ScriptfileAction action = new ScriptfileAction(fw, dsc, null);

            final NodeEntryImpl nodeentry = new NodeEntryImpl("remotehost", "remotehost");
            nodeentry.setOsFamily("unix");
            Task t = action.createRemoteCommandProxy(nodeentry, new Project());
            assertNotNull("shouldn't be null", t);
            assertTrue("wrong task type", t instanceof Sequential);
        }
        {
            ExecTool main = new ExecTool(fw);
            main.parseArgs(new String[]{"-p", "TestCtlExec", "-s", testScriptFile.getAbsolutePath()});
            ScriptfileAction action = new ScriptfileAction(fw, main, null);
            assertNull("incorrect args", main.getArgsDeferred());
            final NodeEntryImpl nodeentry = new NodeEntryImpl("remotehost", "remotehost");
            nodeentry.setOsFamily("windows");
            Task t = action.createRemoteCommandProxy(nodeentry, new Project());
            assertNotNull("shouldn't be null", t);
            assertTrue("wrong task type", t instanceof Sequential);
        }
        {
            ExecTool main = new ExecTool(fw);
            main.parseArgs(new String[]{"-p", "TestCtlExec", "-s", testScriptFile.getAbsolutePath()});
            ScriptfileAction action = new ScriptfileAction(fw, main, null);
            assertNull("incorrect args", main.getArgsDeferred());
            final NodeEntryImpl nodeentry = new NodeEntryImpl("remotehost", "remotehost");
            nodeentry.setOsFamily("INVALID");
            try {
                Task t = action.createRemoteCommandProxy(nodeentry, new Project());
                fail("createRemoteCommandProxy should fail with invalid OS");
            } catch (CoreException exc) {
                assertNotNull("shouldn't be null", exc);
            }

        }
    }

    /**
     * Test the createLocalCommandProxy method of the ScriptFile action class
     *
     * @throws Exception
     */
    public void testScriptFileActionLocal() throws Exception {
        Framework fw = getFrameworkInstance();

        {
            ExecTool main = new ExecTool(fw);
            main.parseArgs(new String[]{"-p", "TestCtlExec", "-s", testScriptFile.getAbsolutePath()});


            ScriptfileAction action = new ScriptfileAction(fw, main, null);

            assertFalse("scriptfile action should return false", action.isCommandAction());
            assertNull("incorrect args", main.getArgsDeferred());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily("unix");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);

        }
        {
            ExecTool main = new ExecTool(fw);
            main.parseArgs(new String[]{"-p", "TestCtlExec", "-s", testScriptFile.getAbsolutePath(),"--","test","args"});


            ScriptfileAction action = new ScriptfileAction(fw, main, null);

            assertFalse("scriptfile action should return false", action.isCommandAction());
            assertNotNull("incorrect args", main.getArgsDeferred());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily("unix");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);

        }

        {
            ExecTool main = new ExecTool(fw);
            main.parseArgs(new String[]{"-p", "TestCtlExec", "-s", testScriptFile.getAbsolutePath()});

            ScriptfileAction action = new ScriptfileAction(fw, main, null);

            assertFalse("scriptfile action should return false", action.isCommandAction());
            assertNull("incorrect args", main.getArgsDeferred());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily("windows");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);

        }
    }
}