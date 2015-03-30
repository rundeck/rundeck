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
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
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
        assertTrue("framework.server.hostname property was not set", framework.hasProperty("framework.server.hostname"));
        assertEquals("basedir did not match: " + framework.getBaseDir().getAbsolutePath(), new File(
            getBaseDir()).getAbsolutePath(),
            framework.getBaseDir().getAbsolutePath());
        assertNotNull("FrameworkProjectMgr was null", framework.getFrameworkProjectMgr());
    }

    public void testServices() {
        final Framework fw = Framework.getInstance(getBaseDir(), getFrameworkProjectsBase());
        //test default service implementations
        assertNotNull(fw.getService(ServiceNameConstants.WorkflowStep));
        assertNotNull(fw.getService(ServiceNameConstants.WorkflowNodeStep));
        assertNotNull(fw.getService(ServiceNameConstants.NodeExecutor));
        assertNotNull(fw.getService(ServiceNameConstants.FileCopier));
        assertNotNull(fw.getService(ServiceNameConstants.NodeDispatcher));
        assertNotNull(fw.getService(ServiceNameConstants.ResourceModelSource));
        assertNotNull(fw.getService(ServiceNameConstants.ResourceFormatParser));
        assertNotNull(fw.getService(ServiceNameConstants.ResourceFormatGenerator));
    }
    public void testSetService() {
        final Framework fw = Framework.getInstance(getBaseDir(), getFrameworkProjectsBase());
        //test removing services
        final FrameworkSupportService commandInterpreter = fw.getService(ServiceNameConstants.WorkflowNodeStep);
        assertNotNull(commandInterpreter);
        fw.setService(ServiceNameConstants.WorkflowNodeStep, null);
        assertNull(fw.getService(ServiceNameConstants.WorkflowNodeStep));
        fw.setService(ServiceNameConstants.WorkflowNodeStep, commandInterpreter);
        assertNotNull(fw.getService(ServiceNameConstants.WorkflowNodeStep));
        final FrameworkSupportService commandInterpreter2 = fw.getService(ServiceNameConstants.WorkflowNodeStep);
        assertEquals(commandInterpreter, commandInterpreter2);

    }


    public void testIsLocal() {
        final Framework framework = Framework.getInstance(getBaseDir(),null);
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

    public void testCreatePropertyRetriever() {
        PropertyRetriever propertyRetriever = Framework.createPropertyRetriever(new File(getBaseDir()));
        assertNotNull(propertyRetriever.getProperty("framework.tmp.dir"));
        assertFalse(propertyRetriever.getProperty("framework.tmp.dir").contains("${"));
    }


}
