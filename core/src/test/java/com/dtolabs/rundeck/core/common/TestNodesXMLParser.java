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
/*
* TestNodesXMLParser.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jun 17, 2010 3:19:07 PM
* $Id$
*/

import com.dtolabs.shared.resources.ResourceXMLConstants;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class TestNodesXMLParser extends TestCase {
    NodesXMLParser nodesXMLParser;
    File xmlfile1;
    File xmlfile2;
    File xmlfile3;
    File dneFile1;

    public TestNodesXMLParser(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestNodesXMLParser.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        xmlfile1 = new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        xmlfile2 = new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes2.xml");
        xmlfile3 = new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes3.xml");
        dneFile1 = new File("src/test/resources/com/dtolabs/rundeck/core/common/DNE-file.xml");
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    class nodeReceiver implements NodeReceiver {
        HashMap<String, INodeEntry> map = new HashMap<String, INodeEntry>();

        public void putNode(final INodeEntry iNodeEntry) {
            map.put(iNodeEntry.getNodename(), iNodeEntry);
        }
    }

    public void testParse() throws Exception {

        try {
            new NodesXMLParser(dneFile1, null).parse();
            fail("Should have thrown an Exception");
        }
        catch (NodeFileParserException ex) {
        }
        {
            final nodeReceiver receiver = new nodeReceiver();
            nodesXMLParser = new NodesXMLParser(xmlfile1, receiver);
            nodesXMLParser.parse();
            assertEquals("wrong number of nodes parsed", 2, receiver.map.size());
            INodeEntry node1 = receiver.map.get("test1");
            assertNotNull(node1);
            assertEquals("test1", node1.getNodename());
            assertEquals("this is the test1 node", node1.getDescription());
            assertNotNull(node1.getTags());
            assertEquals(new HashSet<String>(Arrays.asList("boring", "priority1")), node1.getTags());
            assertEquals("host1.local", node1.getHostname());
            assertEquals("i386", node1.getOsArch());
            assertEquals("unix", node1.getOsFamily());
            assertEquals("Mac OS X", node1.getOsName());
            assertEquals("10.5.1", node1.getOsVersion());
            assertEquals("username1", node1.getUsername());
            assertNull(node1.getFrameworkProject());
            INodeEntry node2 = receiver.map.get("testnode2");
            assertNotNull(node2);
            assertEquals("testnode2", node2.getNodename());
            assertEquals("registered Node asdf", node2.getDescription());
            assertNotNull(node2.getTags());
            assertEquals(new HashSet<String>(Arrays.asList("boring")), node2.getTags());
            assertEquals("testnode2", node2.getHostname());
            assertEquals("x86", node2.getOsArch());
            assertEquals("windows", node2.getOsFamily());
            assertEquals("Windows XP", node2.getOsName());
            assertEquals("5.1", node2.getOsVersion());
            assertNull( node2.getUsername());
            assertNull(node2.getFrameworkProject());

        }
        {
            final nodeReceiver receiver = new nodeReceiver();
            nodesXMLParser = new NodesXMLParser(xmlfile2, receiver);
            nodesXMLParser.parse();
            assertEquals("wrong number of nodes parsed", 3, receiver.map.size());
            INodeEntry node1 = receiver.map.get("testnode3");
            assertEquals("testnode3", node1.getNodename());
            assertEquals("This is the third test node", node1.getDescription());
            assertNotNull(node1.getTags());
            assertEquals(new HashSet<String>(Arrays.asList("priority1", "elf")), node1.getTags());
            assertEquals("testnode3.local", node1.getHostname());
            assertEquals("intel", node1.getOsArch());
            assertEquals("solaris", node1.getOsFamily());
            assertEquals("Solaris Something", node1.getOsName());
            assertEquals("3.7", node1.getOsVersion());
            assertNull("username1", node1.getUsername());
            assertNull(node1.getFrameworkProject());
            //test tags="" is empty
            INodeEntry node2 = receiver.map.get("testnode2");
            assertNotNull(node2.getTags());
            assertEquals(0, node2.getTags().size());
            assertNull(node2.getFrameworkProject());
        }
    }
    public void testParseEditRemoteUrl() throws Exception {

        {
            final nodeReceiver receiver = new nodeReceiver();
            nodesXMLParser = new NodesXMLParser(xmlfile3, receiver);
            nodesXMLParser.parse();
            assertEquals("wrong number of nodes parsed", 3, receiver.map.size());

            INodeEntry node0 = receiver.map.get("test1");
            assertNotNull(node0.getAttributes());
            assertEquals("TestEditUrl1", node0.getAttributes().get(ResourceXMLConstants.NODE_EDIT_URL));
            assertNull(node0.getAttributes().get(ResourceXMLConstants.NODE_REMOTE_URL));

            INodeEntry node1 = receiver.map.get("testnode2");
            assertNotNull(node1.getAttributes());
            assertEquals("TestRemoteUrl2", node1.getAttributes().get(ResourceXMLConstants.NODE_REMOTE_URL));
            assertNull(node1.getAttributes().get(ResourceXMLConstants.NODE_EDIT_URL));

            INodeEntry node2 = receiver.map.get("testnode3");
            assertNotNull(node2.getAttributes());
            assertEquals("TestEditUrl3", node2.getAttributes().get(ResourceXMLConstants.NODE_EDIT_URL));
            assertEquals("TestRemoteUrl3", node2.getAttributes().get(ResourceXMLConstants.NODE_REMOTE_URL));
        }
    }
}