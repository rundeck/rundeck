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

/*
* TestNodesYamlRoundtrip.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 20, 2011 9:58:14 AM
*
*/
package com.dtolabs.rundeck.core.common;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 * TestNodesYamlRoundtrip is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestNodesYamlRoundtrip extends TestCase {

    public TestNodesYamlRoundtrip(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestNodesYamlRoundtrip.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    public void testRoundtripObjToObj() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NodesYamlGenerator gen = new NodesYamlGenerator(baos);

        {
            NodeEntryImpl node = new NodeEntryImpl();
            node.setNodename("test1");
            node.setHostname("testhostname");
            gen.addNode(node);
            gen.generate();

            testReceiver recv = new testReceiver();
            final byte[] bytes = baos.toByteArray();
            System.err.println("serial: " + baos.toString());
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            NodesYamlParser pars = new NodesYamlParser(is, recv);
            pars.parse();

            assertEquals(1, recv.nodes.size());
            final INodeEntry entry = recv.nodes.get("test1");
            assertNotNull(entry);
            assertEquals("test1", entry.getNodename());
            assertEquals("testhostname", entry.getHostname());
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
            NodeEntryImpl node = new NodeEntryImpl();
            node.setNodename("test1");
            node.setHostname("testhostname");
            node.setOsFamily("unix");
            node.setOsName("Mac OS X");
            node.setOsVersion("10.6.5");
            node.setOsArch("x86_64");
            node.setDescription("a description");
            node.setUsername("some user");
            final HashSet tags = new HashSet();
            tags.add("rundeck");
            tags.add("dev");
            tags.add("ops");
            node.setTags(tags);

            //set properties that should not be serialized: type, frameworkProject, settings, attributes
            node.setFrameworkProject("my project");

            node.getAttributes().put("a", "b");



            gen.addNode(node);
            gen.generate();

            testReceiver recv = new testReceiver();
            final byte[] bytes = baos.toByteArray();
            System.err.println("serial: " + baos.toString());
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            NodesYamlParser pars = new NodesYamlParser(is, recv);
            pars.parse();

            assertEquals(1, recv.nodes.size());
            final INodeEntry entry = recv.nodes.get("test1");
            assertNotNull(entry);
            assertEquals("test1", entry.getNodename());
            assertEquals("testhostname", entry.getHostname());
            assertNotNull(entry.getTags());
            assertEquals(3, entry.getTags().size());
            assertTrue(entry.getTags().contains("dev"));
            assertTrue(entry.getTags().contains("ops"));
            assertTrue(entry.getTags().contains("rundeck"));
            assertEquals("x86_64", entry.getOsArch());
            assertEquals("unix", entry.getOsFamily());
            assertEquals("10.6.5", entry.getOsVersion());
            assertEquals("Mac OS X", entry.getOsName());
            assertEquals("a description", entry.getDescription());
            assertEquals("some user", entry.getUsername());
            //null values should be ignored
            assertNotNull(entry.getAttributes());
            assertNull(entry.getFrameworkProject());
        }

    }

    public static class testReceiver implements NodeReceiver {
        HashMap<String, INodeEntry> nodes = new HashMap<String, INodeEntry>();

        public void putNode(final INodeEntry iNodeEntry) {
            nodes.put(iNodeEntry.getNodename(), iNodeEntry);
        }
    }
}
