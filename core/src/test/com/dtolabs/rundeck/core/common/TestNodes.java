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
* TestNodes.java
* 
* User: greg
* Created: Apr 1, 2008 9:45:54 AM
* $Id$
*/


import com.dtolabs.rundeck.core.utils.NodeSet;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;


public class TestNodes extends AbstractBaseTest {
    Nodes nodes;
    File xmlfile1;
    File xmlfile2;
    static String PROJECT_NAME = "TestNodes";

    public TestNodes(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestNodes.class);
    }

    protected void setUp() {
        super.setUp();
        xmlfile1 = new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        xmlfile2 = new File("src/test/com/dtolabs/rundeck/core/common/test-nodes2.xml");
    }

    protected void tearDown() throws Exception {
        getFrameworkInstance().getFrameworkProjectMgr().getFrameworkProject(PROJECT_NAME).getBaseDir().delete();
        getFrameworkInstance().getFrameworkProjectMgr().remove(PROJECT_NAME);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testNodesXML() throws Exception {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                new File(getFrameworkProjectsBase()),
                getFrameworkInstance().getFrameworkProjectMgr());
        nodes = new Nodes(project, xmlfile1, Nodes.Format.projectxml);
        assertNotNull(nodes);
        assertEquals("incorrect number of parsed nodes: " + nodes.listNodes().size(), 2, nodes.listNodes().size());
        assertTrue("nodes did not parse testnode1", nodes.hasNode("testnode1"));
        assertTrue("nodes did not parse testnode2", nodes.hasNode("testnode2"));
        assertFalse("nodes should not parse testnode3", nodes.hasNode("testnode3"));
        {
            INodeEntry node1 = nodes.getNode("testnode1");
            assertNotNull(node1);
            assertEquals("testnode1", node1.getNodename());
            assertEquals("host1.local", node1.getHostname());
            assertEquals("i386", node1.getOsArch());
            assertEquals("unix", node1.getOsFamily());
            assertEquals("Mac OS X", node1.getOsName());
            assertEquals("10.5.1", node1.getOsVersion());
            assertEquals("SubNode", node1.getType());
            assertEquals("username1", node1.getUsername());
            assertEquals("tags were incorrect size for testnode1: " + node1.getTags().size(),
                    2,
                    node1.getTags().size());
            assertTrue("tags for testnode1 did not contain boring", node1.getTags().contains("boring"));
            assertTrue("tags for testnode1 did not contain priority1", node1.getTags().contains("priority1"));
            assertFalse("tags for testnode1 should not contain priority2", node1.getTags().contains("priority2"));
        }
        {
            INodeEntry node1 = nodes.getNode("testnode2");
            assertNotNull(node1);
            assertEquals("testnode2", node1.getNodename());
            assertEquals("testnode2", node1.getHostname());
            assertEquals("x86", node1.getOsArch());
            assertEquals("windows", node1.getOsFamily());
            assertEquals("Windows XP", node1.getOsName());
            assertEquals("5.1", node1.getOsVersion());
            assertEquals("Node", node1.getType());
            assertNull( node1.getUsername());
            assertEquals("tags were incorrect size for testnode1: " + node1.getTags().size(),
                    1,
                    node1.getTags().size());
            assertTrue("tags for testnode1 did not contain boring", node1.getTags().contains("boring"));
            assertFalse("tags for testnode1 should not contain priority1", node1.getTags().contains("priority1"));
            assertFalse("tags for testnode1 should not contain priority2", node1.getTags().contains("priority2"));
        }
        {
            INodeEntry node1 = nodes.getNode("testnode3");
            assertNull(node1);
        }
    }


    public void testFilterNodesXML() throws Exception {
        FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
            new File(getFrameworkProjectsBase()),
            getFrameworkInstance().getFrameworkProjectMgr());
        nodes = new Nodes(project, xmlfile1, Nodes.Format.projectxml);

        assertNotNull(nodes);
        ArrayList nodeDescs = new ArrayList();
        INodeBase nodeDesc1 = new NodeEntryImpl("host1.local", "testnode1");
        INodeBase nodeDesc2 = new NodeEntryImpl("testnode2", "testnode2");
        nodeDescs.add(nodeDesc1);
        nodeDescs.add(nodeDesc2);
        {
            NodeSet nset = new NodeSet();
            nset.createInclude();
            nset.createExclude().setName("testnode1");

            Collection result = nodes.filterNodes(nodeDescs, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertFalse("contains incorrect node desc", result.contains(nodeDesc1));
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc2));
        }
        {
            NodeSet nset = new NodeSet();
            nset.createInclude().setName("testnode1");
            nset.createExclude();

            Collection result = nodes.filterNodes(nodeDescs, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc1));
            assertFalse("contains incorrect node desc", result.contains(nodeDesc2));
        }
        //hostname
        {
            NodeSet nset = new NodeSet();
            nset.createInclude().setHostname("host1.local");
            nset.createExclude();

            Collection result = nodes.filterNodes(nodeDescs, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc1));
            assertFalse("contains incorrect node desc", result.contains(nodeDesc2));
        }
        {
            NodeSet nset = new NodeSet();
            nset.createInclude();
            nset.createExclude().setHostname("host1.local");

            Collection result = nodes.filterNodes(nodeDescs, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertFalse("contains incorrect node desc", result.contains(nodeDesc1));
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc2));
        }
        {
            NodeSet nset = new NodeSet();
            nset.createInclude().setHostname("testnode2");
            nset.createExclude();

            Collection result = nodes.filterNodes(nodeDescs, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertFalse("contains incorrect node desc", result.contains(nodeDesc1));
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc2));
        }
        {
            NodeSet nset = new NodeSet();
            nset.createInclude();
            nset.createExclude().setHostname("testnode2");

            Collection result = nodes.filterNodes(nodeDescs, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc1));
            assertFalse("contains incorrect node desc", result.contains(nodeDesc2));
        }
        //hostname regex

        {
            NodeSet nset = new NodeSet();
            nset.createInclude();
            nset.createExclude().setHostname("test.*");

            Collection result = nodes.filterNodes(nodeDescs, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc1));
            assertFalse("contains incorrect node desc", result.contains(nodeDesc2));
        }
        {
            NodeSet nset = new NodeSet();
            nset.createInclude().setHostname("test.*");
            nset.createExclude();

            Collection result = nodes.filterNodes(nodeDescs, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertFalse("contains incorrect node desc", result.contains(nodeDesc1));
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc2));
        }
        {
            NodeSet nset = new NodeSet();
            nset.createInclude();
            nset.createExclude().setHostname("host1\\..*");

            Collection result = nodes.filterNodes(nodeDescs, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertFalse("contains incorrect node desc", result.contains(nodeDesc1));
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc2));
        }
        {
            NodeSet nset = new NodeSet();
            nset.createInclude().setHostname("host1\\..*");
            nset.createExclude();

            Collection result = nodes.filterNodes(nodeDescs, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc1));
            assertFalse("contains incorrect node desc", result.contains(nodeDesc2));
        }
        project = FrameworkProject.create(PROJECT_NAME,
            new File(getFrameworkProjectsBase()),
            getFrameworkInstance().getFrameworkProjectMgr());
        //test multipl filters
        Nodes nodes2 = new Nodes(project, xmlfile2, Nodes.Format.projectxml);
        assertNotNull(nodes2);
        ArrayList nodeDescs2 = new ArrayList();
        INodeBase nodeDesc3 = new NodeEntryImpl("testnode3", "testnode3");
        nodeDescs2.add(nodeDesc1);
        nodeDescs2.add(nodeDesc2);
        nodeDescs2.add(nodeDesc3);

        {
            NodeSet nset = new NodeSet();
            nset.createExclude();
            nset.createInclude().setHostname(".*");
            nset.getInclude().setName("testnode[2-3]");

            Collection result = nodes2.filterNodes(nodeDescs2, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 2, result.size());
            assertFalse("doesn't contain correct node desc", result.contains(nodeDesc1));
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc3));
            assertTrue("contains incorrect node desc", result.contains(nodeDesc2));
        }

        {
            NodeSet nset = new NodeSet();
            nset.createExclude();
            nset.createInclude().setHostname(".*.local");
            nset.getInclude().setName("testnode1");

            Collection result = nodes2.filterNodes(nodeDescs2, nset);
            assertNotNull(result);
            assertEquals("incorrect size", 1, result.size());
            assertFalse("doesn't contain correct node desc", result.contains(nodeDesc3));
            assertTrue("contains incorrect node desc", result.contains(nodeDesc1));
            assertFalse("contains incorrect node desc", result.contains(nodeDesc2));
        }

    }


    public void testGetNodeByHostnameXML() throws Exception {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
            new File(getFrameworkProjectsBase()),
            getFrameworkInstance().getFrameworkProjectMgr());
        nodes = new Nodes(project, xmlfile1, Nodes.Format.projectxml);

        INodeEntry node1 = nodes.getNodeByHostname("host1.local");
        assertNotNull(node1);
        assertEquals("incorrect name", "testnode1", node1.getNodename());

        INodeEntry node2 = nodes.getNodeByHostname("testnode2");
        assertNotNull(node2);
        assertEquals("incorrect name", "testnode2", node2.getNodename());

        assertNull(nodes.getNodeByHostname("testnode1"));
    }


    public void testHasNodeByHostnameXML() throws Exception {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                new File(getFrameworkProjectsBase()),
                getFrameworkInstance().getFrameworkProjectMgr());
        nodes = new Nodes(project, xmlfile1, Nodes.Format.projectxml);
        assertTrue(nodes.hasNodeByHostname("host1.local"));
        assertTrue(nodes.hasNodeByHostname("testnode2"));
        assertFalse(nodes.hasNodeByHostname("testnode1"));
    }

    public void testGetNodeXML() throws Exception {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                new File(getFrameworkProjectsBase()),
                getFrameworkInstance().getFrameworkProjectMgr());
        nodes = new Nodes(project, xmlfile1, Nodes.Format.projectxml);

        INodeEntry node1 = nodes.getNode("testnode1");
        assertNotNull(node1);
        assertEquals("incorrect name", "testnode1", node1.getNodename());

        INodeEntry node2 = nodes.getNode("testnode2");
        assertNotNull(node2);
        assertEquals("incorrect name", "testnode2", node2.getNodename());

        assertNull(nodes.getNode("host1.local"));
    }

    public void testHasNodeXML() throws Exception {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                new File(getFrameworkProjectsBase()),
                getFrameworkInstance().getFrameworkProjectMgr());
        nodes = new Nodes(project, xmlfile1, Nodes.Format.projectxml);
        assertTrue(nodes.hasNode("testnode1"));
        assertTrue(nodes.hasNode("testnode2"));
        assertFalse(nodes.hasNode("host1.local"));
    }

    public void testGetNodeEntriesXML() throws Exception {
        FrameworkProject project = FrameworkProject.create(PROJECT_NAME,
                new File(getFrameworkProjectsBase()),
                getFrameworkInstance().getFrameworkProjectMgr());
        nodes = new Nodes(project, xmlfile1, Nodes.Format.projectxml);

        assertNotNull(nodes);
        final ArrayList<INodeBase> nodelist = new ArrayList<INodeBase>();
        final INodeBase nodeDesc1 = NodeBaseImpl.create("testnode1");
        final INodeBase nodeDesc2 = NodeBaseImpl.create("testnode2");
        nodelist.add(nodeDesc1);
        nodelist.add(nodeDesc2);
        {
            final Collection<INodeEntry> result = nodes.getNodeEntries(nodelist);
            assertNotNull(result);
            assertEquals("incorrect size", 2, result.size());
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc1));
            assertTrue("doesn't contain correct node desc", result.contains(nodeDesc2));
            INodeEntry[] entries = result.toArray(new INodeEntry[2]);
            assertEquals("Incorrect entry", "testnode1", entries[0].getNodename());
            assertEquals("Incorrect entry", "testnode2", entries[1].getNodename());
        }
    }
}
