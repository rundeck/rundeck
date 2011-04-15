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
        final ProjectTool setup = new ProjectTool();
        assertNotNull(setup);
    }


    public void testCreateActions() throws Throwable {


        final ProjectTool setup = new ProjectTool();
        final String[] args = new String[]{
            "-o", "-p", PROJECT,
        };
        setup.parseArgs(args);
        Action create = setup.createAction(ProjectTool.ACTION_CREATE);
        assertTrue(create instanceof CreateAction);
        create.exec();

    }


    public void testGo() {
        final File dir = new File(projectsBasedir, PROJECT);
        if (dir.exists()) {
            FileUtils.deleteDir(dir);
        }
        final ProjectTool setup = new ProjectTool();
        final String[] args = new String[]{
            "-p", PROJECT,
            "-b", setupXml.getAbsolutePath(),
            "-o"
        };
        setup.parseArgs(args);
        setup.executeAction();

        assertTrue("project did not exist", getFrameworkInstance().getFrameworkProjectMgr().existsFrameworkProject(PROJECT));

        final FrameworkProject d = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(PROJECT);
        assertEquals("project name did not match", d.getName(), PROJECT);
    }


}
