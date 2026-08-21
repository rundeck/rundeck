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
import java.util.HashMap;
import java.util.Map;
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

    public void testGetNodes_missing_notrequired() throws Exception {

        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", "src/test/resources/com/dtolabs/rundeck/core/common/test-DNE.xml");
        props.setProperty("generateFileAutomatically", "false");
        props.setProperty("includeServerNode", "false");
        props.setProperty("requireFileExists", "false");
        final FileResourceModelSource fileNodesProvider = new FileResourceModelSource(getFrameworkInstance());
        fileNodesProvider.configure(props);

        final INodeSet nodes = fileNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(0, nodes.getNodes().size());

    }

    public void testGetNodes_missing_required() throws Exception {

        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", "src/test/resources/com/dtolabs/rundeck/core/common/test-DNE.xml");
        props.setProperty("generateFileAutomatically", "false");
        props.setProperty("includeServerNode", "false");
        props.setProperty("requireFileExists", "true");
        final FileResourceModelSource fileNodesProvider = new FileResourceModelSource(getFrameworkInstance());
        fileNodesProvider.configure(props);

        final INodeSet nodes;
        try {
            nodes = fileNodesProvider.getNodes();
            fail();
        } catch (ResourceModelSourceException e) {
            assertTrue(e.getMessage()
                        .contains("File does not exist: src/test/resources/com/dtolabs/rundeck/core/common/test-DNE.xml"));
        }

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

        // clean up
        testfile.delete();
        new File(testfile2, "sub/dir").delete();
        new File(testfile2, "sub").delete();
        testfile2.delete();
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
        testfile2.deleteOnExit();
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
            assertTrue(e.getMessage().contains("File does not exist: " + dneFile.getAbsolutePath()));
        }
    }

    public void testGetNodesWritableEmpty() throws Exception {
        Properties props = new Properties();
        File temp = File.createTempFile("test-nodesX", ".xml");
        temp.delete();
        temp.deleteOnExit();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", temp.getAbsolutePath());
        props.setProperty("generateFileAutomatically", "true");
        props.setProperty("includeServerNode", "false");
        final FileResourceModelSource fileNodesProvider = new FileResourceModelSource(getFrameworkInstance());
        fileNodesProvider.configure(props);

        final InputStream is = fileNodesProvider.openFileDataInputStream();
        //assertNotNull(is);
        final INodeSet nodes = fileNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(0, nodes.getNodes().size());
    }

    public void testWriteFileDataCreatesMissingParentDirectory() throws Exception {
        File tempDir = File.createTempFile("test-write-parent-dir", "");
        tempDir.delete();
        File missingParentDir = new File(tempDir, "nested/etc");
        File nodesFile = new File(missingParentDir, "writable-resource-file.xml");
        try {
            assertFalse("parent dir should not exist yet", missingParentDir.exists());

            Properties props = new Properties();
            props.setProperty("project", PROJ_NAME);
            props.setProperty("file", nodesFile.getAbsolutePath());
            props.setProperty("generateFileAutomatically", "false");
            props.setProperty("includeServerNode", "false");
            props.setProperty("writeable", "true");
            final FileResourceModelSource fileNodesProvider = new FileResourceModelSource(getFrameworkInstance());
            fileNodesProvider.configure(props);

            File testfile = new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml");
            try (InputStream is = new FileInputStream(testfile)) {
                fileNodesProvider.writeData(is);
            }

            assertTrue("parent dir should have been created", missingParentDir.exists());
            assertTrue("file should have been written", nodesFile.exists());
        } finally {
            FileUtils.deleteDir(tempDir);
        }
    }

    // Security validation tests for RUN-4671

    public void testValidateWriteableSource_PathWithinProjectDirectory() throws Exception {
        // Test that paths within the project directory are allowed
        Properties props = new Properties();
        File projectDir = frameworkProject.getBaseDir();
        File nodesFile = new File(projectDir, "etc/resources.xml");
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", nodesFile.getAbsolutePath());

        FileResourceModelSource source = new FileResourceModelSource(getFrameworkInstance());
        source.configure(props);

        Map<String, Object> configProps = new HashMap<>();
        // No allowed paths - only project directory should be allowed

        // Should not throw - path is within project
        source.validateWriteableSource(configProps, getFrameworkInstance(), PROJ_NAME);
    }

    public void testValidateWriteableSource_PathOutsideProjectDirectory() throws Exception {
        // Test that paths outside project directory are rejected by default
        Properties props = new Properties();
        File outsideFile = File.createTempFile("test-outside", ".xml");
        outsideFile.deleteOnExit();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", outsideFile.getAbsolutePath());

        FileResourceModelSource source = new FileResourceModelSource(getFrameworkInstance());
        source.configure(props);

        Map<String, Object> configProps = new HashMap<>();
        // No allowed paths configured

        try {
            source.validateWriteableSource(configProps, getFrameworkInstance(), PROJ_NAME);
            fail("Expected ConfigurationException for path outside project");
        } catch (ConfigurationException e) {
            assertTrue(e.getMessage().contains("must be within the project directory"));
        }
    }

    public void testValidateWriteableSource_PathTraversal() throws Exception {
        // Test that path traversal attempts are blocked
        Properties props = new Properties();
        File projectDir = frameworkProject.getBaseDir();
        // Attempt to escape project directory using ../
        File traversalFile = new File(projectDir, "../../../etc/passwd");
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", traversalFile.getAbsolutePath());

        FileResourceModelSource source = new FileResourceModelSource(getFrameworkInstance());
        source.configure(props);

        Map<String, Object> configProps = new HashMap<>();

        try {
            source.validateWriteableSource(configProps, getFrameworkInstance(), PROJ_NAME);
            fail("Expected ConfigurationException for path traversal");
        } catch (ConfigurationException e) {
            // Path traversal should be blocked - canonical path will be outside project
            assertTrue(e.getMessage().contains("must be within the project directory"));
        }
    }

    public void testValidateWriteableSource_AllowedBasePaths() throws Exception {
        // Test that configuration property allows additional paths
        Properties props = new Properties();
        File allowedDir = File.createTempFile("test-allowed", ".dir");
        allowedDir.delete();
        allowedDir.mkdir();
        allowedDir.deleteOnExit();

        File nodesFile = new File(allowedDir, "resources.xml");
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", nodesFile.getAbsolutePath());

        FileResourceModelSource source = new FileResourceModelSource(getFrameworkInstance());
        source.configure(props);

        Map<String, Object> configProps = new HashMap<>();
        configProps.put("resourceModelSource.file.allowedBasePaths", allowedDir.getAbsolutePath());

        // Should not throw - path is in allowed base paths
        source.validateWriteableSource(configProps, getFrameworkInstance(), PROJ_NAME);
    }

    public void testValidateWriteableSource_MultipleAllowedPaths() throws Exception {
        // Test that multiple comma-separated allowed paths work
        Properties props = new Properties();
        File allowedDir1 = File.createTempFile("test-allowed1", ".dir");
        allowedDir1.delete();
        allowedDir1.mkdir();
        allowedDir1.deleteOnExit();

        File allowedDir2 = File.createTempFile("test-allowed2", ".dir");
        allowedDir2.delete();
        allowedDir2.mkdir();
        allowedDir2.deleteOnExit();

        File nodesFile = new File(allowedDir2, "resources.xml");
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", nodesFile.getAbsolutePath());

        FileResourceModelSource source = new FileResourceModelSource(getFrameworkInstance());
        source.configure(props);

        Map<String, Object> configProps = new HashMap<>();
        // File is in allowedDir2, not allowedDir1
        configProps.put("resourceModelSource.file.allowedBasePaths",
                       allowedDir1.getAbsolutePath() + "," + allowedDir2.getAbsolutePath());

        // Should not throw - path is in one of the allowed base paths
        source.validateWriteableSource(configProps, getFrameworkInstance(), PROJ_NAME);
    }

    public void testValidateWriteableSource_CanonicalizationNormalizesPath() throws Exception {
        // Test that paths are canonicalized (handles ./ and ../ correctly)
        Properties props = new Properties();
        File projectDir = frameworkProject.getBaseDir();
        // Use redundant path elements that should normalize to project directory
        File nodesFile = new File(projectDir, "./etc/../etc/./resources.xml");
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", nodesFile.getAbsolutePath());

        FileResourceModelSource source = new FileResourceModelSource(getFrameworkInstance());
        source.configure(props);

        Map<String, Object> configProps = new HashMap<>();

        // Should not throw - canonical path is within project
        source.validateWriteableSource(configProps, getFrameworkInstance(), PROJ_NAME);
    }

    public void testValidateWriteableSource_ErrorMessageDoesNotLeakPath() throws Exception {
        // Test that error messages don't expose sensitive path information
        Properties props = new Properties();
        File sensitiveFile = new File("/etc/passwd");
        props.setProperty("project", PROJ_NAME);
        props.setProperty("file", sensitiveFile.getAbsolutePath());

        FileResourceModelSource source = new FileResourceModelSource(getFrameworkInstance());
        source.configure(props);

        Map<String, Object> configProps = new HashMap<>();

        try {
            source.validateWriteableSource(configProps, getFrameworkInstance(), PROJ_NAME);
            fail("Expected ConfigurationException");
        } catch (ConfigurationException e) {
            // Error message should not contain the actual file path
            assertFalse("Error message should not leak file path",
                       e.getMessage().contains("/etc/passwd"));
            // But should have a generic message
            assertTrue(e.getMessage().contains("must be within the project directory"));
        }
    }

    public void testValidateWriteableSource_NoConfiguration() throws Exception {
        // Test that validation handles unconfigured source gracefully
        FileResourceModelSource source = new FileResourceModelSource(getFrameworkInstance());
        // Don't configure - configuration is null

        Map<String, Object> configProps = new HashMap<>();

        // Should not throw - no file configured yet
        source.validateWriteableSource(configProps, getFrameworkInstance(), PROJ_NAME);
    }

}
