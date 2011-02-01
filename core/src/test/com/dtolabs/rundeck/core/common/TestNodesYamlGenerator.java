/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
/*
* TestNodesYamlGenerator.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 19, 2011 12:34:03 PM
*
*/

import junit.framework.*;
import com.dtolabs.rundeck.core.common.NodesYamlGenerator;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.HashMap;

import org.yaml.snakeyaml.representer.Representer;

public class TestNodesYamlGenerator extends TestCase {


    public TestNodesYamlGenerator(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestNodesYamlGenerator.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testShouldSupportOutputStream() throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(baos);
        final NodeEntryImpl nodeEntry = new NodeEntryImpl();
        nodeEntry.setNodename("test1");
        nodeEntry.setHostname("testhostname");
        nodesYamlGenerator.addNode(nodeEntry);
        nodesYamlGenerator.generate();
        final byte[] bytes = baos.toByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }
    public void testShouldSupportWriter() throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(new OutputStreamWriter(baos));
        final NodeEntryImpl nodeEntry = new NodeEntryImpl();
        nodeEntry.setNodename("test1");
        nodeEntry.setHostname("testhostname");
        nodesYamlGenerator.addNode(nodeEntry);
        nodesYamlGenerator.generate();
        final byte[] bytes = baos.toByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }
    public void testShouldSupportFile() throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        File tempfile = File.createTempFile("out", "temp");
        tempfile.deleteOnExit();

        NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(tempfile);
        final NodeEntryImpl nodeEntry = new NodeEntryImpl();
        nodeEntry.setNodename("test1");
        nodeEntry.setHostname("testhostname");
        nodesYamlGenerator.addNode(nodeEntry);
        nodesYamlGenerator.generate();
        assertTrue(tempfile.length() > 0);
    }
    public void testGenerateShouldFailOnNullOutput() throws Exception{
        NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator((File)null);
        final NodeEntryImpl nodeEntry = new NodeEntryImpl();
        nodeEntry.setNodename("test1");
        nodeEntry.setHostname("testhostname");
        nodesYamlGenerator.addNode(nodeEntry);
        try {
            nodesYamlGenerator.generate();
            fail("Should have failed");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }
    public void testGenerate() throws Exception {
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(baos);

            try {
                nodesYamlGenerator.generate();
                fail("Should have thrown an Exception");
            }
            catch (NodesGeneratorException ex) {
            }
        }
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(baos);
            final NodeEntryImpl nodeEntry = new NodeEntryImpl();
            nodeEntry.setNodename("test1");
            nodeEntry.setHostname("testhostname");
            nodesYamlGenerator.addNode(nodeEntry);

            nodesYamlGenerator.generate();
            final String outputString = baos.toString();
            assertNotNull(outputString);
            assertEquals("test1:\n"
                         + "  hostname: testhostname\n"
                         + "  nodename: test1\n"
                         + "  tags: ''\n", outputString);
        }
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(baos);
            final NodeEntryImpl nodeEntry = new NodeEntryImpl();
            nodeEntry.setNodename("test1");
            nodeEntry.setHostname("testhostname");
            nodeEntry.setDescription("test description");
            nodeEntry.setOsArch("an os arch");
            nodeEntry.setOsFamily("an os fam");
            nodeEntry.setOsName("an os name");
            nodeEntry.setOsVersion("an os vers");
            nodeEntry.setUsername("someUser");
            nodeEntry.setType("a type");
            nodeEntry.setTags(new HashSet());

            nodesYamlGenerator.addNode(nodeEntry);

            nodesYamlGenerator.generate();
            final String outputString = baos.toString();
            assertNotNull(outputString);
            assertEquals("test1:\n"
                         + "  description: test description\n"
                         + "  hostname: testhostname\n"
                         + "  nodename: test1\n"
                         + "  osArch: an os arch\n"
                         + "  osFamily: an os fam\n"
                         + "  osName: an os name\n"
                         + "  osVersion: an os vers\n"
                         + "  tags: ''\n"
//                         + "  type: a type\n"
+ "  username: someUser\n"
                , outputString);
        }
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(baos);
            final NodeEntryImpl nodeEntry = new NodeEntryImpl();
            nodeEntry.setNodename("strongbad");
            nodeEntry.setHostname("strongbad");
            nodeEntry.setDescription("Rundeck server node");
            nodeEntry.setOsArch("x86_64");
            nodeEntry.setOsFamily("unix");
            nodeEntry.setOsName("Mac OS X");
            nodeEntry.setOsVersion("10.6.5");
            nodeEntry.setUsername("alexh");
            nodeEntry.setType("ignored");
            nodeEntry.setFrameworkProject("ignored");
            final HashSet tags = new HashSet();
            tags.add("rundeck");
            tags.add("dev");
            tags.add("ops");
            nodeEntry.setTags(tags);
            /**
             * strongbad:

             type: Node
             description: "Rundeck server node"
             hostname: "strongbad"
             osArch: "x86_64"
             osFamily: "unix"
             osName: "Mac OS X"
             osVersion: "10.6.5"
             username: "alexh"
             editUrl: ""
             remoteUrl: ""
             tags: "rundeck"
             */

            nodesYamlGenerator.addNode(nodeEntry);

            nodesYamlGenerator.generate();
            final String outputString = baos.toString();
            assertNotNull(outputString);
            assertEquals("strongbad:\n"
                         + "  description: Rundeck server node\n"
                         + "  hostname: strongbad\n"
                         + "  nodename: strongbad\n"
                         + "  osArch: x86_64\n"
                         + "  osFamily: unix\n"
                         + "  osName: Mac OS X\n"
                         + "  osVersion: 10.6.5\n"
                         + "  tags: dev, rundeck, ops\n"
//                         + "  type: a type\n"
+ "  username: alexh\n"
                , outputString);
        }
    }

    public void testShouldOutputEditUrl() throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(baos);
        final NodeEntryImpl nodeEntry = new NodeEntryImpl();
        nodeEntry.setNodename("strongbad");
        nodeEntry.setHostname("strongbad");
        nodeEntry.setAttributes(new HashMap<String, String>());
        nodeEntry.getAttributes().put("editUrl","http://some.com/test/url");

        nodesYamlGenerator.addNode(nodeEntry);

        nodesYamlGenerator.generate();
        final String outputString = baos.toString();
        assertNotNull(outputString);
        assertEquals("strongbad:\n"
                     + "  editUrl: http://some.com/test/url\n"
                     + "  hostname: strongbad\n"
                     + "  nodename: strongbad\n"
                     + "  tags: ''\n"
            , outputString);
    }
    
    public void testShouldOutputRemoteUrl() throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(baos);
        final NodeEntryImpl nodeEntry = new NodeEntryImpl();
        nodeEntry.setNodename("strongbad");
        nodeEntry.setHostname("strongbad");
        nodeEntry.setAttributes(new HashMap<String, String>());
        nodeEntry.getAttributes().put("remoteUrl", "http://somez.com/test/other/url");

        nodesYamlGenerator.addNode(nodeEntry);

        nodesYamlGenerator.generate();
        final String outputString = baos.toString();
        assertNotNull(outputString);
        assertEquals("strongbad:\n"
                     + "  hostname: strongbad\n"
                     + "  nodename: strongbad\n"
                     + "  remoteUrl: http://somez.com/test/other/url\n"
                     + "  tags: ''\n"
            , outputString);
    }
}