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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import com.dtolabs.rundeck.core.utils.FileUtils;

import java.io.File;
import java.util.Collection;


/**
 * TestFrameworkResourceParent
 */
public class TestFrameworkResourceParent extends TestCase {
    private final String RESOURCE_PROJECT = "hahaDepot";
    private final String RESOURCE_TYPE = "HahaType";
    private final String RESOURCE_NAME = "aHaha";
    private final String RESOURCE_NAME2 = "aHaha2";
    private Framework framework = null;
    File projectsBasedir = new File("build/test-target/TestFrameworkResourceParent");
    File projectBasedir = new File(projectsBasedir, RESOURCE_PROJECT);
    final File baseDir = new File(projectsBasedir, RESOURCE_NAME);

    public TestFrameworkResourceParent(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestFrameworkResourceParent.class);
    }

    protected void setUp() {
        framework = null;
        projectsBasedir.mkdirs();
        projectBasedir = new File(projectsBasedir, RESOURCE_PROJECT);
        FileUtils.deleteDir(projectBasedir);
    }

    protected void tearDown() {
        FileUtils.deleteDir(projectsBasedir);
    }
    public static final class FrameworkResourceParentImpl extends FrameworkResourceParent{
        /**
         * Constructor
         *
         * @param name Name of resource
         * @param dir  Base directory of resource
         */
        public FrameworkResourceParentImpl(final String name, final File dir, final IFrameworkResourceParent parent) {
            super(name, dir, parent);
        }

        public IFrameworkResource loadChild(String name) {
            return null;
        }
    }

    public void testConstruction() {
        assertTrue(true);
        FrameworkResourceParent resource = new FrameworkResourceParentImpl(RESOURCE_NAME, baseDir, null);
        assertNotNull(resource);
        assertEquals(resource.getName(), RESOURCE_NAME);
        assertEquals(resource.getBaseDir(), baseDir);

        assertEquals("Children were incorrect size: " + resource.listChildren(), 0, resource.listChildren().size());

        assertEquals("Child names were incorrect size: " + resource.listChildNames(),
                     0,
                     resource.listChildNames().size());
    }

    public void testLoadChild(){

        final IFrameworkResourceParent parent2 = new FrameworkResourceParent(RESOURCE_NAME2, baseDir, null) {

            public IFrameworkResource loadChild(String name) {
                File newbase = new File(baseDir, name);
                if(newbase.exists()&&newbase.isDirectory()) {
                    return new FrameworkResourceParentImpl(name, newbase, this);
                }else{
                    return null;
                }
            }
        };

        assertFalse(parent2.childCouldBeLoaded("blahex"));
        try{
            assertNull(parent2.getChild("blahex"));
            fail("child should not be loadedable: blahex");
        } catch (FrameworkResourceParent.NoSuchResourceException e) {
            assertNotNull(e);
        }

        File newbase2 = new File(baseDir, "blahex");
        newbase2.mkdirs();
        assertTrue(newbase2.exists());
        assertTrue(newbase2.isDirectory());
        assertTrue(parent2.childCouldBeLoaded("blahex"));

        try {
            IFrameworkResource child = parent2.getChild("blahex");
            assertNotNull("Child was null", child);
            assertTrue("Child was not correct class (" + child.getClass().getName() + ")",
                       child instanceof FrameworkResourceParentImpl);
            assertEquals("Child had invalid base dir", child.getBaseDir(), newbase2);
        } catch (FrameworkResourceParent.NoSuchResourceException e) {
            fail("Exception should not be thrown: NoSuchResourceException: " + e.getMessage());
        }
        newbase2.delete();
    }


    public void testChildCouldBeLoaded() {
        final IFrameworkResourceParent parent = new FrameworkResourceParentImpl(RESOURCE_NAME, baseDir, null);

        assertFalse(parent.childCouldBeLoaded("blah"));
        try {
            assertNull(parent.getChild("blah"));
            fail("child should not be loadedable: blah");
        } catch (FrameworkResourceParent.NoSuchResourceException e) {
            assertNotNull(e);
        }

        File newbase = new File(baseDir, "blah");
        newbase.mkdirs();
        assertTrue(newbase.exists());
        assertTrue(newbase.isDirectory());
        assertTrue(parent.childCouldBeLoaded("blah"));
        try {
            parent.getChild("blah");
            fail("child should not be loadedable: blah");
        } catch (FrameworkResourceParent.NoSuchResourceException e) {
            assertNotNull(e);
        }
        newbase.delete();
    }

    public void testChildIsValid(){
        final IFrameworkResourceParent parent = new FrameworkResourceParentImpl(RESOURCE_NAME, baseDir, null);

        assertFalse(parent.childCouldBeLoaded("blah"));

        final IFrameworkResource child = parent.createChild("blah");


        File newbase = child.getBaseDir();
        assertTrue(newbase.exists());
        assertTrue(newbase.isDirectory());
        assertTrue(parent.childCouldBeLoaded("blah"));
        assertTrue(child.isValid());
        
        newbase.delete();

        assertFalse(newbase.exists());
        assertFalse(newbase.isDirectory());
        assertFalse(parent.childCouldBeLoaded("blah"));
        assertFalse(child.isValid());
    }

    public void testListChildNames(){
        final IFrameworkResourceParent parent = new FrameworkResourceParentImpl(RESOURCE_NAME, baseDir, null);
        //create sub dirs and test that child names list includes them
        File newbase = new File(baseDir, "blah");
        newbase.mkdirs();
        assertTrue(newbase.exists());
        assertTrue(newbase.isDirectory());
        assertTrue(parent.childCouldBeLoaded("blah"));
        Collection list = parent.listChildNames();
        assertEquals("Child names list was wrong size: " + list, 1,list.size());
        assertTrue("List doesn't contain correct value blah: " + list, list.contains("blah"));
        File newbase2 = new File(baseDir, "blahex");
        newbase2.mkdirs();
        assertTrue(newbase2.exists());
        assertTrue(newbase2.isDirectory());
        assertTrue(parent.childCouldBeLoaded("blahex"));
        list = parent.listChildNames();
        assertEquals("Child names list was wrong size: " + list, 2,list.size());
        assertTrue("List doesn't contain correct value blah: " + list, list.contains("blah"));
        assertTrue("List doesn't contain correct value blahex: " + list, list.contains("blahex"));

        //add a new child manually without creating subdir and verify that names list is correct size

        final FrameworkResourceParent child1 = new FrameworkResourceParentImpl("child1", baseDir, parent);
        parent.createChild("child1");
        list = parent.listChildNames();
        assertEquals("Child names list was wrong size: " + list, 3, list.size());
        assertTrue("List doesn't contain correct value blah: " + list, list.contains("blah"));
        assertTrue("List doesn't contain correct value blahex: " + list, list.contains("blahex"));
        assertTrue("List doesn't contain correct value blahex: " + list, list.contains("child1"));

        newbase.delete();
        newbase2.delete();

    }
    public void testListChildNamesIgnoresDotDir(){
        final IFrameworkResourceParent parent = new FrameworkResourceParentImpl(RESOURCE_NAME, baseDir, null);
        //create sub dirs and test that child names list includes them
        File newbase = new File(baseDir, ".blah");
        newbase.mkdirs();
        assertTrue(newbase.exists());
        assertTrue(newbase.isDirectory());
        assertTrue(parent.childCouldBeLoaded(".blah"));
        Collection list = parent.listChildNames();
        assertEquals("Child names list was wrong size: " + list, 0,list.size());
        assertFalse("List doesn't contain correct value blah: " + list, list.contains(".blah"));
        File newbase2 = new File(baseDir, "blahex");
        newbase2.mkdirs();
        assertTrue(newbase2.exists());
        assertTrue(newbase2.isDirectory());
        assertTrue(parent.childCouldBeLoaded("blahex"));
        list = parent.listChildNames();
        assertEquals("Child names list was wrong size: " + list, 1,list.size());
        assertFalse("List doesn't contain correct value blah: " + list, list.contains("blah"));
        assertTrue("List doesn't contain correct value blahex: " + list, list.contains("blahex"));

        //add a new child manually without creating subdir and verify that names list is correct size

        try {
            parent.createChild(".child1");
            fail("Should not allow createChild with invalid name");
        } catch (IllegalArgumentException e) {

        }
        list = parent.listChildNames();
        assertEquals(1, list.size());
        assertTrue(list.contains("blahex"));
        assertFalse(list.contains(".blah"));
        assertFalse(list.contains(".child1"));

        newbase.delete();
        newbase2.delete();

    }
    public void testCreateRemoveChild() {
        final IFrameworkResourceParent parent = new FrameworkResourceParent(RESOURCE_NAME, baseDir, null) {

            public IFrameworkResource loadChild(String name) {
                File newbase = new File(baseDir, name);
                if (newbase.exists() && newbase.isDirectory()) {
                    return new FrameworkResourceParentImpl(name, newbase, this);
                } else {
                    return null;
                }
            }
        };
        final FrameworkResourceParent child1 = new FrameworkResourceParentImpl("child1",
                                                                               new File(baseDir, "child1"),
                                                                               parent);
        final File child1Basedir = child1.getBaseDir();

        //child does not exist
        assertFalse("child basedir not found", child1Basedir.exists());
        assertFalse(parent.existsChild("child1"));
        assertEquals(0,parent.listChildren().size());
        assertEquals(0,parent.listChildNames().size());

        //create child
        final IFrameworkResource child2 = parent.createChild("child2");
        final File child2Basedir = child2.getBaseDir();
        assertTrue("child basedir not found", child2Basedir.exists());
        assertTrue(parent.existsChild("child2"));
        assertEquals(child2.getName(), parent.getChild("child2").getName());
        assertEquals(1,parent.listChildren().size());
        assertEquals(1,parent.listChildNames().size());

        //remove child
        parent.remove("child2");
        assertFalse("child basedir not found", child2Basedir.exists());
        assertFalse(parent.existsChild("child1"));
        assertFalse(child2.isValid());

    }
}
