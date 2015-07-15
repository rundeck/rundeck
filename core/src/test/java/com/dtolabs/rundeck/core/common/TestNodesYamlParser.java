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
* TestNodesYamlParser.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 19, 2011 4:18:19 PM
*
*/

import junit.framework.*;
import com.dtolabs.rundeck.core.common.NodesYamlParser;

import java.util.HashMap;
import java.io.File;
import java.io.ByteArrayInputStream;

import org.yaml.snakeyaml.error.YAMLException;

public class TestNodesYamlParser extends TestCase {


    public TestNodesYamlParser(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestNodesYamlParser.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testParse() throws Exception {
        {
            testReceiver recv = new testReceiver();
            NodesYamlParser nodesYamlParser = new NodesYamlParser((File) null, recv);
            try {
                nodesYamlParser.parse();
                fail("Should have thrown an Exception");
            }
            catch (NullPointerException ex) {
            }
        }
        {
            testReceiver recv = new testReceiver();
            ByteArrayInputStream is = new ByteArrayInputStream(
                "test: \n  hostname: test\n".getBytes());

            NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
            nodesYamlParser.parse();
            assertEquals(1, recv.nodes.size());
            assertTrue(recv.nodes.containsKey("test"));
            INodeEntry entry = recv.nodes.get("test");
            assertNotNull(entry);
            assertEquals("test", entry.getNodename());
            assertEquals("test", entry.getHostname());
            assertNotNull(entry.getTags());
            assertEquals(0, entry.getTags().size());
            assertNull(entry.getOsArch());
            assertNull(entry.getOsFamily());
            assertNull(entry.getOsVersion());
            assertNull(entry.getOsName());
            assertNotNull(entry.getAttributes());
            assertNull(entry.getDescription());
            assertNull(entry.getFrameworkProject());
            assertNull(entry.getUsername());
        }
        {
            //test key for map data always overrides node name
            testReceiver recv = new testReceiver();
            ByteArrayInputStream is = new ByteArrayInputStream(
                "test: \n  nodename: bill\n  hostname: test\n".getBytes());

            NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
            nodesYamlParser.parse();
            assertEquals(1, recv.nodes.size());
            assertTrue(recv.nodes.containsKey("test"));
            INodeEntry entry = recv.nodes.get("test");
            assertNotNull(entry);
            assertEquals("test", entry.getNodename());
            assertEquals("test", entry.getHostname());
            assertNotNull(entry.getTags());
            assertEquals(0, entry.getTags().size());
            assertNull(entry.getOsArch());
            assertNull(entry.getOsFamily());
            assertNull(entry.getOsVersion());
            assertNull(entry.getOsName());
            assertNotNull(entry.getAttributes());
            assertNull(entry.getDescription());
            assertNull(entry.getFrameworkProject());
            assertNull(entry.getUsername());
        }
        {
            testReceiver recv = new testReceiver();
            ByteArrayInputStream is = new ByteArrayInputStream(
                ("test: \n"
                 + "  hostname: test\n"
                 + "  description: a description\n"
                 + "  tags: a, b, c\n"
                 + "  osArch: x86_64\n"
                 + "  osFamily: unix\n"
                 + "  osVersion: 10.6.5\n"
                 + "  osName: Mac OS X\n"
                 + "  username: a user\n"
                 //following should be ignored
                 + "  nodename: bill\n"
                 + "  attributes: test\n"
                 + "  settings: test2\n"
                 + "  type: blahtype\n"
                 + "  frameworkProject: ignored\n").getBytes());

            NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
            nodesYamlParser.parse();
            assertEquals(1, recv.nodes.size());
            assertTrue(recv.nodes.containsKey("test"));
            INodeEntry entry = recv.nodes.get("test");
            assertNotNull(entry);
            assertEquals("test", entry.getNodename());
            assertEquals("test", entry.getHostname());
            assertNotNull(entry.getTags());
            assertEquals(3,entry.getTags().size());
            assertTrue(entry.getTags().contains("a"));
            assertTrue(entry.getTags().contains("b"));
            assertTrue(entry.getTags().contains("c"));
            assertEquals("x86_64", entry.getOsArch());
            assertEquals("unix", entry.getOsFamily());
            assertEquals("10.6.5", entry.getOsVersion());
            assertEquals("Mac OS X", entry.getOsName());
            assertEquals("a description",entry.getDescription());
            assertEquals("a user", entry.getUsername());
            //null values should be ignored
            assertNotNull(entry.getAttributes());
            assertNull(entry.getFrameworkProject());
        }
        {
            //test tags receiving Sequence data explicitly
            testReceiver recv = new testReceiver();
            ByteArrayInputStream is = new ByteArrayInputStream(
                ("test: \n"
                 + "  nodename: bill\n"
                 + "  hostname: test\n"
                 + "  tags: [ a, b, c ]\n"
                ).getBytes());

            NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
            nodesYamlParser.parse();
            assertEquals(1, recv.nodes.size());
            assertTrue(recv.nodes.containsKey("test"));
            INodeEntry entry = recv.nodes.get("test");
            assertNotNull(entry);
            assertEquals("test", entry.getNodename());
            assertEquals("test", entry.getHostname());
            assertNotNull(entry.getTags());
            assertEquals(3,entry.getTags().size());
            assertTrue(entry.getTags().contains("a"));
            assertTrue(entry.getTags().contains("b"));
            assertTrue(entry.getTags().contains("c"));
        }
        {
            //test flow style data
            testReceiver recv = new testReceiver();
            ByteArrayInputStream is = new ByteArrayInputStream(
                ("ubuntu: {description: \"Ubuntu server node\", hostname: \"192.168.1.101\","
                 + " osFamily: \"unix\", osName: \"Linux\", username: \"demo\", tags: \"dev\"}").getBytes());

            NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
            nodesYamlParser.parse();
            assertEquals(1, recv.nodes.size());
            assertTrue(recv.nodes.containsKey("ubuntu"));
            INodeEntry entry = recv.nodes.get("ubuntu");
            assertNotNull(entry);
            assertEquals("ubuntu", entry.getNodename());
            assertEquals("192.168.1.101", entry.getHostname());
            assertNotNull(entry.getTags());
            assertEquals(1, entry.getTags().size());
            assertTrue(entry.getTags().contains("dev"));
            assertEquals("Ubuntu server node", entry.getDescription());
            assertEquals("unix", entry.getOsFamily());
            assertEquals("Linux", entry.getOsName());
            assertEquals("demo", entry.getUsername());
        }

    }
    public void testParseAnyAttribute() throws Exception{

        {
            //test flow style data
            testReceiver recv = new testReceiver();
            ByteArrayInputStream is = new ByteArrayInputStream(
                ("test: \n"
                 + "  nodename: bill\n"
                 + "  hostname: test\n"
                 + "  tags: [ a, b, c ]\n"
                 + "  my-attribute: some value\n"
                ).getBytes());

            NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
            nodesYamlParser.parse();
            assertEquals(1, recv.nodes.size());
            assertTrue(recv.nodes.containsKey("test"));
            INodeEntry entry = recv.nodes.get("test");
            assertNotNull(entry);
            assertEquals("test", entry.getNodename());
            assertNotNull(entry.getAttributes());
            assertNotNull(entry.getAttributes().get("my-attribute"));
            assertEquals("some value", entry.getAttributes().get("my-attribute"));
        }

    }
    public void testShouldReadEditUrls() throws Exception{

        {
            testReceiver recv = new testReceiver();
            ByteArrayInputStream is = new ByteArrayInputStream(
                ("test: \n"
                 + "  hostname: test\n"
                 + "  description: a description\n"
                 + "  tags: a, b, c\n"
                 + "  osArch: x86_64\n"
                 + "  osFamily: unix\n"
                 + "  osVersion: 10.6.5\n"
                 + "  osName: Mac OS X\n"
                 + "  username: a user\n"
                 + "  editUrl: http://a.com/url\n"
                 + "  remoteUrl: http://b.com/aurl\n").getBytes());

            NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
            nodesYamlParser.parse();
            assertEquals(1, recv.nodes.size());
            assertTrue(recv.nodes.containsKey("test"));
            INodeEntry entry = recv.nodes.get("test");
            assertNotNull(entry);
            assertEquals("test", entry.getNodename());
            assertEquals("test", entry.getHostname());
            assertNotNull(entry.getTags());
            assertEquals(3, entry.getTags().size());
            assertTrue(entry.getTags().contains("a"));
            assertTrue(entry.getTags().contains("b"));
            assertTrue(entry.getTags().contains("c"));
            assertEquals("x86_64", entry.getOsArch());
            assertEquals("unix", entry.getOsFamily());
            assertEquals("10.6.5", entry.getOsVersion());
            assertEquals("Mac OS X", entry.getOsName());
            assertEquals("a description", entry.getDescription());
            assertEquals("a user", entry.getUsername());
            //null values should be ignored
            assertNull(entry.getFrameworkProject());

            assertNotNull(entry.getAttributes());
            assertNotNull(entry.getAttributes().get("editUrl"));
            assertNotNull(entry.getAttributes().get("remoteUrl"));
            assertEquals("http://a.com/url",entry.getAttributes().get("editUrl"));
            assertEquals("http://b.com/aurl",entry.getAttributes().get("remoteUrl"));
        }
    }
    public void testParseInvalid_require_nodename() throws Exception {
            //no nodename value
            testReceiver recv = new testReceiver();
            ByteArrayInputStream is = new ByteArrayInputStream(
                "- \n  hostname: bill\n  blah: test\n".getBytes());

            NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
            try {
                nodesYamlParser.parse();
                fail("parsing should fail");
            } catch (NodeFileParserException e) {
                assertTrue(e.getCause() instanceof IllegalArgumentException);
                assertEquals("Required property 'nodename' was not specified", e.getCause().getMessage());
            }

        }
    public void testParseInvalid_allow_missing_hostname() throws Exception {
        //allow no hostname value
        testReceiver recv = new testReceiver();
        ByteArrayInputStream is = new ByteArrayInputStream(
            "bill: \n  nodename: bill\n  blah: test\n".getBytes());

        NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
        nodesYamlParser.parse();
        Assert.assertEquals(1, recv.nodes.size());
        Assert.assertNotNull(recv.nodes.get("bill"));
        Assert.assertEquals("bill", recv.nodes.get("bill").getNodename());
        Assert.assertEquals(null, recv.nodes.get("bill").getHostname());

    }
    public void testParseInvalid_unexpecteddatatype() throws Exception {
            //unexpected data type for string field
            testReceiver recv = new testReceiver();
            ByteArrayInputStream is = new ByteArrayInputStream(
                ("test: \n"
                 + "  nodename: bill\n"
                 + "  hostname: test\n"
                 + "  username: {test:value, a:b}\n").getBytes());

            NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
            try {
                nodesYamlParser.parse();
                fail("parsing should fail: " + recv.nodes);
            } catch (NodeFileParserException e) {
                assertTrue(e.getCause() instanceof YAMLException);
            }

        }
    public void testParseInvalid_no_javaclass() throws Exception {
            //don't allow arbitrary java class
            testReceiver recv = new testReceiver();
            ByteArrayInputStream is = new ByteArrayInputStream(
                ("test: \n"
                 + "  nodename: bill\n"
                 + "  hostname: test\n"
                 + "  username: !!java.io.File [woops.txt]\n").getBytes());

            NodesYamlParser nodesYamlParser = new NodesYamlParser(is, recv);
            try {
                nodesYamlParser.parse();
                fail("parsing should fail: " + recv.nodes);
            } catch (NodeFileParserException e) {
                assertTrue(e.getCause() instanceof YAMLException);
            }

    }
    public static class testReceiver implements NodeReceiver{
        HashMap<String,INodeEntry> nodes = new HashMap<String, INodeEntry>();
        public void putNode(final INodeEntry iNodeEntry) {
            nodes.put(iNodeEntry.getNodename(), iNodeEntry);
        }
    }
}