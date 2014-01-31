/*
 Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*
 Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.core.common;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.HashSet;

/**
 * $INTERFACE is ... User: greg Date: 1/25/14 Time: 2:53 PM
 */
@RunWith(JUnit4.class)
public class TestMergedAttributesNodeSet {
    @Test
    public void test() {
        MergedAttributesNodeSet merged = new MergedAttributesNodeSet();

        NodeSetImpl nodeSet1 = new NodeSetImpl();
        NodeEntryImpl nodeEntry1 = new NodeEntryImpl("abc");
        nodeEntry1.setAttribute("blahblah", "blah");
        nodeEntry1.setAttribute("wakawaka", "something");
        nodeSet1.putNode(nodeEntry1);
        Assert.assertEquals("blah", nodeEntry1.getAttribute("blahblah"));
        Assert.assertEquals("something", nodeEntry1.getAttribute("wakawaka"));
        Assert.assertEquals(null, nodeEntry1.getAttribute("bloobloo"));

        NodeSetImpl nodeSet2 = new NodeSetImpl();
        NodeEntryImpl nodeEntry2 = new NodeEntryImpl("abc");
        nodeEntry2.setAttribute("bloobloo", "bloo");
        nodeEntry2.setAttribute("wakawaka", "something-else");
        nodeSet2.putNode(nodeEntry2);
        Assert.assertEquals("bloo", nodeEntry2.getAttribute("bloobloo"));
        Assert.assertEquals("something-else", nodeEntry2.getAttribute("wakawaka"));
        Assert.assertEquals(null, nodeEntry2.getAttribute("blahblah"));

        merged.addNodeSet(nodeSet1);
        merged.addNodeSet(nodeSet2);
        INodeEntry abc = merged.getNode("abc");
        Assert.assertNotNull("should not be null",abc);
        Assert.assertNotNull("should not be null",abc.getAttributes());
        Assert.assertEquals("blah",abc.getAttributes().get("blahblah"));
        Assert.assertEquals("bloo",abc.getAttributes().get("bloobloo"));
        Assert.assertEquals("something-else",abc.getAttributes().get("wakawaka"));
    }
    @Test
    public void testTags() {
        MergedAttributesNodeSet merged = new MergedAttributesNodeSet();

        NodeSetImpl nodeSet1 = new NodeSetImpl();
        NodeEntryImpl nodeEntry1 = new NodeEntryImpl("abc");
        nodeEntry1.setAttribute("blahblah", "blah");
        nodeEntry1.setAttribute("wakawaka", "something");
        HashSet tags1 = new HashSet();
        tags1.add("abc");
        tags1.add("123");
        nodeEntry1.setTags(tags1);
        nodeSet1.putNode(nodeEntry1);
        Assert.assertEquals("blah", nodeEntry1.getAttribute("blahblah"));
        Assert.assertEquals("something", nodeEntry1.getAttribute("wakawaka"));
        Assert.assertEquals(null, nodeEntry1.getAttribute("bloobloo"));

        NodeSetImpl nodeSet2 = new NodeSetImpl();
        NodeEntryImpl nodeEntry2 = new NodeEntryImpl("abc");
        nodeEntry2.setAttribute("bloobloo", "bloo");
        nodeEntry2.setAttribute("wakawaka", "something-else");
        HashSet tags2 = new HashSet();
        tags2.add("abc");
        tags2.add("def");
        tags2.add("456");
        nodeEntry2.setTags(tags2);
        nodeSet2.putNode(nodeEntry2);
        Assert.assertEquals("bloo", nodeEntry2.getAttribute("bloobloo"));
        Assert.assertEquals("something-else", nodeEntry2.getAttribute("wakawaka"));
        Assert.assertEquals(null, nodeEntry2.getAttribute("blahblah"));

        merged.addNodeSet(nodeSet1);
        merged.addNodeSet(nodeSet2);
        INodeEntry abc = merged.getNode("abc");
        Assert.assertNotNull("should not be null",abc);
        Assert.assertNotNull("should not be null",abc.getAttributes());
        Assert.assertEquals("blah", abc.getAttributes().get("blahblah"));
        Assert.assertEquals("bloo", abc.getAttributes().get("bloobloo"));
        Assert.assertEquals("something-else",abc.getAttributes().get("wakawaka"));
        Assert.assertNotNull("should not be null", abc.getTags());
        Assert.assertEquals(4, abc.getTags().size());
        Assert.assertTrue(abc.getTags().containsAll(Arrays.asList("abc", "123", "def", "456")));
    }
}
