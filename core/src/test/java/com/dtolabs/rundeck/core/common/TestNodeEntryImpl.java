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

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;

import java.util.List;
import java.util.Map;

/**
 * Tests the INodeEntry implementation
 */
public class TestNodeEntryImpl extends AbstractBaseTest {
    final private String fwkNodeHostname;
    final private String fwkNodeName;

    public TestNodeEntryImpl(String name) {
        super(name);
        super.setUp();
        fwkNodeHostname = getFrameworkInstance().getFrameworkNodeHostname();
        fwkNodeName = getFrameworkInstance().getFrameworkNodeName();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestNodeEntryImpl.class);
    }

    public void testGetHostname() {
        final INodeDesc node = NodeEntryImpl.create("hoho", "hoho");
        assertEquals("nodename did not match", "hoho", node.getHostname());
        final INodeDesc userAtNode = NodeEntryImpl.create("user@hoho", "hoho");
        assertEquals("nodename did not match", "user@hoho", userAtNode.getHostname());
    }

    public void testCreate() {
        final INodeDesc node = NodeEntryImpl.create("hoho", "hoho");
        assertEquals("nodename did not match", "hoho", node.getHostname());
        assertEquals("nodename did not match", "hoho", node.getNodename());
        final INodeDesc userAtNode = NodeEntryImpl.create("user@hoho", "hoho");
        assertEquals("nodename did not match", "user@hoho", userAtNode.getHostname());
        assertEquals("nodename did not match", "hoho", userAtNode.getNodename());
    }

    public void testIsLocalHostname() {
        final INodeDesc node = NodeEntryImpl.create(fwkNodeHostname, fwkNodeName);
        assertTrue("isLocal failed when it was local", getFrameworkInstance().isLocalNode(node));

        final String localNode = "user@" + fwkNodeHostname;
        final INodeDesc node2 = NodeEntryImpl.create(localNode, fwkNodeHostname);
        assertTrue("isLocal failed when it was local but had an embedded username", getFrameworkInstance().isLocalNode(
            node2));
    }


    public void testEquals() {
        //equality is based on node name, not hostname
        final INodeDesc n1 = NodeEntryImpl.create("n1", "n1");
        final INodeDesc n2 = NodeEntryImpl.create("n1", "n1");
        assertTrue("n1 did not equal n2.", n1.equals(n2));
        final INodeDesc n3 = NodeEntryImpl.create("n2", "n2");
        assertFalse("n3 should not equal n1", n3.equals(n1));
        final INodeDesc n4 = NodeEntryImpl.create("n3", "n2");
        assertTrue("n3 should  equal n4", n3.equals(n4));
    }

    public void testNodeBaseEquals() {
        //equality is based on node name, not hostname
        final INodeBase n1 = NodeBaseImpl.create("n1");
        final INodeBase n2 = NodeBaseImpl.create("n1");
        assertTrue("n1 did not equal n2.", n1.equals(n2));
        final INodeBase n3 = NodeBaseImpl.create("n2");
        assertFalse("n3 should not equal n1", n3.equals(n1));
        assertFalse("n3 should not equal n2", n3.equals(n2));
    }

    public void testContainsUsername() {
        NodeEntryImpl node = (NodeEntryImpl) NodeEntryImpl.create("user@foo", "foo");
        assertTrue("didn't detect embedded user name", node.containsUserName());
    }

    public void testExtractUser() {
        NodeEntryImpl node = (NodeEntryImpl) NodeEntryImpl.create("user@host", "host");
        String extracted = node.extractUserName();
        assertEquals("did not correctly extract user: '" + extracted + "'", "user", extracted);

        node = (NodeEntryImpl) NodeEntryImpl.create("host", "name");
        try {
            extracted = node.extractUserName();
            assertNull("unexected result: " + extracted, extracted);
        } catch (IllegalArgumentException e) {
            fail("should not fail");
        }

        node = (NodeEntryImpl) NodeEntryImpl.create("@host", "name");
        try {
            extracted = node.extractUserName();
            assertNull("unexected result: " + extracted, extracted);
        } catch (IllegalArgumentException e) {
            fail("should not fail");
        }

        node = (NodeEntryImpl) NodeEntryImpl.create("host", "name");
        node.setUsername("user");
        try {
            extracted = node.extractUserName();
            assertEquals("unexected result: " + extracted, "user", extracted);
        } catch (IllegalArgumentException e) {
            fail("should not fail");
        }

    }

    public void testExtractHost() {
        NodeEntryImpl node = (NodeEntryImpl) NodeEntryImpl.create("user@host", "host");
        String extracted = node.extractHostname();
        assertEquals("did not correctly extract host: '" + extracted + "'", "host", extracted);

        node = (NodeEntryImpl) NodeEntryImpl.create("host", "name");
        extracted = node.extractHostname();
        assertEquals("did not correctly extract user: '" + extracted + "'", "host", extracted);

        node = (NodeEntryImpl) NodeEntryImpl.create("host:22", "name");
        extracted = node.extractHostname();
        assertEquals("did not correctly extract user: '" + extracted + "'", "host", extracted);
    }

    public void testContainsPort() {
        assertTrue("did not correctly parse port", NodeEntryImpl.containsPort("host:22"));
        assertFalse("incorrectly parsed a port when it did not exist", NodeEntryImpl.containsPort("host"));
        assertTrue("did not correctly parse port", NodeEntryImpl.containsPort("user@host:22"));
        assertFalse("did not correctly parse port", NodeEntryImpl.containsPort("host:garbage"));

    }

    public void testExtractPort() {
        NodeEntryImpl node = (NodeEntryImpl) NodeEntryImpl.create("host:22", "host");
        String extracted = node.extractPort();
        assertEquals("did not correctly extract port: '" + extracted + "'", "22", extracted);

        node = (NodeEntryImpl) NodeEntryImpl.create("user@host:666", "name");
        extracted = node.extractPort();
        assertEquals("did not correctly extract user: '" + extracted + "'", "666", extracted);

        node = (NodeEntryImpl) NodeEntryImpl.create("host:666", "name");
        extracted = node.extractPort();
        assertEquals("did not correctly extract user: '" + extracted + "'", "666", extracted);

        node = (NodeEntryImpl) NodeEntryImpl.create("host", "name");
        extracted = node.extractPort();
        assertNull("did not correctly extract user: '" + extracted + "'", extracted);
    }

    public void testNodeNamespacedAttrs(){
        NodeEntryImpl node = new NodeEntryImpl("name");
        node.setAttribute("abc", "def1");
        node.setAttribute("xyz:abc", "def2");
        node.setAttribute("xyz:123", "def3");
        node.setAttribute(":123", "def4");
        node.setAttribute("  :456", "def5");
        node.setAttribute(":zyx:blah:", "def6");

        Map<String,Map<String,List<String>>> attrs = NodeEntryImpl.nodeNamespacedAttributes(node);
        Assert.assertNotNull(attrs);
        Assert.assertNotNull(attrs.get(""));
        Assert.assertNotNull(attrs.get("xyz"));

        Assert.assertEquals(2, attrs.get("xyz").size());
        Assert.assertEquals("xyz:abc", attrs.get("xyz").get("abc").get(0));
        Assert.assertEquals("def2", attrs.get("xyz").get("abc").get(1));
        Assert.assertEquals("xyz:123", attrs.get("xyz").get("123").get(0));
        Assert.assertEquals("def3", attrs.get("xyz").get("123").get(1));

        Assert.assertEquals(4, attrs.get("").size());
        Assert.assertEquals("abc", attrs.get("").get("abc").get(0));
        Assert.assertEquals("def1", attrs.get("").get("abc").get(1));
        Assert.assertEquals(":123", attrs.get("").get("123").get(0));
        Assert.assertEquals("def4", attrs.get("").get("123").get(1));
        Assert.assertEquals("  :456", attrs.get("").get("456").get(0));
        Assert.assertEquals("def5", attrs.get("").get("456").get(1));
        Assert.assertEquals(":zyx:blah:", attrs.get("").get("zyx:blah:").get(0));
        Assert.assertEquals("def6", attrs.get("").get("zyx:blah:").get(1));

    }
}
