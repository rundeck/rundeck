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
import com.dtolabs.rundeck.core.utils.FileUtils;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * TestFrameworkProject
 */
public class TestFrameworkProject extends AbstractBaseTest {
    private final String PROJECT_NAME = "TestFrameworkProject";

    File projectBasedir;
    File nodesfile;


    public TestFrameworkProject(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestFrameworkProject.class);
    }

    protected void setUp() {
        super.setUp();
        projectBasedir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        nodesfile = new File(projectBasedir, "/etc/resources.xml");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        File projectdir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        FileUtils.deleteDir(projectdir);
    }

    public void testCreateDepotStructure() throws IOException {
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        if (projectDir.exists()) {
            FileUtils.deleteDir(projectDir);
        }
        FrameworkProject.createFileStructure(projectDir);
        assertTrue(new File(projectDir, FrameworkProject.ETC_DIR_NAME).exists());
    }

    public void testConstruction() {
        if (projectBasedir.exists()) {
            projectBasedir.delete();
        }
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                         new File(getFrameworkProjectsBase()),
                                         getFrameworkInstance().getFrameworkProjectMgr());
        assertTrue("incorrect project.dir", project.getBaseDir().equals(new File(getFrameworkProjectsBase(), PROJECT_NAME)));
        assertTrue("number of types: " + project.listChildren().size() + " should be 0",
                   project.listChildren().size() == 0);
    }


    public void testChildCouldBeLoaded() {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                         new File(getFrameworkProjectsBase()),
                                         getFrameworkInstance().getFrameworkProjectMgr());
        assertFalse(project.childCouldBeLoaded("HahaType"));

        final File deployments = new File(project.getBaseDir(), "resources");
        final File hahadir = new File(deployments, "HahaType");
        hahadir.mkdirs();

        assertTrue(project.childCouldBeLoaded("HahaType"));
        hahadir.delete();
        assertFalse(project.childCouldBeLoaded("HahaType"));

    }

    public void testListChildNames() {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                         new File(getFrameworkProjectsBase()),
                                         getFrameworkInstance().getFrameworkProjectMgr());

        assertEquals(0, project.listChildNames().size());
        final File deployments = new File(project.getBaseDir(), "resources");
        final File hahadir = new File(deployments, "HahaType");
        hahadir.mkdirs();
        assertEquals(1, project.listChildNames().size());
        assertTrue(project.listChildNames().contains("HahaType"));
        hahadir.delete();
        assertEquals(0, project.listChildNames().size());

    }




    public void testProperties() throws IOException {
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        FrameworkProject.createFileStructure(projectDir);
        final File etcDir = new File(projectDir, "etc");
        final File projectPropertyFile = new File(etcDir, "project.properties");
        final Properties p = new Properties();
        p.put("project.dir", "${framework.projects.dir}/${project.name}");
        p.put("project.resources.dir", "${project.dir}/resources");
        p.put("project.etc.dir", "${project.dir}/etc");
        p.put("project.resources.file", "${project.etc.dir}/resources.properties");
        p.store(new FileOutputStream(projectPropertyFile), "test properties");

        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                         new File(getFrameworkProjectsBase()),
                                         getFrameworkInstance().getFrameworkProjectMgr());

        assertEquals(project.getProperty("project.dir"), projectDir.getAbsolutePath());
    }




    
    public void testGetNodes() throws Exception {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                         new File(getFrameworkProjectsBase()),
                                         getFrameworkInstance().getFrameworkProjectMgr());
        FileUtils.copyFileStreams(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"), nodesfile);
        assertTrue(nodesfile.exists());
        Nodes nodes = project.getNodes();
        assertNotNull(nodes);
        assertEquals("nodes was incorrect size", 2, nodes.listNodes().size());
        assertTrue("nodes did not have correct test node1", nodes.hasNode("testnode1"));
        assertTrue("nodes did not have correct test node2", nodes.hasNode("testnode2"));
    }


    public void testGenerateProjectPropertiesFile() throws IOException {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                         new File(getFrameworkProjectsBase()),
                                         getFrameworkInstance().getFrameworkProjectMgr());
        boolean overwrite = true;
        project.generateProjectPropertiesFile(overwrite);

        final File propFile = new File(project.getEtcDir(), "project.properties");
        assertTrue("project.properties file was not generated",
                propFile.exists());

        final Properties p = new Properties();
        p.load(new FileInputStream(propFile));
        assertTrue(p.containsKey("project.resources.file"));


        System.out.println("TEST: propertyFile="+project.getPropertyFile());
    }
}
