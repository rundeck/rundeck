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

package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.tools.ant.Project;

import java.io.File;


/**
 * TestFramework 
 */
public class TestFramework extends AbstractBaseTest {

    /**
     * Constructor for test
     *
     * @param name name of test
     */
    public TestFramework(final String name) {
        super(name);
    }

    /**
     * main method
     *
     * @param args cli args
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Junit suite
     *
     * @return test suite
     */
    public static Test suite() {
        return new TestSuite(TestFramework.class);
    }

    protected void setUp() {
        super.setUp();
    }


    /**
     * Test creation
     */
    public void testConstruction() {
        try {
            Framework.getInstance(getBaseDir()+"/test", getFrameworkProjectsBase());
            fail("Framework.getInstance should have thrown an exception for missing framework.properties");
        } catch (Exception e) {
            assertNotNull(e);
        }

        final Framework framework = Framework.getInstance(getBaseDir(), getFrameworkProjectsBase());
        assertNotNull("Framework.getInstance returned null", framework);
        assertTrue("framework.node.hostname property was not set", framework.existsProperty("framework.node.hostname"));
        assertEquals("basedir did not match: " + framework.getBaseDir().getAbsolutePath(), new File(
            getBaseDir()).getAbsolutePath(),
            framework.getBaseDir().getAbsolutePath());
        assertNotNull("authorization manager was null", framework.getAuthorizationMgr());
        assertNotNull("authentication manager was null", framework.getAuthenticationMgr());
        assertNotNull("FrameworkProjectMgr was null", framework.getFrameworkProjectMgr());
    }

    public void testServices() {
        final Framework fw = Framework.getInstance(getBaseDir(), getFrameworkProjectsBase());
        //test default service implementations
        assertNotNull(fw.services);
        assertNotNull(fw.getService("CommandInterpreter"));
        assertNotNull(fw.getService("NodeExecutor"));
        assertNotNull(fw.getService("FileCopier"));
        assertNotNull(fw.getService("NodeDispatcher"));
    }
    public void testSetService() {
        final Framework fw = Framework.getInstance(getBaseDir(), getFrameworkProjectsBase());
        //test removing services
        assertNotNull(fw.services);
        final FrameworkSupportService commandInterpreter = fw.getService("CommandInterpreter");
        assertNotNull(commandInterpreter);
        fw.setService("CommandInterpreter", null);
        assertNull(fw.getService("CommandInterpreter"));
        fw.setService("CommandInterpreter", commandInterpreter);
        assertNotNull(fw.getService("CommandInterpreter"));
        final FrameworkSupportService commandInterpreter2 = fw.getService("CommandInterpreter");
        assertEquals(commandInterpreter, commandInterpreter2);

    }

    /**
     * Test the allowUserInput property of Framework class
     */
    public void testAllowUserInput() {

        final Framework newfw = Framework.getInstance(getBaseDir());
        assertTrue("User input should be enabled by default", newfw.isAllowUserInput());

        newfw.setAllowUserInput(false);
        assertFalse("User input should be disabled", newfw.isAllowUserInput());
        {
            final Project p = new Project();
            assertNull("property should not be set", p.getProperty("framework.userinput.disabled"));

            newfw.configureProject(p);
            assertEquals("Ant property not set to disable user input",
                    "true",
                    p.getProperty("framework.userinput.disabled"));
            assertNotNull("Input Handler should be configured", p.getInputHandler());
            assertEquals("Input Handler isn't expected type",
                    Framework.FailInputHandler.class,
                    p.getInputHandler().getClass());

            newfw.setAllowUserInput(true);
            newfw.configureProject(p);
            assertTrue("Ant property not set to enable user input",
                    "false".equals(p.getProperty("framework.userinput.disabled")) || null == p.getProperty(
                            "framework.userinput.disabled"));
            assertTrue("Input Handler shouldn't be configured",
                    null == p.getInputHandler() || !(p.getInputHandler() instanceof Framework.FailInputHandler));

        }
        {
            final Project p = new Project();
            p.setProperty("framework.userinput.disabled", "true");
            p.setProperty("rdeck.base", getBaseDir());

            final Framework ftest1 = Framework.getInstanceOrCreate(p);
            assertNotNull("instance should be found from PRoject", ftest1);
            assertFalse("framework input should be disabled", ftest1.isAllowUserInput());
            assertNotNull("Input Handler should be configured", p.getInputHandler());
            assertEquals("Input Handler isn't expected type",
                    Framework.FailInputHandler.class,
                    p.getInputHandler().getClass());

        }
        {
            final Project p = new Project();
            p.setProperty("framework.userinput.disabled", "false");
            p.setProperty("rdeck.base", getBaseDir());

            final Framework ftest1 = Framework.getInstanceOrCreate(p);
            assertNotNull("instance should be found from PRoject", ftest1);
            assertTrue("framework input should be enabled", ftest1.isAllowUserInput());
            assertTrue("Input Handler shouldn't be configured",
                    null == p.getInputHandler() || !(p.getInputHandler() instanceof Framework.FailInputHandler));

        }
        {
            final Project p = new Project();
//            p.setResultproperty("framework.userinput.disabled", "false");
            p.setProperty("rdeck.base", getBaseDir());

            final Framework ftest1 = Framework.getInstanceOrCreate(p);
            assertNotNull("instance should be found from PRoject", ftest1);
            assertTrue("framework input should be enabled", ftest1.isAllowUserInput());
            assertTrue("Input Handler shouldn't be configured",
                    null == p.getInputHandler() || !(p.getInputHandler() instanceof Framework.FailInputHandler));

        }
    }

    public void testIsLocal() {
        final Project p = new Project();
        p.setProperty("rdeck.base", getBaseDir());

        final Framework framework = Framework.getInstanceOrCreate(p);
        assertTrue("framework node self-comparison should be true",
                framework.isLocalNode(framework.getNodeDesc()));

        final String frameworkNodename = framework.getFrameworkNodeName();
        final String frameworkHostname = framework.getFrameworkNodeHostname();

        assertTrue("framework node should be local",
                framework.isLocalNode(NodeEntryImpl.create(
                        frameworkHostname,
                        frameworkNodename)));

        assertFalse("incorrect local node result",
                framework.isLocalNode(NodeEntryImpl.create(
                        frameworkHostname,
                        "blahanode")));

        assertTrue("should have matched based on the common node name value",
                framework.isLocalNode(NodeEntryImpl.create(
                        "blahahostname",
                        frameworkNodename)));


    }


}
