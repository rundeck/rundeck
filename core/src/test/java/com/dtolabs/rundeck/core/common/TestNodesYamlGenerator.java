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
/*
* TestNodesYamlGenerator.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 19, 2011 12:34:03 PM
*
*/

import junit.framework.*;
import com.dtolabs.rundeck.core.common.NodesYamlGenerator;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.events.Event;
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
    public void testGenerateEmptyNodeSetIsBlank() throws Exception{
        StringWriter writer=new StringWriter();
        NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(writer);
        nodesYamlGenerator.generate();
        assertEquals("",writer.toString());
    }
    public void testGenerate() throws Exception {
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
            //convert via yaml
            Map parsed = parseYamlMap(outputString);
            assertNotNull(parsed.get("test1"));
            assertTrue(parsed.get("test1") instanceof Map);
            Map node1 = (Map) parsed.get("test1");
            assertEquals("testhostname", node1.get("hostname"));
            assertEquals("test1", node1.get("nodename"));
            assertEquals("", node1.get("tags"));
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
            nodeEntry.setTags(new HashSet());

            nodesYamlGenerator.addNode(nodeEntry);

            nodesYamlGenerator.generate();
            final String outputString = baos.toString();
            assertNotNull(outputString);
            //convert via yaml
            Map parsed = parseYamlMap(outputString);
            assertNotNull(parsed.get("test1"));
            assertTrue(parsed.get("test1") instanceof Map);
            Map node1 = (Map) parsed.get("test1");
            assertEquals("test description", node1.get("description"));
            assertEquals("testhostname", node1.get("hostname"));
            assertEquals("test1", node1.get("nodename"));
            assertEquals("an os arch", node1.get("osArch"));
            assertEquals("an os fam", node1.get("osFamily"));
            assertEquals("an os name", node1.get("osName"));
            assertEquals("an os vers", node1.get("osVersion"));
            assertEquals("someUser", node1.get("username"));
            assertEquals("", node1.get("tags"));
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
            //convert via yaml
            Map parsed = parseYamlMap(outputString);
            assertNotNull(parsed.get("strongbad"));
            assertTrue(parsed.get("strongbad") instanceof Map);
            Map node1 = (Map) parsed.get("strongbad");
            assertEquals("Rundeck server node", node1.get("description"));
            assertEquals("strongbad", node1.get("hostname"));
            assertEquals("strongbad", node1.get("nodename"));
            assertEquals("x86_64", node1.get("osArch"));
            assertEquals("unix", node1.get("osFamily"));
            assertEquals("Mac OS X", node1.get("osName"));
            assertEquals("10.6.5", node1.get("osVersion"));
            assertEquals("alexh", node1.get("username"));
            assertEquals("dev, ops, rundeck", node1.get("tags"));
        }
    }

    private Map parseYamlMap(String outputString) {
        Yaml yaml = new Yaml(new LoaderOptions());
        final Object load = yaml.load(new StringReader(outputString));
        assertNotNull(load);
        assertTrue(load instanceof Map);
        return (Map) load;
    }

    public void testShouldOutputEditUrl() throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(baos);
        final NodeEntryImpl nodeEntry = new NodeEntryImpl();
        nodeEntry.setNodename("strongbad");
        nodeEntry.setHostname("strongbad");
        nodeEntry.getAttributes().put("editUrl","http://some.com/test/url");

        nodesYamlGenerator.addNode(nodeEntry);

        nodesYamlGenerator.generate();
        final String outputString = baos.toString();
        assertNotNull(outputString);

        //convert via yaml
        Map parsed = parseYamlMap(outputString);
        assertNotNull(parsed.get("strongbad"));
        assertTrue(parsed.get("strongbad") instanceof Map);
        Map node1 = (Map) parsed.get("strongbad");
        assertEquals("strongbad", node1.get("hostname"));
        assertEquals("strongbad", node1.get("nodename"));
        assertEquals("http://some.com/test/url", node1.get("editUrl"));
        assertEquals("", node1.get("tags"));

    }
    
    public void testShouldOutputRemoteUrl() throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(baos);
        final NodeEntryImpl nodeEntry = new NodeEntryImpl();
        nodeEntry.setNodename("strongbad");
        nodeEntry.setHostname("strongbad");

        nodeEntry.getAttributes().put("remoteUrl", "http://somez.com/test/other/url");

        nodesYamlGenerator.addNode(nodeEntry);

        nodesYamlGenerator.generate();
        final String outputString = baos.toString();
        assertNotNull(outputString);
        //convert via yaml
        Map parsed = parseYamlMap(outputString);
        assertNotNull(parsed.get("strongbad"));
        assertTrue(parsed.get("strongbad") instanceof Map);
        Map node1 = (Map) parsed.get("strongbad");
        assertEquals("strongbad", node1.get("hostname"));
        assertEquals("strongbad", node1.get("nodename"));
        assertEquals("http://somez.com/test/other/url", node1.get("remoteUrl"));
        assertEquals("", node1.get("tags"));
    }

    public void testShouldOutputAnyAttribtue() throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(baos);
        final NodeEntryImpl nodeEntry = new NodeEntryImpl();
        nodeEntry.setNodename("strongbad");
        nodeEntry.setHostname("strongbad");
        
        nodeEntry.getAttributes().put("test-attribute", "some value");

        nodesYamlGenerator.addNode(nodeEntry);

        nodesYamlGenerator.generate();
        final String outputString = baos.toString();
        assertNotNull(outputString);
        //convert via yaml
        Map parsed = parseYamlMap(outputString);
        assertNotNull(parsed.get("strongbad"));
        assertTrue(parsed.get("strongbad") instanceof Map);
        Map node1 = (Map) parsed.get("strongbad");
        assertEquals("strongbad", node1.get("nodename"));
        assertEquals("some value", node1.get("test-attribute"));
    }
}