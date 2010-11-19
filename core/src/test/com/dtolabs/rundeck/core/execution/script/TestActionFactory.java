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
* TestActionFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 8, 2010 3:50:00 PM
* $Id$
*/

import com.dtolabs.rundeck.core.cli.ExecTool;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.DispatchedScriptImpl;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.utils.NodeSet;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;

public class TestActionFactory extends AbstractBaseTest {
    ActionFactory actionFactory;
    File testScriptFile;

    public TestActionFactory(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestActionFactory.class);
    }

    protected void setUp() {
        super.setUp();
        testScriptFile=new File("src/test/com/dtolabs/rundeck/core/cli/test-dispatch-script.txt");
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }



    public void testCreateAction() throws Exception {
        final Framework fw = getFrameworkInstance();
        { //fail: no script or command
            IDispatchedScript context = new DispatchedScriptImpl(null, "TestCtlExec", null, null, null, null, 1);
            IAction iaction = null;
            try {
                iaction = ActionFactory.createAction(context, fw, null);
                fail("incorrect params should have failed createAction");
            } catch (ExecutionException e) {
                assertNotNull(e);
                assertEquals("No script or command specified", e.getMessage());
            }

        }
        {   //fail no nodeset
            IDispatchedScript context = new DispatchedScriptImpl(null, "TestCtlExec", null, null, null, new String[]{"cmd1"}, 1);
            IAction iaction = null;
            try {
                iaction = ActionFactory.createAction(context, fw, null);
                fail("incorrect params should have failed createAction");
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
                assertEquals("dispatched script context requires nodeset", e.getMessage());
            }

        }
        final NodeSet nodeset = new NodeSet();
        {   //succeed with command
            IDispatchedScript context = new DispatchedScriptImpl(nodeset, "TestCtlExec", null, null, null,
                new String[]{"cmd1"}, 1);
            IAction iaction = null;
            try {
                iaction = ActionFactory.createAction(context, fw, null);
                assertNotNull(iaction);
                assertEquals(CommandAction.class, iaction.getClass());
            } catch (ExecutionException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
        {//succeed with command
            IDispatchedScript context = new DispatchedScriptImpl(nodeset, "TestCtlExec", null, null, null,
                new String[]{"a","script"}, 1);
            IAction iaction = null;
            try {
                iaction = ActionFactory.createAction(context, fw, null);
                assertNotNull(iaction);
                assertEquals(CommandAction.class, iaction.getClass());
            } catch (ExecutionException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
        {//succeed with script inputstream
            IDispatchedScript context = new DispatchedScriptImpl(nodeset, "TestCtlExec", null, new FileInputStream(
                testScriptFile), null, null, 1);
            IAction iaction = null;
            try {
                iaction = ActionFactory.createAction(context, fw, null);
                assertNotNull(iaction);
                assertEquals(ScriptfileAction.class, iaction.getClass());
            } catch (ExecutionException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
        {//succeed with script file path
            IDispatchedScript context = new DispatchedScriptImpl(nodeset, "TestCtlExec", null, null, testScriptFile.getAbsolutePath(), null, 1);
            IAction iaction = null;
            try {
                iaction = ActionFactory.createAction(context, fw, null);
                assertNotNull(iaction);
                assertEquals(ScriptfileAction.class, iaction.getClass());
            } catch (ExecutionException e) {
                fail("unexpected exception: " + e.getMessage());
            }

        }
        {   //fail due to no context
            ExecTool main = new ExecTool(fw);
            main.parseArgs(new String[]{"-p", "TestCtlExec"});
            IAction iaction = null;
            try {
                iaction = ActionFactory.createAction(main, fw, null);
                fail("incorrect params should have failed createAction");
            } catch (ExecutionException e) {
                assertNotNull(e);
            }

        }
        {
            ExecTool main = new ExecTool(fw);
            main.parseArgs(new String[]{"-p", "TestCtlExec","-s", testScriptFile.getAbsolutePath()});
            IAction iaction = ActionFactory.createAction(main, fw, null);
            assertTrue("incorrect type", iaction instanceof ScriptfileAction);
        }
        {
            ExecTool main = new ExecTool(fw);
            main.parseArgs(new String[]{"-p", "TestCtlExec", "--", "test.sh", "blah"});
            IAction iaction = ActionFactory.createAction(main, fw, null);
            assertTrue("incorrect type", iaction instanceof CommandAction);
        }
        {
            ExecTool main = new ExecTool(fw);
            main.parseArgs(new String[]{"-p", "TestCtlExec", "-S"});
            main.setInlineScriptContent("test script content");
            IAction iaction = ActionFactory.createAction(main, fw, null);
            assertTrue("incorrect type", iaction instanceof ScriptfileAction);
        }
        {
            ExecTool main = new ExecTool(fw);
            main.parseArgs(
                new String[]{"-p", "TestCtlExec", "-I", "tags=priority1", "-s", testScriptFile.getAbsolutePath()});
            IAction iaction = ActionFactory.createAction(main, fw, null);
            assertTrue("incorrect type", iaction instanceof ScriptfileAction);

        }
    }

}