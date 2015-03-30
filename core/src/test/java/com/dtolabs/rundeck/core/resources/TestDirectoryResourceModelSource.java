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
* TestDirectoryNodesProvider.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 11:42 AM
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
import org.junit.Assert;

import java.io.*;
import java.util.Properties;

/**
 * TestDirectoryNodesProvider is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestDirectoryResourceModelSource extends AbstractBaseTest {
    public static final String PROJ_NAME = "TestDirectoryNodesProvider";

    public TestDirectoryResourceModelSource(String name) {
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


    public void testConfiguration() {
        try {
            DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        Properties props = new Properties();
        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        assertNull(config.project);
        assertNull(config.directory);

        props.setProperty("project", PROJ_NAME);
        config = new DirectoryResourceModelSource.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNull(config.directory);

        props.setProperty("directory", "target/test");
        config = new DirectoryResourceModelSource.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.directory);
        assertEquals(new File("target/test"), config.directory);
    }
    public void testValidation() throws Exception{
        try {
            DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        Properties props = new Properties();
        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        try{
            config.validate();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("project is required", e.getMessage());
        }

        props.setProperty("project", PROJ_NAME);
        config = new DirectoryResourceModelSource.Configuration(props);
        try {
            config.validate();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("directory is required", e.getMessage());
        }

        File testfile = File.createTempFile("testfile", "test");
        //set directory to point to a file instead of a directory
        props.setProperty("directory", testfile.getAbsolutePath());

        config = new DirectoryResourceModelSource.Configuration(props);
        try {
            config.validate();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("path specified is not a directory: " + testfile.getAbsolutePath(), e.getMessage());
        }
    }

    public void testGetNodesMissingDir() throws Exception{
        File directory = new File(frameworkProject.getBaseDir(), "testGetNodesMissingDir");
        assertFalse(directory.isDirectory());

        //test with no files.
        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("directory", directory.getAbsolutePath());
        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        final DirectoryResourceModelSource directoryNodesProvider = new DirectoryResourceModelSource(getFrameworkInstance());
        directoryNodesProvider.configure(config);

        final INodeSet nodes = directoryNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(0, nodes.getNodes().size());
        FileUtils.deleteDir(directory);
    }
    public void testGetNodesEmptyDir() throws Exception{
        File directory = new File(frameworkProject.getBaseDir(), "testGetNodesEmptyDir");
        directory.mkdirs();
        assertTrue(directory.isDirectory());

        //test with no files.
        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("directory", directory.getAbsolutePath());
        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        final DirectoryResourceModelSource directoryNodesProvider = new DirectoryResourceModelSource(getFrameworkInstance());
        directoryNodesProvider.configure(config);

        final INodeSet nodes = directoryNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(0, nodes.getNodes().size());
        FileUtils.deleteDir(directory);
    }
    public void testGetNodesSingleFile() throws Exception{
        File directory = new File(frameworkProject.getBaseDir(), "testGetNodesSingleFile");
        directory.mkdirs();
        assertTrue(directory.isDirectory());

        File file1 = new File(directory, "test1.xml");
        try {
            FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                file1);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }
        //test with single files.
        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("directory", directory.getAbsolutePath());
        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        final DirectoryResourceModelSource directoryNodesProvider = new DirectoryResourceModelSource(getFrameworkInstance());
        directoryNodesProvider.configure(config);

        final INodeSet nodes = directoryNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(2, nodes.getNodes().size());
        assertNotNull(nodes.getNode("test1"));
        assertNotNull(nodes.getNode("testnode2"));
        FileUtils.deleteDir(directory);
    }
    public void testGetNodesMultiFile() throws Exception{
        File directory = new File(frameworkProject.getBaseDir(), "testGetNodesMultiFile");
        directory.mkdirs();
        assertTrue(directory.isDirectory());

        File file1 = new File(directory, "test1.xml");
        try {
            FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                file1);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }
        File file2 = new File(directory, "test1.yaml");
        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
            (file2))));
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
        //test with single files.
        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("directory", directory.getAbsolutePath());
        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        final DirectoryResourceModelSource directoryNodesProvider = new DirectoryResourceModelSource(getFrameworkInstance());
        directoryNodesProvider.configure(config);

        final INodeSet nodes = directoryNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(3, nodes.getNodes().size());
        assertNotNull(nodes.getNode("test1"));
        assertNotNull(nodes.getNode("testnode2"));
        assertNotNull(nodes.getNode("testyaml1"));
        FileUtils.deleteDir(directory);
    }
    public void testGetNodesMultiFileModified() throws Exception{
        File directory = new File(frameworkProject.getBaseDir(), "testGetNodesMultiFileModified");
        directory.mkdirs();
        assertTrue(directory.isDirectory());

        File file1 = new File(directory, "test1.xml");
        try {
            FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                file1);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }
        File file2 = new File(directory, "test1.yaml");
        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
            (file2))));
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
        //test with single files.
        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("directory", directory.getAbsolutePath());
        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        final DirectoryResourceModelSource directoryNodesProvider = new DirectoryResourceModelSource(getFrameworkInstance());
        directoryNodesProvider.configure(config);

        final INodeSet nodes = directoryNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(3, nodes.getNodes().size());
        assertNotNull(nodes.getNode("test1"));
        assertNotNull(nodes.getNode("testnode2"));
        assertNotNull(nodes.getNode("testyaml1"));

        assertEquals("a description", nodes.getNode("testyaml1").getDescription());

        //now sleep 100ms, modify file
        Thread.sleep(10001);
        final BufferedWriter bufferedWriter2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
            (file2))));
        bufferedWriter2.write("testyaml1: \n"
                             + "  hostname: test\n"
                             + "  description: a new description\n"
                             + "  tags: a, b, c\n"
                             + "  osArch: x86_64\n"
                             + "  osFamily: unix\n"
                             + "  osVersion: 10.6.5\n"
                             + "  osName: Mac OS X\n"
                             + "  username: a user\n"
                             + "testyaml2: \n"
                             + "  hostname: test\n"
                             + "  description: a description\n"
                             + "  tags: a, b, c\n"
                             + "  osArch: x86_64\n"
                             + "  osFamily: unix\n"
                             + "  osVersion: 10.6.5\n"
                             + "  osName: Mac OS X\n"
                             + "  username: a user\n");
        bufferedWriter2.flush();
        bufferedWriter2.close();


        final INodeSet nodes2 = directoryNodesProvider.getNodes();
        assertNotNull(nodes2);
        assertEquals(4, nodes2.getNodes().size());
        assertNotNull(nodes2.getNode("test1"));
        assertNotNull(nodes2.getNode("testnode2"));
        assertNotNull(nodes2.getNode("testyaml1"));
        assertNotNull(nodes2.getNode("testyaml2"));

        assertEquals("a new description", nodes2.getNode("testyaml1").getDescription());
        FileUtils.deleteDir(directory);
    }

    public void testGetNodesMultiFileAdded() throws Exception{
        File directory = new File(frameworkProject.getBaseDir(), "testGetNodesMultiFileAdded");
        directory.mkdirs();
        assertTrue(directory.isDirectory());

        File file1 = new File(directory, "test1.xml");
        try {
            FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                file1);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }
        //test with single files.
        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("directory", directory.getAbsolutePath());
        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        final DirectoryResourceModelSource directoryNodesProvider = new DirectoryResourceModelSource(getFrameworkInstance());
        directoryNodesProvider.configure(config);

        File file2 = new File(directory, "test1.yaml");
        assertFalse(file2.exists());
        
        final INodeSet nodes = directoryNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(2, nodes.getNodes().size());
        assertNotNull(nodes.getNode("test1"));
        assertNotNull(nodes.getNode("testnode2"));

        //add a new file

        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
            (file2))));
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

        final INodeSet nodes2 = directoryNodesProvider.getNodes();
        assertNotNull(nodes2);
        assertEquals(3, nodes2.getNodes().size());
        assertNotNull(nodes2.getNode("test1"));
        assertNotNull(nodes2.getNode("testnode2"));
        assertNotNull(nodes2.getNode("testyaml1"));

        FileUtils.deleteDir(directory);
    }


    public void testGetNodesMultiFileRemoved() throws Exception {
        File directory = new File(frameworkProject.getBaseDir(), "testGetNodesMultiFileRemoved");
        directory.mkdirs();
        assertTrue(directory.isDirectory());

        File file1 = new File(directory, "test1.xml");
        try {
            FileUtils.copyFileStreams(new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                file1);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }
        File file2 = new File(directory, "test1.yaml");
        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
            (file2))));
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
        //test with single files.
        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("directory", directory.getAbsolutePath());
        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        final DirectoryResourceModelSource directoryNodesProvider = new DirectoryResourceModelSource(getFrameworkInstance());
        directoryNodesProvider.configure(config);

        final INodeSet nodes = directoryNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(3, nodes.getNodes().size());
        assertNotNull(nodes.getNode("test1"));
        assertNotNull(nodes.getNode("testnode2"));
        assertNotNull(nodes.getNode("testyaml1"));

        assertEquals("a description", nodes.getNode("testyaml1").getDescription());

        //now remove file
        file2.delete();


        final INodeSet nodes2 = directoryNodesProvider.getNodes();
        assertNotNull(nodes2);
        assertEquals(2, nodes2.getNodes().size());
        assertNotNull(nodes2.getNode("test1"));
        assertNotNull(nodes2.getNode("testnode2"));
        assertNull(nodes2.getNode("testyaml1"));

        FileUtils.deleteDir(directory);
    }

    public void testGetNodesOrdering() throws Exception{
        File directory = new File(frameworkProject.getBaseDir(), "testGetNodesOrdering");
        directory.mkdirs();
        assertTrue(directory.isDirectory());

        File file1 = new File(directory, "testA.yaml");
        final BufferedWriter writer1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
            (file1))));
        writer1.write("test1: \n"
                             + "  hostname: test\n"
                             + "  description: A description\n"
                             + "  tags: a, b, c\n"
                             + "  osArch: x86_64\n"
                             + "  osFamily: unix\n"
                             + "  osVersion: 10.6.5\n"
                             + "  osName: Mac OS X\n"
                             + "  username: a user\n"
                             + "test2: \n"
                             + "  hostname: test\n"
                             + "  description: A description\n"
                             + "  tags: a, b, c\n"
                             + "  osArch: x86_64\n"
                             + "  osFamily: unix\n"
                             + "  osVersion: 10.6.5\n"
                             + "  osName: Mac OS X\n"
                             + "  username: a user\n"
                             + "test3: \n"
                             + "  hostname: test\n"
                             + "  description: A description\n"
                             + "  tags: a, b, c\n"
                             + "  osArch: x86_64\n"
                             + "  osFamily: unix\n"
                             + "  osVersion: 10.6.5\n"
                             + "  osName: Mac OS X\n"
                             + "  username: a user\n");
        writer1.flush();
        writer1.close();

        File file2 = new File(directory, "testB.yaml");
        final BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
            (file2))));
        writer2.write("test2: \n"
                             + "  hostname: test\n"
                             + "  description: B description\n"
                             + "  tags: a, b, c\n"
                             + "  osArch: x86_64\n"
                             + "  osFamily: unix\n"
                             + "  osVersion: 10.6.5\n"
                             + "  osName: Mac OS X\n"
                             + "  username: a user\n"
                             + "test3: \n"
                             + "  hostname: test\n"
                             + "  description: B description\n"
                             + "  tags: a, b, c\n"
                             + "  osArch: x86_64\n"
                             + "  osFamily: unix\n"
                             + "  osVersion: 10.6.5\n"
                             + "  osName: Mac OS X\n"
                             + "  username: a user\n");
        writer2.flush();
        writer2.close();

        File file3 = new File(directory, "testC.yaml");
        final BufferedWriter writer3 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
            (file3))));
        writer3.write("test3: \n"
                             + "  hostname: test\n"
                             + "  description: C description\n"
                             + "  tags: a, b, c\n"
                             + "  osArch: x86_64\n"
                             + "  osFamily: unix\n"
                             + "  osVersion: 10.6.5\n"
                             + "  osName: Mac OS X\n"
                             + "  username: a user\n");
        writer3.flush();
        writer3.close();


        //test with single files.
        Properties props = new Properties();
        props.setProperty("project", PROJ_NAME);
        props.setProperty("directory", directory.getAbsolutePath());
        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        final DirectoryResourceModelSource directoryNodesProvider = new DirectoryResourceModelSource(getFrameworkInstance());
        directoryNodesProvider.configure(config);

        final INodeSet nodes = directoryNodesProvider.getNodes();
        assertNotNull(nodes);
        assertEquals(3, nodes.getNodes().size());
        assertNotNull(nodes.getNode("test1"));
        assertNotNull(nodes.getNode("test2"));
        assertNotNull(nodes.getNode("test3"));

        assertEquals("A description", nodes.getNode("test1").getDescription());
        assertEquals("B description", nodes.getNode("test2").getDescription());
        assertEquals("C description", nodes.getNode("test3").getDescription());


        //change modification time of a file
        assertTrue(file1.setLastModified(System.currentTimeMillis()));
        final INodeSet nodes2 = directoryNodesProvider.getNodes();
        assertNotNull(nodes2);
        assertEquals(3, nodes2.getNodes().size());
        assertNotNull(nodes2.getNode("test1"));
        assertNotNull(nodes2.getNode("test2"));
        assertNotNull(nodes2.getNode("test3"));

        assertEquals("A description", nodes2.getNode("test1").getDescription());
        assertEquals("B description", nodes2.getNode("test2").getDescription());
        assertEquals("C description", nodes2.getNode("test3").getDescription());


        FileUtils.deleteDir(directory);
    }
}
