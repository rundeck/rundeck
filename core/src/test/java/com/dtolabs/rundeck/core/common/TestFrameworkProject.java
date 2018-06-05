/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.resources.*;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.*;
import java.util.*;


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
        getFrameworkInstance().getFilesystemFrameworkProjectManager().removeFrameworkProject(PROJECT_NAME);
    }

    public void testCreateDepotStructure() throws IOException {
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        if (projectDir.exists()) {
            FileUtils.deleteDir(projectDir);
        }
        FrameworkProject.createFileStructure(projectDir);
        assertTrue(new File(projectDir, FrameworkProject.ETC_DIR_NAME).exists());
    }
    public void writeProps(final Properties props, final File outputFile) throws IOException{

        FileOutputStream fos = new FileOutputStream(outputFile);
        try{
            props.store(fos,null);
        }finally{
            fos.close();
        }
    }
    public void loadProps(final Properties props, final File file) throws IOException{

        FileInputStream fis = new FileInputStream(file);
        try{
            props.load(fis);
        }finally{
            fis.close();
        }
    }
    public FrameworkProject createProject() {
        Framework frameworkInstance = getFrameworkInstance();
        FilesystemFramework filesystemFramework = FrameworkFactory.createFilesystemFramework(
                frameworkInstance.getBaseDir()
        );
        return FrameworkProject.create(PROJECT_NAME,
                                       new File(getFrameworkProjectsBase()),
                                       filesystemFramework,
                                       frameworkInstance.getFilesystemFrameworkProjectManager(),
                                       frameworkInstance::getResourceFormatGeneratorService,
                                       frameworkInstance::getResourceModelSourceService
        );
    }
    public void testConstruction() {
        if (projectBasedir.exists()) {
            projectBasedir.delete();
        }
        final FrameworkProject project = createProject();
        assertTrue("incorrect project.dir", project.getBaseDir().equals(new File(getFrameworkProjectsBase(), PROJECT_NAME)));
    }


    public void testChildCouldBeLoaded() {
        final FrameworkProject project = createProject();

        assertFalse(project.childCouldBeLoaded("HahaType"));

        final File deployments = new File(project.getBaseDir(), "resources");
        final File hahadir = new File(deployments, "HahaType");
        hahadir.mkdirs();

        assertTrue(project.childCouldBeLoaded("HahaType"));
        hahadir.delete();
        assertFalse(project.childCouldBeLoaded("HahaType"));

    }

    public void testListChildNames() {
        final FrameworkProject project = createProject();

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
        p.put("a.b", "monkey");
        p.put("b.c", "helmann");
        p.store(new FileOutputStream(projectPropertyFile), "test properties");

        FrameworkProject project = createProject();

        assertEquals("monkey", project.getProperty("a.b"));
        assertEquals("helmann", project.getProperty("b.c"));
        Map<String, String> projectProperties = project.getProperties();
        assertEquals(3+15, projectProperties.size());
    }
    public void testProjectProperties() throws IOException {
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        FrameworkProject.createFileStructure(projectDir);
        final File etcDir = new File(projectDir, "etc");
        final File projectPropertyFile = new File(etcDir, "project.properties");
        final Properties p = new Properties();
        p.put("a.b", "monkey");
        p.put("b.c", "helmann");
        p.store(new FileOutputStream(projectPropertyFile), "test properties");

        FrameworkProject project = createProject();
        Map<String, String> projectProperties = project.getProjectProperties();
        assertEquals(3, projectProperties.size());
        assertEquals(PROJECT_NAME, projectProperties.get("project.name"));
        assertEquals("monkey", projectProperties.get("a.b"));
        assertEquals("helmann", projectProperties.get("b.c"));
    }

    /**
     * Test exists file resource
     * @throws Exception
     */
    public void testExistsFileResource() throws Exception {
        FrameworkProject project = createProject();
        //attempt to update nodes resource file without url prop
        assertFalse(project.existsFileResource("test.file"));
        File testFile = new File(projectBasedir, "test.file");
        testFile.deleteOnExit();
        FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"), testFile);
        assertTrue(project.existsFileResource("test.file"));
        testFile.delete();
    }
    /**
     * Test exists dir resource
     * @throws Exception
     */
    public void testExistsDirResource() throws Exception {
        FrameworkProject project = createProject();
        //attempt to update nodes resource file without url prop
        assertFalse(project.existsDirResource("monkey"));
        File testdir = new File(projectBasedir, "monkey");
        assertTrue(testdir.mkdirs());
        assertTrue(project.existsDirResource("monkey"));
        testdir.delete();
    }
    /**
     * Test list dir paths
     * @throws Exception
     */
    public void testListDirPaths() throws Exception {
        FrameworkProject project = createProject();
        //attempt to update nodes resource file without url prop
        assertFalse(project.existsDirResource("monkey"));
        File testDir = new File(projectBasedir, "monkey");
        assertTrue(testDir.mkdirs());
        File testFile1 = new File(testDir, "test1.file");
        File testFile2 = new File(testDir, "test2.file");
        File testDir2 = new File(testDir, "testdir");
        assertTrue(testDir2.mkdirs());

        FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"), testFile1);
        FileUtils.copyFileStreams(
                new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                testFile2
        );

        List<String> listing = project.listDirPaths("monkey");
        assertEquals(3, listing.size());
        assertTrue("not expected: " + listing, listing.contains("monkey/test1.file"));
        assertTrue("not expected: " + listing, listing.contains("monkey/test2.file"));
        assertTrue("not expected: "+listing,listing.contains("monkey/testdir/"));

        testFile1.delete();
        testFile2.delete();
        testDir2.delete();
        testDir.delete();
    }
    /**
     * Test load file resource
     * @throws Exception
     */
    public void testLoadFileResource() throws Exception {
        FrameworkProject project = createProject();
        //attempt to update nodes resource file without url prop
        File testFile = new File(projectBasedir, "test.file");

        FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"), testFile);
        testFile.deleteOnExit();
        assertTrue(project.existsFileResource("test.file"));

        File testFile2 = new File(projectBasedir, "test.file.copy");

        testFile2.deleteOnExit();
        assertFalse(testFile2.exists());

        long copied=project.loadFileResource("test.file", new FileOutputStream(testFile2));

        assertTrue(testFile2.exists());
        assertEquals(testFile.length(), copied);
        assertEquals(testFile.length(), testFile2.length());
        testFile.delete();
        testFile2.delete();
    }
    /**
     * Test store file resource
     * @throws Exception
     */
    public void testStoreFileResource() throws Exception {
        FrameworkProject project = createProject();
        //attempt to update nodes resource file without url prop
        File testFile = new File(projectBasedir, "test.file");

        testFile.deleteOnExit();
        assertFalse(project.existsFileResource("test.file"));

        File sourceFile = new File(
                "src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"
        );
        long copied = project.storeFileResource("test.file", new FileInputStream(sourceFile));

        assertTrue(testFile.exists());
        assertTrue(project.existsFileResource("test.file"));
        assertEquals(testFile.length(), copied);
        assertEquals(testFile.length(), sourceFile.length());
        testFile.delete();
    }

    /**
     * Test store file resource
     *
     * @throws Exception
     */
    public void testStoreFileResource_invalid() throws Exception {
        FrameworkProject project = createProject();

        File sourceFile = new File(
            "src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"
        );
        long copied = 0;
        try {
            copied = project.storeFileResource("blah/../../test.file", new FileInputStream(sourceFile));
            fail("Expected exception");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Path is outside of destination directory"));
        }


    }
    /**
     * Test delete file resource
     * @throws Exception
     */
    public void testDeleteFileResource() throws Exception {
        FrameworkProject project = createProject();
        File testFile = new File(projectBasedir, "test.file");

        FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"), testFile);

        testFile.deleteOnExit();
        assertTrue(project.existsFileResource("test.file"));

        boolean deleted = project.deleteFileResource("test.file");

        assertTrue(deleted);
        assertFalse(testFile.exists());
        assertFalse(project.existsFileResource("test.file"));

    }
    /**
     * Test delete file resource already gone
     * @throws Exception
     */
    public void testDeleteFileResource_notExists() throws Exception {
        FrameworkProject project = createProject();
        File testFile = new File(projectBasedir, "test.file");

        assertFalse(project.existsFileResource("test.file"));

        boolean deleted = project.deleteFileResource("test.file");

        assertTrue(deleted);
        assertFalse(testFile.exists());
        assertFalse(project.existsFileResource("test.file"));

    }



    
    public void testGetNodes() throws Exception {
        final FrameworkProject project = createProject();
        FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"), nodesfile);
        assertTrue(nodesfile.exists());
        INodeSet nodes = project.getNodeSet();
        assertNotNull(nodes);
        assertEquals("nodes was incorrect size", 2, nodes.getNodes().size());
        assertNotNull("nodes did not have correct test node1", nodes.getNode("test1"));
        assertNotNull("nodes did not have correct test node2", nodes.getNode("testnode2"));
    }







    public void testGenerateProjectPropertiesFile() throws IOException {
        final FrameworkProject project = createProject();

        final File propFile = new File(project.getEtcDir(), "project.properties");
        assertTrue("project.properties file was not generated",
                   propFile.exists());

         Properties p = new Properties();
        loadProps(p,propFile);
        assertEquals("file", p.get("resources.source.1.type"));
        assertEquals(null, p.get("a.b"));

        assertTrue(project.hasProperty("resources.source.1.type"));
        assertFalse(project.hasProperty("a.b"));
        assertEquals("file", project.getProperty("resources.source.1.type"));

        boolean overwrite = true;
        Properties newprops = new Properties();
        newprops.put("a.b", "value");
        project.generateProjectPropertiesFile(overwrite, newprops, true);

        p = new Properties();
        loadProps(p,propFile);
        assertEquals("file", p.get("resources.source.1.type"));
        assertEquals("value", p.get("a.b"));

        assertTrue(project.hasProperty("resources.source.1.type"));
        assertTrue(project.hasProperty("a.b"));
        assertEquals("value",project.getProperty("a.b"));
        assertEquals("file", project.getProperty("resources.source.1.type"));


    }
    public void testGenerateProjectPropertiesFileOverwrite() throws IOException {
        final FrameworkProject project = createProject();
        final Properties testprops = new Properties();
        testprops.setProperty("test1", "value1");
        testprops.setProperty("test2", "value2");
        testprops.setProperty("test3.something", "value3");
        testprops.setProperty("test3.somethingelse", "value3.else");
        boolean overwrite = true;
        project.generateProjectPropertiesFile(overwrite,testprops, false);

        final File propFile = new File(project.getEtcDir(), "project.properties");
        assertTrue("project.properties file was not generated",
                propFile.exists());

        final Properties p = new Properties();
        loadProps(p,propFile);
        assertFalse(p.containsKey("project.resources.file"));
        assertTrue(p.containsKey("test1"));
        assertEquals("value1", p.getProperty("test1"));
        assertTrue(p.containsKey("test2"));
        assertEquals("value2",p.getProperty("test2"));
        assertTrue(p.containsKey("test3.something"));
        assertEquals("value3", p.getProperty("test3.something"));
        assertTrue(p.containsKey("test3.somethingelse"));
        assertEquals("value3.else", p.getProperty("test3.somethingelse"));
    }
    public void testMergeProjectPropertiesFile() throws IOException {
        final FrameworkProject project = createProject();
        final Properties testprops = new Properties();
        testprops.setProperty("test1", "value1");
        testprops.setProperty("test2", "value2");
        testprops.setProperty("test3.something", "value3");
        testprops.setProperty("test3.somethingelse", "value3.else");
        boolean overwrite = true;
        project.generateProjectPropertiesFile(overwrite, testprops, false);

        final File propFile = new File(project.getEtcDir(), "project.properties");
        assertTrue("project.properties file was not generated",
            propFile.exists());


        //merge new values for some, and remove prefixes for others
        final Properties testprops2 = new Properties();
        testprops2.setProperty("test1", "xvalue1");
        testprops2.setProperty("test2", "xvalue2");
        project.mergeProjectProperties(testprops2, new HashSet<String>(Arrays.asList("test3.")));
        

        final Properties p = new Properties();
        loadProps(p,propFile);
        assertFalse(p.containsKey("project.resources.file"));
        assertTrue(p.containsKey("test1"));
        assertEquals("xvalue1", p.getProperty("test1"));
        assertTrue(p.containsKey("test2"));
        assertEquals("xvalue2", p.getProperty("test2"));
        assertFalse(p.containsKey("test3.something"));
        assertFalse(p.containsKey("test3.somethingelse"));

        assertFalse(project.hasProperty("project.resources.file"));
        assertTrue(project.hasProperty("test1"));
        assertEquals("xvalue1", project.getProperty("test1"));
        assertTrue(project.hasProperty("test2"));
        assertEquals("xvalue2", project.getProperty("test2"));
        assertFalse(project.hasProperty("test3.something"));
        assertFalse(project.hasProperty("test3.somethingelse"));
    }
    public void testMergeProjectPropertiesFileNullPrefixes() throws IOException {
        final FrameworkProject project = createProject();
        final Properties testprops = new Properties();
        testprops.setProperty("test1", "value1");
        testprops.setProperty("test2", "value2");
        testprops.setProperty("test3.something", "value3");
        testprops.setProperty("test3.somethingelse", "value3.else");
        boolean overwrite = true;
        project.generateProjectPropertiesFile(overwrite, testprops, false);

        final File propFile = new File(project.getEtcDir(), "project.properties");
        assertTrue("project.properties file was not generated",
            propFile.exists());


        //merge new values for some, and remove prefixes for others
        final Properties testprops2 = new Properties();
        testprops2.setProperty("test1", "xvalue1");
        testprops2.setProperty("test2", "xvalue2");
        project.mergeProjectProperties(testprops2, null);


        final Properties p = new Properties();
        loadProps(p,propFile);
        assertFalse(p.containsKey("project.resources.file"));
        assertTrue(p.containsKey("test1"));
        assertEquals("xvalue1", p.getProperty("test1"));
        assertTrue(p.containsKey("test2"));
        assertEquals("xvalue2", p.getProperty("test2"));
        assertTrue(p.containsKey("test3.something"));
        assertTrue(p.containsKey("test3.somethingelse"));
        assertEquals("value3",p.get("test3.something"));
        assertEquals("value3.else", p.get("test3.somethingelse"));

        assertFalse(project.hasProperty("project.resources.file"));
        assertTrue(project.hasProperty("test1"));
        assertEquals("xvalue1", project.getProperty("test1"));
        assertTrue(project.hasProperty("test2"));
        assertEquals("xvalue2", project.getProperty("test2"));
        assertTrue(project.hasProperty("test3.something"));
        assertTrue(project.hasProperty("test3.somethingelse"));
        assertEquals("value3",project.getProperty("test3.something"));
        assertEquals("value3.else",project.getProperty("test3.somethingelse"));
    }


    static class testSource implements ResourceModelSource {
        INodeSet returnNodes;
        int called=0;
        public INodeSet getNodes() throws ResourceModelSourceException {
            called++;
            return returnNodes;
        }
    }
    static class testFactory implements ResourceModelSourceFactory {
        ResourceModelSource returnProvider;
        Properties createConfiguration;
        int called=0;
        public ResourceModelSource createResourceModelSource(Properties configuration) throws ConfigurationException {
            called++;
            createConfiguration=configuration;
            return returnProvider;
        }
    }
    public void testLoadNodesProvidersBasic() throws Exception {

        final ResourceModelSourceService service = ResourceModelSourceService.getInstanceForFramework(
            getFrameworkInstance());
        testSource provider1 = new testSource();
        final NodeSetImpl set1 = new NodeSetImpl();
        set1.putNode(new NodeEntryImpl("set1node1"));

        provider1.returnNodes = set1;
        testFactory factory1 = new testFactory();
        factory1.returnProvider=provider1;


        service.registerInstance("file", factory1);
        service.registerInstance("url", factory1);
        service.registerInstance("directory", factory1);


        FrameworkProject project = createProject();
        final INodeSet nodeSet = project.getNodeSet();
        assertNotNull(nodeSet);
        assertEquals(1, factory1.called);
        assertEquals(1,provider1.called);
        
        assertEquals(1, nodeSet.getNodes().size());
        assertNotNull(nodeSet.getNode("set1node1"));
    }

    public void testLoadNodesProvidersMultiples() throws Exception {

        final ResourceModelSourceService service = ResourceModelSourceService.getInstanceForFramework(
            getFrameworkInstance());
        testSource provider1 = new testSource();
        final NodeSetImpl set1 = new NodeSetImpl();
        set1.putNode(new NodeEntryImpl("set1node1"));

        provider1.returnNodes = set1;
        testFactory factory1 = new testFactory();
        factory1.returnProvider=provider1;

        testSource provider2 = new testSource();
        final NodeSetImpl set2 = new NodeSetImpl();
        set2.putNode(new NodeEntryImpl("set2node1"));
        provider2.returnNodes = set2;
        testFactory factory2 = new testFactory();
        factory2.returnProvider = provider2;


        testSource provider3 = new testSource();
        final NodeSetImpl set3 = new NodeSetImpl();
        set3.putNode(new NodeEntryImpl("set3node1"));
        provider3.returnNodes = set3;
        testFactory factory3 = new testFactory();
        factory3.returnProvider = provider3;


        service.registerInstance("file", factory1);
        service.registerInstance("url", factory2);
        service.registerInstance("directory", factory3);

        //backup a copy project.properties

        //add framework.resources.url property
        Properties props1 = new Properties();
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".1.type", "file");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".1.config.file", "/test/file/path");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".2.type", "url");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".2.config.url", "http://example.com/test2");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".3.type", "directory");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".3.config.directory", "/test/file/path3");
        projectPropsFile.getParentFile().mkdirs();
        writeProps(props1, projectPropsFile);

        FrameworkProject project = createProject();
        final INodeSet nodeSet = project.getNodeSet();
        assertNotNull(nodeSet);
        assertEquals(2, factory1.called);
        assertEquals(1,provider1.called);
        assertEquals(1, factory2.called);
        assertEquals(1,provider2.called);
        assertEquals(1, factory3.called);
        assertEquals(1,provider3.called);

        assertEquals(3, nodeSet.getNodes().size());
        assertNotNull(nodeSet.getNode("set1node1"));
        assertNotNull(nodeSet.getNode("set2node1"));
        assertNotNull(nodeSet.getNode("set3node1"));
        projectPropsFile.delete();
    }

    /**
     * Multiple node definitions should have merged attributes in final result by default
     * @throws Exception
     */
    public void testMergedAttributesDefault() throws Exception {

        final ResourceModelSourceService service = ResourceModelSourceService.getInstanceForFramework(
            getFrameworkInstance());
        testSource provider1 = new testSource();
        final NodeSetImpl set1 = new NodeSetImpl();
        set1.putNode(new NodeEntryImpl("set1node1"));

        provider1.returnNodes = set1;
        testFactory factory1 = new testFactory();
        factory1.returnProvider=provider1;

        testSource provider2 = new testSource();
        final NodeSetImpl set2 = new NodeSetImpl();
        NodeEntryImpl set2node1_orig = new NodeEntryImpl("set2node1");
        set2node1_orig.setAttribute("abc", "123");
        set2node1_orig.setAttribute("def", "456");
        set2.putNode(set2node1_orig);
        provider2.returnNodes = set2;
        testFactory factory2 = new testFactory();
        factory2.returnProvider = provider2;


        testSource provider3 = new testSource();
        final NodeSetImpl set3 = new NodeSetImpl();
        NodeEntryImpl set2node1 = new NodeEntryImpl("set2node1");
        set2node1.setAttribute("abc", "321");
        set2node1.setAttribute("ghi", "789");
        set3.putNode(set2node1);
        provider3.returnNodes = set3;
        testFactory factory3 = new testFactory();
        factory3.returnProvider = provider3;


        service.registerInstance("file", factory1);
        service.registerInstance("url", factory2);
        service.registerInstance("directory", factory3);

        //backup a copy project.properties

        //add framework.resources.url property
        Properties props1 = new Properties();
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".1.type", "file");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".1.config.file", "/test/file/path");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".2.type", "url");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".2.config.url", "http://example.com/test2");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".3.type", "directory");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".3.config.directory", "/test/file/path3");
        projectPropsFile.getParentFile().mkdirs();
        writeProps(props1,projectPropsFile);

        FrameworkProject project = createProject();
        final INodeSet nodeSet = project.getNodeSet();
        assertNotNull(nodeSet);
        assertEquals(2, factory1.called);
        assertEquals(1,provider1.called);
        assertEquals(1, factory2.called);
        assertEquals(1,provider2.called);
        assertEquals(1, factory3.called);
        assertEquals(1,provider3.called);

        assertEquals(2, nodeSet.getNodes().size());
        assertNotNull(nodeSet.getNode("set1node1"));
        assertNotNull(nodeSet.getNode("set2node1"));
        assertEquals("321", nodeSet.getNode("set2node1").getAttributes().get("abc"));
        assertEquals("456", nodeSet.getNode("set2node1").getAttributes().get("def"));
        assertEquals("789", nodeSet.getNode("set2node1").getAttributes().get("ghi"));
        assertNull(nodeSet.getNode("set3node1"));
        projectPropsFile.delete();
    }

    /**
     * Multiple node definitions should have not merge attributes if disabled
     */
    public void testMergedAttributesDisabled() throws Exception {
        Properties props1 = new Properties();
        //disable merged attributes
        props1.setProperty(FrameworkProject.PROJECT_RESOURCES_MERGE_NODE_ATTRIBUTES, "false");

        final ResourceModelSourceService service = ResourceModelSourceService.getInstanceForFramework(
            getFrameworkInstance());
        testSource provider1 = new testSource();
        final NodeSetImpl set1 = new NodeSetImpl();
        set1.putNode(new NodeEntryImpl("set1node1"));

        provider1.returnNodes = set1;
        testFactory factory1 = new testFactory();
        factory1.returnProvider=provider1;

        testSource provider2 = new testSource();
        final NodeSetImpl set2 = new NodeSetImpl();
        NodeEntryImpl set2node1_orig = new NodeEntryImpl("set2node1");
        set2node1_orig.setAttribute("abc", "123");
        set2node1_orig.setAttribute("def", "456");
        set2.putNode(set2node1_orig);
        provider2.returnNodes = set2;
        testFactory factory2 = new testFactory();
        factory2.returnProvider = provider2;


        testSource provider3 = new testSource();
        final NodeSetImpl set3 = new NodeSetImpl();
        NodeEntryImpl set2node1 = new NodeEntryImpl("set2node1");
        set2node1.setAttribute("abc", "321");
        set2node1.setAttribute("ghi", "789");
        set3.putNode(set2node1);
        provider3.returnNodes = set3;
        testFactory factory3 = new testFactory();
        factory3.returnProvider = provider3;


        service.registerInstance("file", factory1);
        service.registerInstance("url", factory2);
        service.registerInstance("directory", factory3);


        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".1.type", "file");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".1.config.file", "/test/file/path");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".2.type", "url");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".2.config.url", "http://example.com/test2");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".3.type", "directory");
        props1.setProperty(FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + ".3.config.directory", "/test/file/path3");
        projectPropsFile.getParentFile().mkdirs();
        writeProps(props1,projectPropsFile);

        FrameworkProject project = createProject();
        final INodeSet nodeSet = project.getNodeSet();
        assertNotNull(nodeSet);
        assertEquals(2, factory1.called);
        assertEquals(1,provider1.called);
        assertEquals(1, factory2.called);
        assertEquals(1,provider2.called);
        assertEquals(1, factory3.called);
        assertEquals(1,provider3.called);

        assertEquals(2, nodeSet.getNodes().size());
        assertNotNull(nodeSet.getNode("set1node1"));
        assertNotNull(nodeSet.getNode("set2node1"));
        assertEquals("321", nodeSet.getNode("set2node1").getAttributes().get("abc"));
        assertEquals(null, nodeSet.getNode("set2node1").getAttributes().get("def"));
        assertEquals("789", nodeSet.getNode("set2node1").getAttributes().get("ghi"));
        assertNull(nodeSet.getNode("set3node1"));
        projectPropsFile.delete();
    }
}
