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

package com.dtolabs.rundeck.core.cli.project;

import com.dtolabs.rundeck.core.cli.Action;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

/**
 * TestProjectTool
 */
public class TestProjectTool extends AbstractBaseTest {

    private static final String PROJECT = "TestProjectTool";
    File projectsBasedir;
    private File setupXml;

    public TestProjectTool(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtils.deleteDir(new File(projectsBasedir, PROJECT));
    }

    protected void setUp() {
        super.setUp();
        projectsBasedir = new File(getFrameworkProjectsBase());
        setupXml = new File("src/ant/controllers/rdeck/projectsetupCmd.xml");
    }

    public static Test suite() {
        return new TestSuite(TestProjectTool.class);
    }


    public void testConstruction() {
        final ProjectTool setup = createProjectTool();
        assertNotNull(setup);
    }

    private ProjectTool createProjectTool() {
        return new ProjectTool(new File(getBaseDir()));
    }


    public void testCreateActions() throws Throwable {


        final ProjectTool setup = createProjectTool();
        final String[] args = new String[]{
            "-o", "-p", PROJECT,
        };
        setup.parseArgs(args);
        Action create = setup.createAction(ProjectTool.ACTION_CREATE);
        assertTrue(create instanceof CreateAction);
        create.exec();

    }

    public void testCreateActionProperties() throws Throwable {


        final ProjectTool setup = createProjectTool();
        final String[] args = new String[]{
            "-o", "-p", PROJECT,
            "--test1=value",
            "--project.blah=something"
        };
        setup.parseArgs(args);
        Action create = setup.createAction(ProjectTool.ACTION_CREATE);
        assertTrue(create instanceof CreateAction);
        CreateAction caction = (CreateAction) create;
        assertNotNull(caction.getProperties());
        assertEquals(2, caction.getProperties().size());
        assertTrue(caction.getProperties().containsKey("test1"));
        assertEquals("value", caction.getProperties().getProperty("test1"));
        assertTrue(caction.getProperties().containsKey("project.blah"));
        assertEquals("something", caction.getProperties().getProperty("project.blah"));

    }

    public void testParsePropertyArg() throws Exception {
        final Properties properties = new Properties();
        ProjectTool.parsePropertyArg(properties, "blah");
        assertEquals(0, properties.size());
        ProjectTool.parsePropertyArg(properties, "--blah");
        assertEquals(0, properties.size());
        ProjectTool.parsePropertyArg(properties, "--blah=");
        assertEquals(1, properties.size());
        assertTrue(properties.containsKey("blah"));
        assertEquals("", properties.getProperty("blah"));
        ProjectTool.parsePropertyArg(properties, "--blah=zamboni");
        assertEquals(1, properties.size());
        assertTrue(properties.containsKey("blah"));
        assertEquals("zamboni", properties.getProperty("blah"));
        ProjectTool.parsePropertyArg(properties, "--blah=zam=boni");
        assertEquals(1, properties.size());
        assertTrue(properties.containsKey("blah"));
        assertEquals("zam=boni", properties.getProperty("blah"));
        ProjectTool.parsePropertyArg(properties, "--blah=zam-boni");
        assertEquals(1, properties.size());
        assertTrue(properties.containsKey("blah"));
        assertEquals("zam-boni", properties.getProperty("blah"));
        ProjectTool.parsePropertyArg(properties, "--blah=zam boni");
        assertEquals(1, properties.size());
        assertTrue(properties.containsKey("blah"));
        assertEquals("zam boni", properties.getProperty("blah"));
    }

    public void testParseExtendedProperties() throws Exception {
        final Properties properties = new Properties();
        String[] args1={"-z","--test1=1","--blah","something","--test2=2"};
        ProjectTool.parseExtendedProperties(args1, properties);
        assertEquals(2, properties.size());
        assertTrue(properties.containsKey("test1"));
        assertEquals("1", properties.getProperty("test1"));
        assertTrue(properties.containsKey("test2"));
        assertEquals("2", properties.getProperty("test2"));

    }
    public void testRemoveExtendedProperties() throws Exception {
        String[] args1={"-z","--test1=1","--blah","something","--test2=2"};
        String[] args2=ProjectTool.removeExtendedProperties(args1);
        assertEquals(3, args2.length);
        assertEquals(Arrays.asList("-z", "--blah", "something"), Arrays.asList(args2));
    }

    public void testIsExtendedPropertyArg() throws Exception {
        assertFalse(ProjectTool.isExtendedPropertyArg("--a"));
        assertFalse(ProjectTool.isExtendedPropertyArg("-a"));
        assertFalse(ProjectTool.isExtendedPropertyArg("a"));
        assertFalse(ProjectTool.isExtendedPropertyArg("-a="));
        assertFalse(ProjectTool.isExtendedPropertyArg("-a=b"));
        assertFalse(ProjectTool.isExtendedPropertyArg("-a=b=c"));
        assertFalse(ProjectTool.isExtendedPropertyArg("a="));
        assertFalse(ProjectTool.isExtendedPropertyArg("a=b"));
        assertFalse(ProjectTool.isExtendedPropertyArg("a=b=c"));
        assertTrue(ProjectTool.isExtendedPropertyArg("--a="));
        assertTrue(ProjectTool.isExtendedPropertyArg("--a=b"));
        assertTrue(ProjectTool.isExtendedPropertyArg("--a=b=c"));
    }


    public void testGo() {
        final File dir = new File(projectsBasedir, PROJECT);
        if (dir.exists()) {
            FileUtils.deleteDir(dir);
        }
        final ProjectTool setup = createProjectTool();
        final String[] args = new String[]{
            "-p", PROJECT,
            "-b", setupXml.getAbsolutePath(),
            "-o"
        };
        setup.parseArgs(args);
        setup.executeAction();

        assertTrue("project did not exist", getFrameworkInstance().getFrameworkProjectMgr().existsFrameworkProject(
            PROJECT));

        final FrameworkProject d = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(PROJECT);
        assertEquals("project name did not match", d.getName(), PROJECT);
    }


}
