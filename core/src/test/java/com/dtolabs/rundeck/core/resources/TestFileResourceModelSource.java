/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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

/*
* TestFileNodesProvider.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 9:08 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;

import java.io.*;
import java.util.Properties;

/**
 * TestFileNodesProvider is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestFileResourceModelSource extends AbstractBaseTest {
    public static final String PROJ_NAME = "TestFileNodesProvider";

    public TestFileResourceModelSource(String name) {
        super(name);
    }

    FrameworkProject frameworkProject;
    public void setUp() {

        final Framework frameworkInstance = getFrameworkInstance();

        frameworkProject = frameworkInstance.getFilesystemFrameworkProjectManager().createFSFrameworkProject(
                PROJ_NAME);
        generateProjectResourcesFile(
                new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                frameworkProject
        );

    }

    public void tearDown() throws Exception {
        super.tearDown();
        File projectdir = new File(getFrameworkProjectsBase(), PROJ_NAME);
        FileUtils.deleteDir(projectdir);
    }

    public void testConfigureProperties() throws Exception {
        final FileResourceModelSource fileNodesProvider = new FileResourceModelSource(getFrameworkInstance());
        try {
            fileNodesProvider.configure((Properties) null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        Properties props = new Properties();
        try {
            fileNodesProvider.configure(props);
            fail("shouldn't succeed");
        } catch (ConfigurationException e) {
            assertEquals("project is required", e.getMessage());
        }

        props.setProperty("project", PROJ_NAME);
        try {
            fileNodesProvider.configure(props);
            fail("shouldn't succeed");
        } catch (ConfigurationException e) {
            assertEquals("file is required", e.getMessage());
        }


    }
    public void testValidation() throws Exception {

        Properties props = new Properties();
        FileResourceModelSource.Configuration config = new FileResourceModelSource.Configuration(props);

        //missing project
        try{
            config.validate();
            fail("should not succeed");
        }catch (ConfigurationException e) {
            assertEquals("project is required", e.getMessage());
        }

        props.setProperty("project", PROJ_NAME);
        config = new FileResourceModelSource.Configuration(props);
        //missing file
        try {
            config.validate();
            fail("should not succeed");
        } catch (ConfigurationException e) {
            assertEquals("file is required", e.getMessage());
        }


        props.setProperty("file", "src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        props.setProperty("format", "xml");
        config = new FileResourceModelSource.Configuration(props);
        //should succeed
        try {
            config.validate();
        } catch (ConfigurationException e) {
            fail("unexpected failure");
        }

        
        props.setProperty("format", "resourcexml");
        config = new FileResourceModelSource.Configuration(props);
        //validation should succeed
        try {
            config.validate();

        } catch (ConfigurationException e) {
            fail("unexpected failure");
        }

        props.setProperty("format", "resourceyaml");
        config = new FileResourceModelSource.Configuration(props);
        //validation should succeed
        try {
            config.validate();

        } catch (ConfigurationException e) {
            fail("unexpected failure");
        }
    }
    public void testConfiguration() throws Exception {

        try {
            FileResourceModelSource.Configuration config = new FileResourceModelSource.Configuration((Properties) null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        Properties props = new Properties();
        FileResourceModelSource.Configuration config = new FileResourceModelSource.Configuration(props);
        assertNull(config.project);
        assertNull(config.format);
        assertNull(config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        props.setProperty("project", PROJ_NAME);
        config = new FileResourceModelSource.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNull(config.format);
        assertNull(config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        props.setProperty("format", "resourcexml");
        config = new FileResourceModelSource.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals("resourcexml", config.format);
        assertNull(config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        props.setProperty("format", "resourceyaml");
        config = new FileResourceModelSource.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals("resourceyaml", config.format);
        assertNull(config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        props.setProperty("file", "src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        config = new FileResourceModelSource.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals("resourceyaml", config.format);
        assertNotNull(config.nodesFile);
        assertEquals(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"), config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        props.setProperty("generateFileAutomatically", "true");
        config = new FileResourceModelSource.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals("resourceyaml", config.format);
        assertNotNull(config.nodesFile);
        assertEquals(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"), config.nodesFile);
        assertTrue(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);


        props.setProperty("generateFileAutomatically", "false");
        config = new FileResourceModelSource.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals("resourceyaml", config.format);
        assertNotNull(config.nodesFile);
        assertEquals(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"), config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);


        props.setProperty("includeServerNode", "true");
        config = new FileResourceModelSource.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals("resourceyaml", config.format);
        assertNotNull(config.nodesFile);
        assertEquals(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"), config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertTrue(config.includeServerNode);

        props.setProperty("includeServerNode", "false");
        config = new FileResourceModelSource.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals("resourceyaml", config.format);
        assertNotNull(config.nodesFile);
        assertEquals(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"), config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        //test using file extension of file to determine format, using xml
        props.remove("format");
        props.setProperty("file", "src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        config = new FileResourceModelSource.Configuration(props);
        assertNull(config.format);

        props.setProperty("file", "src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.yaml");
        config = new FileResourceModelSource.Configuration(props);
        assertNull(config.format);

    }

    public void testGetNodes() throws Exception {

        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", "src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        props.setProperty("generateFileAutomatically", "false");
        props.setProperty("includeServerNode", "false");
        final FileResourceModelSource fileNodesProvider = new FileResourceModelSource(getFrameworkInstance());
        fileNodesProvider.configure(props);

        final INodeSet nodes = fileNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(2, nodes.getNodes().size());
        assertNotNull(nodes.getNode("test1"));
        assertNotNull(nodes.getNode("testnode2"));

    }
    public void testGetNodesYaml() throws Exception {
        File testfile = new File(frameworkProject.getEtcDir(), "testformat.yaml");
        assertFalse(testfile.exists());
        //create yaml file
        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
            (testfile))));
        bufferedWriter.write("testyaml1: \n"
                             + "  hostname: test\n"
                             + "  description: a description\n"
                             + "  tags: a, b, c\n"
                             + "  osArch: x86_64\n"
                             + "  osFamily: unix\n"
                             + "  osVersion: 10.6.5\n"
                             + "  osName: Mac OS X\n"
                             + "  username: a user\n");
        bufferedWriter.flush();
        bufferedWriter.close();
        assertTrue(testfile.exists());

        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", testfile.getAbsolutePath());
        props.setProperty("generateFileAutomatically", "false");
        props.setProperty("includeServerNode", "false");
        final FileResourceModelSource fileNodesProvider = new FileResourceModelSource(getFrameworkInstance());
        fileNodesProvider.configure(props);

        final INodeSet nodes = fileNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.getNodes().size());
        assertNotNull(nodes.getNode("testyaml1"));
        testfile.delete();
    }
    public void testGetNodesIncludeServerNode() throws Exception {
        File testfile = new File(frameworkProject.getEtcDir(), "testresources.yaml");
        assertFalse(testfile.exists());

        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", testfile.getAbsolutePath());
        props.setProperty("generateFileAutomatically", "false");
        props.setProperty("includeServerNode", "true");
        final FileResourceModelSource fileNodesProvider = new FileResourceModelSource(getFrameworkInstance());
        fileNodesProvider.configure(props);

        final INodeSet nodes = fileNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.getNodes().size());
        assertNotNull(nodes.getNode(getFrameworkInstance().getFrameworkNodeName()));
        assertFalse(testfile.exists());
    }
    public void testGetNodesGenerateFileAutomaticallyWithFormatYaml() throws Exception {

        //explicit format resourceyaml
        File testfile = File.createTempFile("testresources2", ".blah");
        testfile.delete();
        assertFalse(testfile.exists());

        Properties props = new Properties();

        props.setProperty("format", "resourceyaml");

        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", testfile.getAbsolutePath());
        props.setProperty("generateFileAutomatically", "true");
        props.setProperty("includeServerNode", "true");
        final FileResourceModelSource fileNodesProvider = new FileResourceModelSource(getFrameworkInstance());
        fileNodesProvider.configure(props);

        final INodeSet nodes = fileNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.getNodes().size());
        assertNotNull(nodes.getNode(getFrameworkInstance().getFrameworkNodeName()));
        assertTrue(testfile.exists());
        testfile.delete();
    }
    public void testGetNodesGenerateFileAutomaticallyParentDirs() throws Exception {

        //explicit format resourceyaml
        File testfile2 = File.createTempFile("test", "blah");
        testfile2.delete();
        assertFalse(testfile2.exists());
        File testfile = new File(testfile2, "sub/dir/temp.blah");
        assertFalse(testfile.exists());

        Properties props = new Properties();

        props.setProperty("format", "resourceyaml");

        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", testfile.getAbsolutePath());
        props.setProperty("generateFileAutomatically", "true");
        props.setProperty("includeServerNode", "true");
        final FileResourceModelSource fileNodesProvider = new FileResourceModelSource(getFrameworkInstance());
        fileNodesProvider.configure(props);

        final INodeSet nodes = fileNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.getNodes().size());
        assertNotNull(nodes.getNode(getFrameworkInstance().getFrameworkNodeName()));
        assertTrue(testfile.exists());
        testfile.delete();
    }
    public void testGetNodesGenerateFileAutomaticallyWithFormatXml() throws Exception {

        //explicit format resourcexml
            File testfile2 = new File(frameworkProject.getEtcDir(), "testresources2.blah");
            assertFalse(testfile2.exists());

            Properties props2 = new Properties();

            props2.setProperty("format", "resourcexml");

            props2.setProperty("project", PROJ_NAME);
            props2.setProperty("file", testfile2.getAbsolutePath());
            props2.setProperty("generateFileAutomatically", "true");
            props2.setProperty("includeServerNode", "true");
            final FileResourceModelSource provider2 = new FileResourceModelSource(getFrameworkInstance());
            provider2.configure(props2);

            final INodeSet nodes2 = provider2.getNodes();
            assertNotNull(nodes2);
            assertEquals(1, nodes2.getNodes().size());
            assertNotNull(nodes2.getNode(getFrameworkInstance().getFrameworkNodeName()));
            assertTrue(testfile2.exists());
            testfile2.delete();
    }
        //implicit from filename

    public void testGetNodesGenerateFileAutomaticallyWithFilenameYaml() throws Exception {
            File testfile2 = new File(frameworkProject.getEtcDir(), "testresources2.yaml");
            assertFalse(testfile2.exists());

            Properties props2 = new Properties();
            props2.setProperty("project", PROJ_NAME);
            props2.setProperty("file", testfile2.getAbsolutePath());
            props2.setProperty("generateFileAutomatically", "true");
            props2.setProperty("includeServerNode", "true");
            final FileResourceModelSource provider2 = new FileResourceModelSource(getFrameworkInstance());
            provider2.configure(props2);

            final INodeSet nodes2 = provider2.getNodes();
            assertNotNull(nodes2);
            assertEquals(1, nodes2.getNodes().size());
            assertNotNull(nodes2.getNode(getFrameworkInstance().getFrameworkNodeName()));
            assertTrue(testfile2.exists());
    }
    //implicit from filename
    public void testGetNodesGenerateFileAutomaticallyWithFilenameXml() throws Exception {
            File testfile2 = new File(frameworkProject.getEtcDir(), "testresources2.xml");
            assertFalse(testfile2.exists());

            Properties props2 = new Properties();
            props2.setProperty("project", PROJ_NAME);
            props2.setProperty("file", testfile2.getAbsolutePath());
            props2.setProperty("generateFileAutomatically", "true");
            props2.setProperty("includeServerNode", "true");
            final FileResourceModelSource provider2 = new FileResourceModelSource(getFrameworkInstance());
            provider2.configure(props2);

            final INodeSet nodes2 = provider2.getNodes();
            assertNotNull(nodes2);
            assertEquals(1, nodes2.getNodes().size());
            assertNotNull(nodes2.getNode(getFrameworkInstance().getFrameworkNodeName()));
            assertTrue(testfile2.exists());
    }

    public void testParseFile() throws Exception {
        File testfile= new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        final INodeSet iNodeSet = FileResourceModelSource.parseFile(testfile, getFrameworkInstance(), PROJ_NAME);
        assertNotNull(iNodeSet);
        assertEquals(2, iNodeSet.getNodes().size());
        assertNotNull(iNodeSet.getNode("test1"));
        assertNotNull(iNodeSet.getNode("testnode2"));

        File testfile2 = File.createTempFile("testParseFile", ".yaml");
        //create yaml file
        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
            (testfile2))));
        bufferedWriter.write("testyaml1: \n"
                             + "  hostname: test\n"
                             + "  description: a description\n"
                             + "  tags: a, b, c\n"
                             + "  osArch: x86_64\n"
                             + "  osFamily: unix\n"
                             + "  osVersion: 10.6.5\n"
                             + "  osName: Mac OS X\n"
                             + "  username: a user\n");
        bufferedWriter.flush();
        bufferedWriter.close();
        assertTrue(testfile2.exists());

        final INodeSet nodeSet2 = FileResourceModelSource.parseFile(testfile2, getFrameworkInstance(), PROJ_NAME);
        assertNotNull(nodeSet2);
        assertEquals(1, nodeSet2.getNodes().size());
        assertNotNull(nodeSet2.getNode("testyaml1"));

        //test failures
        File dneFile = new File("build/DNEFile.xml");

        try {
            final INodeSet result = FileResourceModelSource.parseFile(dneFile, getFrameworkInstance(), PROJ_NAME);
            fail();
        } catch (ResourceModelSourceException e) {
            assertEquals("File does not exist: " + dneFile.getAbsolutePath(), e.getMessage());
        }
    }

}
