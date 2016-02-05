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
import java.io.IOException;


/**
 * TestFrameworkProjectMgr
 */
public class TestFrameworkProjectMgr extends AbstractBaseTest {

    public static final String PROJECT_NAME = "TestFrameworkProjectMgr";
    public static final String PROJECT_NAME2 = "TestDepotResourceMgr2";



    public TestFrameworkProjectMgr(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestFrameworkProjectMgr.class);
    }

    protected void setUp() {
        super.setUp();
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        projectDir.mkdir();
        final File propsFile = new File(projectDir, "etc/project.properties");
        try {
            propsFile.getParentFile().mkdirs();
            propsFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final File projectDir2 = new File(getFrameworkProjectsBase(), PROJECT_NAME2);
        if(projectDir2.exists()){
            FileUtils.deleteDir(projectDir2);
        }
    }

    protected void tearDown() throws Exception {
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        final File projectDir2 = new File(getFrameworkProjectsBase(), PROJECT_NAME2);
        FileUtils.deleteDir(projectDir);
        FileUtils.deleteDir(projectDir2);
        super.tearDown();
    }

    public void testConstruction() {
        final FrameworkProjectMgr mgr = FrameworkProjectMgr.create("projectMgr",
                                                                   new File(getFrameworkProjectsBase()),
                                                                   getFrameworkInstance(),
                                                                   FrameworkFactory.createNodesFactory(
                                                                           getFrameworkInstance().getFilesystemFramework())
        );
        assertEquals("mgr.getBaseDir() did not match exepcted", mgr.getBaseDir(), new File(getFrameworkProjectsBase()));

        assertEquals("expected 1 projects listed. number found: " + mgr.listFrameworkProjects().size()
                + " projects="+mgr.listChildNames(),1,
                   mgr.listFrameworkProjects().size() );
    }

    public void testChildCouldBeLoaded() throws IOException {
        final FrameworkProjectMgr mgr = FrameworkProjectMgr.create("projectMgr",
                                                                   new File(getFrameworkProjectsBase()),
                                                                   getFrameworkInstance(),
                                                                   FrameworkFactory.createNodesFactory(
                                                                           getFrameworkInstance().getFilesystemFramework())
        );
        assertTrue(mgr.childCouldBeLoaded(PROJECT_NAME));
        assertFalse(mgr.childCouldBeLoaded(PROJECT_NAME2));
        assertFalse(mgr.childCouldBeLoaded(PROJECT_NAME2));
        final File projectDir2 = new File(mgr.getBaseDir(), PROJECT_NAME2);
        File propsfile = fakeCreateProject(projectDir2);

        assertTrue(mgr.childCouldBeLoaded(PROJECT_NAME2));
        propsfile.delete();
        propsfile.getParentFile().delete();
        projectDir2.delete();

    }

    private File fakeCreateProject(final File projectDir2) throws IOException {
        projectDir2.mkdir();
        File propsfile = new File(projectDir2, "etc/project.properties");
        assertTrue(propsfile.getParentFile().mkdirs());
        assertTrue(propsfile.createNewFile());
        return propsfile;
    }

    public void testListChildNames() throws IOException {
        final FrameworkProjectMgr mgr = FrameworkProjectMgr.create("projectMgr",
                                                                   new File(getFrameworkProjectsBase()),
                                                                   getFrameworkInstance(),
                                                                   FrameworkFactory.createNodesFactory(
                                                                           getFrameworkInstance().getFilesystemFramework())
        );
        assertTrue(mgr.listChildNames().contains(PROJECT_NAME));
        assertFalse(mgr.listChildNames().contains(PROJECT_NAME2));
        final File projectDir2 = new File(mgr.getBaseDir(), PROJECT_NAME2);
        File propsfile = fakeCreateProject(projectDir2);
        assertTrue(mgr.listChildNames().contains(PROJECT_NAME));
        assertTrue(mgr.listChildNames().contains(PROJECT_NAME2));
        projectDir2.delete();
    }

    public void testLoadChild() throws IOException {
        final FrameworkProjectMgr mgr = FrameworkProjectMgr.create("projectMgr",
                                                                   new File(getFrameworkProjectsBase()),
                                                                   getFrameworkInstance(),
                                                                   FrameworkFactory.createNodesFactory(
                                                                           getFrameworkInstance().getFilesystemFramework())
        );
        try {
            IFrameworkResource res = mgr.loadChild(PROJECT_NAME);
            assertNotNull(res);
            assertTrue(res instanceof FrameworkProject);
            assertEquals(res.getName(), PROJECT_NAME);
        } catch (FrameworkResourceException e) {
            fail("loadChild threw exception: " + e.getMessage());
        }

        IFrameworkResource resa = mgr.loadChild(PROJECT_NAME2);
        assertNull(resa);

        final File projectDir2 = new File(mgr.getBaseDir(), PROJECT_NAME2);
        fakeCreateProject(projectDir2);
        try {
            IFrameworkResource res = mgr.loadChild(PROJECT_NAME);
            assertNotNull(res);
            assertTrue(res instanceof FrameworkProject);
            assertEquals(res.getName(), PROJECT_NAME);
        } catch (FrameworkResourceException e) {
            fail("loadChild threw exception: " + e.getMessage());
        }
        try {
            IFrameworkResource res = mgr.loadChild(PROJECT_NAME2);
            assertNotNull(res);
            assertTrue(res instanceof FrameworkProject);
            assertEquals(res.getName(), PROJECT_NAME2);
        } catch (FrameworkResourceException e) {
            fail("loadChild threw exception: " + e.getMessage());
        }
        projectDir2.delete();
    }

    public void testCreateRemoveDepot() {
        final IFrameworkProjectMgr mgr = FrameworkProjectMgr.create("projectMgr",
                                                                    new File(getFrameworkProjectsBase()),
                                                                    getFrameworkInstance(),
                                                                    FrameworkFactory.createNodesFactory(
                                                                            getFrameworkInstance().getFilesystemFramework())
        );
        final IRundeckProject d1 = mgr.createFrameworkProject("Depot1");
        assertEquals("exected two listed FrameworkProject after the addDepot call", 2, mgr.listFrameworkProjects().size());
        assertTrue("expected existsFrameworkProject to be true after it was added", mgr.existsFrameworkProject("Depot1"));
        assertEquals("getFrameworkProject did not return the expected project instance", d1.getName(), mgr.getFrameworkProject("Depot1").getName() );
        try {
            mgr.getFrameworkProject("NotThere");
            fail("Should have failed getting a non existent project");
        } catch (FrameworkResourceException e) {
            assertNotNull(e);
        }
    }

    public void testAddRemoveFrameworkProject() {
        final FrameworkProjectMgr mgr = FrameworkProjectMgr.create("projectMgr",
                                                             new File(getFrameworkProjectsBase()),
                                                             getFrameworkInstance(),FrameworkFactory.createNodesFactory(getFrameworkInstance().getFilesystemFramework()) );
        final FrameworkProject d1 = mgr.createFSFrameworkProject("Depot1");
        assertTrue("existsFrameworkProject did not return true for a type that should exist",
                   mgr.existsFrameworkProject("Depot1"));
        File projectBaseDir = d1.getBaseDir();
        assertTrue(projectBaseDir.exists());
        assertTrue(projectBaseDir.isDirectory());
        assertTrue(new File(projectBaseDir,"etc/project.properties").exists());
        assertTrue(new File(projectBaseDir,"etc/project.properties").isFile());
        mgr.removeFrameworkProject(d1.getName());
        assertFalse(projectBaseDir.exists());
        assertFalse(mgr.existsFrameworkProject(d1.getName()));
    }
}
