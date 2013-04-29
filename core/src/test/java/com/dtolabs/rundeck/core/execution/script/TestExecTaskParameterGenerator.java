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
* TestExecTaskParameterGenerator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 15, 2010 5:59:20 PM
* $Id$
*/

import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;

public class TestExecTaskParameterGenerator extends TestCase {
    private File testScriptFile;

    public TestExecTaskParameterGenerator(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestExecTaskParameterGenerator.class);
    }

    protected void setUp() throws Exception {
        testScriptFile = File.createTempFile("TestExecTaskParameterGenerator",".sh");
    }

    protected void tearDown() throws Exception {
        if(null!=testScriptFile){
            testScriptFile.delete();
        }
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    static void assertArrayEquals(String desc, String[] expected, String[] actual) {
        assertEquals(desc, expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            String expect = expected[i];
            String actualStr = actual[i];
            assertEquals("pos " + i + ": " + desc, expect, actualStr);
        }
    }

    public void testGenerateCommand() throws Exception {
        /*
         * test invoke using isCommand==true
         */
        {
            // basic command input with default OS-family for the node.

            final boolean testIsCommand = true;
            final File testScriptFile = null;
            final String[] testArgs = {"id"};

            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);

            final ExecTaskParameterGenerator testGen = new ExecTaskParameterGeneratorImpl(
            );
            final ExecTaskParameters params = testGen.generate(nodeentry, testIsCommand, testScriptFile,
                testArgs);

            assertEquals("Wrong executable for local unix command", "/bin/sh", params.getCommandexecutable());
            assertArrayEquals("Wrong argline for local command", new String[]{"-c", "id"}, params.getCommandArgs());
        }
        {
            //command input with unix os-family

            final String testOs = "unix";
            final boolean testIsCommand = true;
            final File testScriptFile = null;
            final String[] testArgs = {"id","&&","hostname"};

            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily(testOs);

            final ExecTaskParameterGenerator testGen = new ExecTaskParameterGeneratorImpl(
            );
            final ExecTaskParameters params = testGen.generate(nodeentry, testIsCommand, testScriptFile,
                testArgs);

            assertEquals("Wrong executable for local unix command", "/bin/sh", params.getCommandexecutable());
            assertArrayEquals("Wrong argline for local command", new String[]{"-c","id && hostname"},
                    params.getCommandArgs());
        }
        {
            //basic command with windows os-family

            final String testOs = "windows";
            final boolean testIsCommand = true;
            final File testScriptFile = null;
            final String[] testArgs = {"id"};

            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily(testOs);

            final ExecTaskParameterGenerator testGen = new ExecTaskParameterGeneratorImpl(
            );
            final ExecTaskParameters params = testGen.generate(nodeentry, testIsCommand, testScriptFile,
                testArgs);

            assertEquals("Wrong executable for local windows command", "cmd.exe", params.getCommandexecutable());
            assertArrayEquals("Wrong argline for local command", new String[]{"/c", "id"}, params.getCommandArgs());

        }
        {
            // command with windows os-family

            final String testOs = "windows";
            final boolean testIsCommand = true;
            final File testScriptFile = null;
            final String[] testArgs = {"id","potato","hell"};

            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily(testOs);

            final ExecTaskParameterGenerator testGen = new ExecTaskParameterGeneratorImpl(
            );
            final ExecTaskParameters params = testGen.generate(nodeentry, testIsCommand, testScriptFile,
                testArgs);

            assertEquals("Wrong executable for local windows command", "cmd.exe", params.getCommandexecutable());
            assertArrayEquals("Wrong argline for local command", new String[]{"/c", "id", "potato", "hell"},
                    params.getCommandArgs());

        }
        {// quoted command with windows os-family

            final String testOs = "windows";
            final boolean testIsCommand = true;
            final File testScriptFile = null;
            final String[] testArgs = {"echo","test belief"};

            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily(testOs);

            final ExecTaskParameterGenerator testGen = new ExecTaskParameterGeneratorImpl(
            );
            final ExecTaskParameters params = testGen.generate(nodeentry, testIsCommand, testScriptFile,
                testArgs);

            assertEquals("Wrong executable for local windows command", "cmd.exe", params.getCommandexecutable());
            assertArrayEquals("Wrong argline for local command", new String[]{"/c", "echo","test belief"}, params.getCommandArgs());

        }
    }
    public void testGenerateScript() throws Exception{
        /**
         *  test invoke using isCommand==false
         */
        {
            //scriptfile with unix os-family

            final String testOs = "unix";
            final boolean testIsCommand = false;
            final File testScriptFile = this.testScriptFile;
            final String[] testArgs = null;

            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily(testOs);

            final ExecTaskParameterGenerator testGen = new ExecTaskParameterGeneratorImpl(
            );
            final ExecTaskParameters params = testGen.generate(nodeentry, testIsCommand, testScriptFile,
                testArgs);
            assertEquals("Wrong executable for local unix command", testScriptFile.getAbsolutePath(),
                params.getCommandexecutable());
            assertNull("Wrong argline for local command", params.getCommandArgs());
        }
        {
            //scriptfile with args, with unix os-family

            final String testOs = "unix";
            final boolean testIsCommand = false;
            final File testScriptFile = this.testScriptFile;
            final String[] testArgs = new String[]{"test", "args"};

            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily(testOs);

            final ExecTaskParameterGenerator testGen = new ExecTaskParameterGeneratorImpl(
            );
            final ExecTaskParameters params = testGen.generate(nodeentry, testIsCommand, testScriptFile,
                testArgs);


            assertEquals("Wrong executable for local unix command", testScriptFile.getAbsolutePath(),
                params.getCommandexecutable());
            assertArrayEquals("Wrong argline for local command", new String[]{"test","args"}, params.getCommandArgs());
        }

        {
            //scriptfile with windows os-family

            final String testOs = "windows";
            final boolean testIsCommand = false;
            final File testScriptFile = this.testScriptFile;
            final String[] testArgs = null;

            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily(testOs);

            final ExecTaskParameterGenerator testGen = new ExecTaskParameterGeneratorImpl(
            );
            final ExecTaskParameters params = testGen.generate(nodeentry, testIsCommand, testScriptFile,
                testArgs);

            assertEquals("Wrong executable for local unix command", "cmd.exe", params.getCommandexecutable());
            assertArrayEquals("Wrong argline for local command", new String[]{"/c", testScriptFile.getAbsolutePath()}, params.getCommandArgs());
        }
        {
            //scriptfile with args, with windows os-family

            final String testOs = "windows";
            final boolean testIsCommand = false;
            final File testScriptFile = this.testScriptFile;
            final String[] testArgs = new String[]{"test","args","something"};

            final NodeEntryImpl nodeentry = new NodeEntryImpl(AbstractBaseTest.localNodeHostname, AbstractBaseTest.localNodeHostname);
            nodeentry.setOsFamily(testOs);

            final ExecTaskParameterGenerator testGen = new ExecTaskParameterGeneratorImpl(
            );
            final ExecTaskParameters params = testGen.generate(nodeentry, testIsCommand, testScriptFile,
                testArgs);

            assertEquals("Wrong executable for local unix command", "cmd.exe", params.getCommandexecutable());
            assertArrayEquals("Wrong argline for local command", new String[]{"/c", testScriptFile.getAbsolutePath(),
                    "test", "args", "something"}, params.getCommandArgs());
        }
    }
}
