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

import java.io.*;
import java.util.Properties;


/**
 * TestFrameworkProject
 */
public class TestFrameworkProject extends AbstractBaseTest {
    private final String PROJECT_NAME = "TestFrameworkProject";

    File projectBasedir;
    File nodesfile;
    File projectPropsFile;


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
        projectPropsFile = new File(projectBasedir, "/etc/project.properties");
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
     /*   final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        FrameworkProject.createFileStructure(projectDir);
        final File etcDir = new File(projectDir, "etc");
        final File projectPropertyFile = new File(etcDir, "project.properties");
        final Properties p = new Properties();
        p.put("project.dir", "${framework.projects.dir}/${project.name}");
        p.put("project.resources.dir", "${project.dir}/resources");
        p.put("project.etc.dir", "${project.dir}/etc");
        p.put("project.resources.file", "${project.etc.dir}/resources.xml");
        p.store(new FileOutputStream(projectPropertyFile), "test properties");

        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                         new File(getFrameworkProjectsBase()),
                                         getFrameworkInstance().getFrameworkProjectMgr());

        assertEquals(project.getProperty("project.dir"), projectDir.getAbsolutePath());*/
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
        assertTrue("nodes did not have correct test node1", nodes.hasNode("test1"));
        assertTrue("nodes did not have correct test node2", nodes.hasNode("testnode2"));
    }

    public void testUpdateNodesResourceFile() throws Exception {
        FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                         new File(getFrameworkProjectsBase()),
                                         getFrameworkInstance().getFrameworkProjectMgr());
        //attempt to update nodes resource file without url prop
        assertFalse(project.hasProperty(FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY));
        assertFalse(project.updateNodesResourceFile());

        //set the nodes resources url property
        Properties orig = new Properties();
        orig.load(new InputStreamReader(new FileInputStream(projectPropsFile)));

        Properties newProps = new Properties();
        newProps.load(new InputStreamReader(new FileInputStream(projectPropsFile)));
        final File filesrc = new File("src/test/com/dtolabs/rundeck/core/common/test-nodes2.xml");
        final File tempfile = File.createTempFile("test", ".xml");
        FileUtils.copyFileStreams(filesrc, tempfile);
        final String providerURL = tempfile.toURI().toURL().toExternalForm();
        
        newProps.setProperty("project.resources.url", providerURL);

        newProps.store(new FileOutputStream(projectPropsFile),null);

        project = FrameworkProject.create(PROJECT_NAME,
            new File(getFrameworkProjectsBase()),
            getFrameworkInstance().getFrameworkProjectMgr());
        assertTrue(project.hasProperty("project.resources.url"));
        assertEquals(providerURL, project.getProperty("project.resources.url"));

        tempfile.setLastModified(System.currentTimeMillis());
        File resourcesFile = new File(project.getNodesResourceFilePath());
        assertTrue(project.updateNodesResourceFile());
        final File toFile = new File(projectBasedir, "/etc/testout");
        assertTrue("does not exist file: "+resourcesFile.getAbsolutePath(), resourcesFile.exists());
        FileUtils.copyFileStreams(resourcesFile, toFile);
        System.err.println("copied to file " + resourcesFile + " to " + toFile);

        Nodes nodes = project.getNodes();
        assertNotNull(nodes);
        assertEquals("nodes was incorrect size", 3, nodes.listNodes().size());
        assertTrue("nodes did not have correct test node1", nodes.hasNode("test1"));
        assertTrue("nodes did not have correct test node2", nodes.hasNode("testnode2"));
        assertTrue("nodes did not have correct test node2", nodes.hasNode("testnode3"));

        //restore props and resources
        orig.store(new FileOutputStream(projectPropsFile), null);
        FileUtils.copyFileStreams(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"), resourcesFile);
    }

    public void testUpdateNodesResourceFileFromUrl() throws Exception {
        FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                                         new File(getFrameworkProjectsBase()),
                                         getFrameworkInstance().getFrameworkProjectMgr());

        //attempt to update nodes resource file without url prop
        assertFalse(project.hasProperty(FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY));

        //use invalid protocol
        try{
            project.updateNodesResourceFileFromUrl("ftp://test.com/test", null, null);
            fail("Should fail");
        } catch (UpdateUtils.UpdateException e) {
            assertEquals("URL protocol not allowed: ftp", e.getMessage());
        }

        final String nodesUrl = new File("src/test/com/dtolabs/rundeck/core/common/test-nodes2.xml")
            .toURI().toURL().toExternalForm();


        //set the nodes resources url property
        Properties orig = new Properties();
        orig.load(new InputStreamReader(new FileInputStream(projectPropsFile)));

        Properties newProps = new Properties();
        newProps.load(new InputStreamReader(new FileInputStream(projectPropsFile)));
        newProps.setProperty("project.resources.url", nodesUrl);
        newProps.store(new FileOutputStream(projectPropsFile), null);
        
        project = FrameworkProject.create(PROJECT_NAME,
            new File(getFrameworkProjectsBase()),
            getFrameworkInstance().getFrameworkProjectMgr());
        project.updateNodesResourceFileFromUrl(nodesUrl,null,null);

        Nodes nodes = project.getNodes();
        assertNotNull(nodes);
        assertEquals("nodes was incorrect size", 3, nodes.listNodes().size());
        assertTrue("nodes did not have correct test node1", nodes.hasNode("test1"));
        assertTrue("nodes did not have correct test node2", nodes.hasNode("testnode2"));
        assertTrue("nodes did not have correct test node2", nodes.hasNode("testnode3"));

        //restore props and resources

        orig.store(new FileOutputStream(projectPropsFile), null);
        FileUtils.copyFileStreams(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"), nodesfile);
    }
    public void testValidateResourceProviderURL() throws Exception{
        FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
            new File(getFrameworkProjectsBase()),
            getFrameworkInstance().getFrameworkProjectMgr());

        //use invalid protocol
        try {
            project.validateResourceProviderURL("ftp://test.com/test");
            fail("Should fail");
        } catch (UpdateUtils.UpdateException e) {
            assertEquals("URL protocol not allowed: ftp", e.getMessage());
        }
        //use valid protocol
        try {
            project.validateResourceProviderURL("http://test.com/test");
        } catch (UpdateUtils.UpdateException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        //use valid protocol
        try {
            project.validateResourceProviderURL("https://test.com/test");
        } catch (UpdateUtils.UpdateException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        //use valid protocol
        try {
            project.validateResourceProviderURL("file:///tmp/test");
        } catch (UpdateUtils.UpdateException e) {
            fail("Unexpected exception: " + e.getMessage());
        }

    }
    public void testIsAllowedProviderURL() throws Exception{
        FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
            new File(getFrameworkProjectsBase()),
            getFrameworkInstance().getFrameworkProjectMgr());

        //set project providerURL and allowed URL regexes
        Properties orig = new Properties();
        orig.load(new InputStreamReader(new FileInputStream(projectPropsFile)));

        Properties newProps = new Properties();
        newProps.load(new InputStreamReader(new FileInputStream(projectPropsFile)));
        final String providerURL = new File(
            "src/test/com/dtolabs/rundeck/core/common/test-nodes2.xml")
            .toURI().toURL().toExternalForm();
        newProps.setProperty("project.resources.url", providerURL);
        newProps.setProperty(FrameworkProject.PROJECT_RESOURCES_ALLOWED_URL_PREFIX + "0", "^http://example.com/test1$");
        newProps.setProperty(FrameworkProject.PROJECT_RESOURCES_ALLOWED_URL_PREFIX + "1",
            "^http://example.com/test2/.*$");
        newProps.setProperty(FrameworkProject.PROJECT_RESOURCES_ALLOWED_URL_PREFIX + "2",
            "^https://example.com/.*?/monkey$");

        newProps.store(new FileOutputStream(projectPropsFile), null);

        project = FrameworkProject.create(PROJECT_NAME,
            new File(getFrameworkProjectsBase()),
            getFrameworkInstance().getFrameworkProjectMgr());

        //provider URL for the project should work
        assertTrue(project.isAllowedProviderURL(providerURL));

        //valid URL match 0
        assertTrue(project.isAllowedProviderURL("http://example.com/test1"));
        assertFalse(project.isAllowedProviderURL("HTTP://example.com/test2"));

        //valid URL match 1
        assertTrue(project.isAllowedProviderURL("HTTP://example.com/test2/"));
        assertTrue(project.isAllowedProviderURL("HTTP://example.com/test2/elephant"));
        assertTrue(project.isAllowedProviderURL("HTTP://example.com/test2/bologna/something"));
        assertTrue(project.isAllowedProviderURL("HTTP://example.com/test2/elf"));
        assertTrue(project.isAllowedProviderURL("HTTP://example.com/test2/bologna"));

        //valid URL match 2
        assertTrue(project.isAllowedProviderURL("HTTPs://example.com/blah/monkey"));
        assertTrue(project.isAllowedProviderURL("HTTPs://example.com/blah/blee/monkey"));
        assertTrue(project.isAllowedProviderURL("HTTPs://example.com/xylophone/monkey"));
        assertTrue(project.isAllowedProviderURL("HTTPs://example.com/a/lonely/xylophone/monkey"));

        //invalid file URL
        assertFalse(project.isAllowedProviderURL("file:///tmp/test"));

        //invalid https URL
        assertFalse(project.isAllowedProviderURL("https://example.com/test1"));
        //invalid http URL
        assertFalse(project.isAllowedProviderURL("http://example.com/blah/monkey"));

        //set some framework properties to intersect the regexes


        final File frameworkProps = new File(getFrameworkInstance().getBaseDir(),
            "/etc/framework.properties");
        Properties origFProps = new Properties();
        origFProps.load(new InputStreamReader(new FileInputStream(frameworkProps)));

        Properties newFProps = new Properties();
        newFProps.load(new InputStreamReader(new FileInputStream(frameworkProps)));
        newFProps.setProperty(FrameworkProject.FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX+"0", "^https?://example.com/test[\\d]$");
        newFProps.setProperty(FrameworkProject.FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX + "1",
            "^http://example.com/test2/(elf|bologna)$");
        newFProps.setProperty(FrameworkProject.FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX+"2",
            "^https://example.com/.*?xylophone/monkey$");
        newFProps.setProperty(FrameworkProject.FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX+"3",
            "^file:///tmp/test.*$");

        newFProps.store(new FileOutputStream(frameworkProps), null);

        //load framework instance
        Framework framework = Framework.getInstance(getFrameworkInstance().getBaseDir().getAbsolutePath(),
            getFrameworkProjectsBase());
        project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()),
            framework.getFrameworkProjectMgr());

        //provider URL for the project should work
        assertTrue(project.isAllowedProviderURL(providerURL));

        //valid URL match 0
        assertTrue(project.isAllowedProviderURL("http://example.com/test1"));
        assertFalse(project.isAllowedProviderURL("HTTP://example.com/test2"));

        //valid URL match 1
        assertFalse(project.isAllowedProviderURL("HTTP://example.com/test2/"));
        assertFalse(project.isAllowedProviderURL("HTTP://example.com/test2/elephant"));
        assertFalse(project.isAllowedProviderURL("HTTP://example.com/test2/bologna/something"));
        assertTrue(project.isAllowedProviderURL("HTTP://example.com/test2/elf"));
        assertTrue(project.isAllowedProviderURL("HTTP://example.com/test2/bologna"));

        //valid URL match 2
        assertFalse(project.isAllowedProviderURL("HTTPs://example.com/blah/monkey"));
        assertFalse(project.isAllowedProviderURL("HTTPs://example.com/blah/blee/monkey"));
        assertTrue(project.isAllowedProviderURL("HTTPs://example.com/xylophone/monkey"));
        assertTrue(project.isAllowedProviderURL("HTTPs://example.com/a/lonely/xylophone/monkey"));

        //invalid file URL
        assertFalse(project.isAllowedProviderURL("file:///tmp/test"));

        //invalid https URL
        assertFalse(project.isAllowedProviderURL("https://example.com/test1"));
        //invalid http URL
        assertFalse(project.isAllowedProviderURL("http://example.com/blah/monkey"));


        //remove project specific props
        orig.store(new FileOutputStream(projectPropsFile), null);
        project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()),
            framework.getFrameworkProjectMgr());

        //provider URL for the project should now fail
        assertFalse(project.isAllowedProviderURL(providerURL));

        //valid URL match 0
        assertTrue(project.isAllowedProviderURL("http://example.com/test1"));
        assertTrue(project.isAllowedProviderURL("HTTP://example.com/test2"));

        //valid URL match 1
        assertFalse(project.isAllowedProviderURL("HTTP://example.com/test2/"));
        assertFalse(project.isAllowedProviderURL("HTTP://example.com/test2/elephant"));
        assertFalse(project.isAllowedProviderURL("HTTP://example.com/test2/bologna/something"));
        assertTrue(project.isAllowedProviderURL("HTTP://example.com/test2/elf"));
        assertTrue(project.isAllowedProviderURL("HTTP://example.com/test2/bologna"));

        //valid URL match 2
        assertFalse(project.isAllowedProviderURL("HTTPs://example.com/blah/monkey"));
        assertFalse(project.isAllowedProviderURL("HTTPs://example.com/blah/blee/monkey"));
        assertTrue(project.isAllowedProviderURL("HTTPs://example.com/xylophone/monkey"));
        assertTrue(project.isAllowedProviderURL("HTTPs://example.com/a/lonely/xylophone/monkey"));

        //invalid file URL
        assertTrue(project.isAllowedProviderURL("file:///tmp/test"));

        //invalid https URL
        assertTrue(project.isAllowedProviderURL("https://example.com/test1"));
        //invalid http URL
        assertFalse(project.isAllowedProviderURL("http://example.com/blah/monkey"));

        //restore fprops
        origFProps.store(new FileOutputStream(frameworkProps), null);
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
