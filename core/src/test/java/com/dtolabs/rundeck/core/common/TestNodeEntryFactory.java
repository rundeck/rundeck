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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 1/20/14 Time: 2:27 PM
 */
public class TestNodeEntryFactory {
    @Test(expected = IllegalArgumentException.class)
    public void createFromMapMissingNodename() {
        Map<String, Object> stringObjectMap = new HashMap<String, Object>();
//        stringObjectMap.put("nodename", "blah");
        stringObjectMap.put("hostname", "blah");
        NodeEntryImpl node = NodeEntryFactory.createFromMap(stringObjectMap);
    }
    @Test(expected = IllegalArgumentException.class)
    public void createFromMapMissingHostname() {
        Map<String, Object> stringObjectMap = new HashMap<String, Object>();
        stringObjectMap.put("nodename", "blah");
//        stringObjectMap.put("hostname", "blah");
        NodeEntryImpl node = NodeEntryFactory.createFromMap(stringObjectMap);
    }
    @Test()
    public void createFromMapRequiredSet() {
        Map<String, Object> stringObjectMap = new HashMap<String, Object>();
        stringObjectMap.put("nodename", "blah");
        stringObjectMap.put("hostname", "blah2");
        NodeEntryImpl node = NodeEntryFactory.createFromMap(stringObjectMap);
        Assert.assertEquals("blah", node.getNodename());
        Assert.assertEquals("blah2", node.getHostname());
    }
    @Test
    public void createFromMapNullCollectionTags() {
        Map<String, Object> stringObjectMap = new HashMap<String, Object>();

        stringObjectMap.put("nodename", "blah");
        stringObjectMap.put("hostname", "blah");
        stringObjectMap.put("tags", Arrays.asList("abc", "def", null, "ghi"));

        NodeEntryImpl node = NodeEntryFactory.createFromMap(stringObjectMap);

        Assert.assertEquals("blah", node.getNodename());
        Assert.assertEquals(3, node.getTags().size());
        Assert.assertTrue(node.getTags().containsAll(Arrays.asList("abc", "def", "ghi")));
    }
    @Test
    public void createFromMapBlankCollectionTags() {
        Map<String, Object> stringObjectMap = new HashMap<String, Object>();

        stringObjectMap.put("nodename", "blah");
        stringObjectMap.put("hostname", "blah");
        stringObjectMap.put("tags", Arrays.asList("abc", "def", "", "ghi"));

        NodeEntryImpl node = NodeEntryFactory.createFromMap(stringObjectMap);

        Assert.assertEquals("blah", node.getNodename());
        Assert.assertEquals(3, node.getTags().size());
        Assert.assertTrue(node.getTags().containsAll(Arrays.asList("abc", "def", "ghi")));
    }
    @Test
    public void createFromMapBlankStringlistTags() {
        Map<String, Object> stringObjectMap = new HashMap<String, Object>();

        stringObjectMap.put("nodename", "blah");
        stringObjectMap.put("hostname", "blah");
        stringObjectMap.put("tags", "abc,def,,ghi");

        NodeEntryImpl node = NodeEntryFactory.createFromMap(stringObjectMap);

        Assert.assertEquals("blah", node.getNodename());
        Assert.assertEquals(3, node.getTags().size());
        Assert.assertTrue(node.getTags().containsAll(Arrays.asList("abc", "def", "ghi")));
    }
    @Test
    public void createFromMapNonStringValue() {
        Map<String, Object> stringObjectMap = new HashMap<String, Object>();

        stringObjectMap.put("nodename", "blah");
        stringObjectMap.put("hostname", "blah");
        stringObjectMap.put("an-attribute", 123L);

        NodeEntryImpl node = NodeEntryFactory.createFromMap(stringObjectMap);

        Assert.assertEquals("blah", node.getNodename());
        Assert.assertEquals("123", node.getAttribute("an-attribute"));
    }
    @Test
    public void createFromMapNullValue() {
        Map<String, Object> stringObjectMap = new HashMap<String, Object>();

        stringObjectMap.put("nodename", "blah");
        stringObjectMap.put("hostname", "blah");
        stringObjectMap.put("null-attribute", null);

        NodeEntryImpl node = NodeEntryFactory.createFromMap(stringObjectMap);

        Assert.assertEquals("blah", node.getNodename());
        Assert.assertEquals(null, node.getAttribute("null-attribute"));
    }
}
