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

package com.dtolabs.rundeck.core.utils;
/*
* TestNodeSet.java
* 
* User: greg
* Created: Apr 1, 2008 10:09:32 AM
* $Id$
*/


import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.PatternSyntaxException;


public class TestNodeSet extends TestCase {
    NodeSet set;
    NodeEntryImpl nodeimp1;
    NodeEntryImpl nodeimp2;
    NodeEntryImpl nodeimp3;

    public TestNodeSet(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestNodeSet.class);
    }

    protected void setUp() throws Exception {
        nodeimp1 = new NodeEntryImpl("test1.local", "testnode1");
        nodeimp1.setOsArch("x86");
        nodeimp1.setOsFamily("windows");
        nodeimp1.setOsName("Windows NT");
        nodeimp1.setOsVersion("5.1");
        final HashSet tags1 = new HashSet();
        tags1.add("priority1");
        tags1.add("devenv");
        tags1.add("serverbox");
        nodeimp1.setTags(tags1);

        nodeimp2 = new NodeEntryImpl("testnode2", "testnode2");
        nodeimp2.setOsArch("x386");
        nodeimp2.setOsFamily("unix");
        nodeimp2.setOsName("Mac OS X");
        nodeimp2.setOsVersion("10.5.1");
        final HashSet tags2 = new HashSet();
        tags2.add("priority2");
        tags2.add("devenv");
        tags2.add("workstation");
        nodeimp2.setTags(tags2);
        nodeimp2.getAttributes().put("testattribute1", "testvalue1");
        nodeimp2.getAttributes().put("testattribute2", "testvalue2");
        nodeimp2.getAttributes().put("testattribute3", "testvalue3");

        nodeimp3 = new NodeEntryImpl("testnode3.local", "testnode3");
        nodeimp3.setOsArch("intel");
        nodeimp3.setOsFamily("solaris");
        nodeimp3.setOsName("Solaris Something");
        nodeimp3.setOsVersion("3.7");
        final HashSet tags3 = new HashSet();
        tags3.add("priority1");
        tags3.add("workstation");
        nodeimp3.setTags(tags3);
        nodeimp3.getAttributes().put("testattribute1", "testvalue1");
        nodeimp3.getAttributes().put("testattribute2", "testvalue2redux");
        nodeimp3.getAttributes().put("testattribute4", "testvalue5");
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    public static void assertFilterMap(Map<String, String> expectInclude, Map<String, String> expectExclude,
            Map<String, Map<String, String>> result) {
        Map<String, String> resultInclude = result.get("include");
        Map<String, String> resultExclude = result.get("exclude");

        assertNotNull(resultInclude);
        assertNotNull(resultExclude);
        if(null!=expectInclude){
            assertEquals("wrong include size: "+ expectInclude,expectInclude.size(), resultInclude.size());
            for (String s : expectInclude.keySet()) {
                assertNotNull("expected non-null for "+s + "( "+result+" )", resultInclude.get(s));
                assertEquals("expected value for " + s +": "+ expectInclude.get(s) + "( " + result + " )", expectInclude.get(s), resultInclude.get(s));
            }
        }else{
            assertEquals(0, resultInclude.size());
        }
        if (null != expectExclude) {
            assertEquals("wrong exclude size: " + expectExclude,expectExclude.size(), resultExclude.size());
            for (String s : expectExclude.keySet()) {
                assertNotNull("expected non-null for " + s + "( " + result + " )", resultExclude.get(s));
                assertEquals("expected value for " + s + ": " + expectExclude.get(s) + "( " + result + " )", expectExclude.get(s),
                        resultExclude.get(s));
            }
        } else {
            assertEquals(0, resultExclude.size());
        }

    }
    public void testParseFilterBasic() {
        HashMap<String, String> expect = new HashMap<String, String>();
        expect.put("tags", "xyz");
        assertFilterMap(expect, null, NodeSet.parseFilter("tags: xyz"));
    }
    public void testParseFilterDefaultKey() {
        HashMap<String, String> expect = new HashMap<String, String>();
        expect.put("name", "xyz");
        assertFilterMap(expect, null, NodeSet.parseFilter("xyz"));
    }
    public void testParseFilterJoinMultiDefaultKey() {
        HashMap<String, String> expect = new HashMap<String, String>();
        expect.put("name", "xyz,abc");
        assertFilterMap(expect, null, NodeSet.parseFilter("xyz abc"));
    }
    public void testParseFilterJoinMultiKey() {
        HashMap<String, String> expect = new HashMap<String, String>();
        expect.put("tags", "xyz,abc");
        assertFilterMap(expect, null, NodeSet.parseFilter("tags:xyz tags:abc"));
    }
    public void testParseFilterMultiInclude() {
        HashMap<String, String> expect = new HashMap<String, String>();
        expect.put("tags", "xyz");
        expect.put("name", "monkey toe");
        assertFilterMap(expect, null, NodeSet.parseFilter("tags: xyz  name: 'monkey toe'"));
    }
    public void testParseFilterNoSpace() {
        HashMap<String, String> expect = new HashMap<String, String>();
        expect.put("tags", "xyz");
        assertFilterMap(expect, null, NodeSet.parseFilter("tags:xyz"));
    }
    public void testParseFilterBasicExclude() {
        HashMap<String, String> expect = new HashMap<String, String>();
        expect.put("tags", "xyz");
        assertFilterMap(null, expect, NodeSet.parseFilter("!tags: xyz"));
    }
    public void testParseFilterMultiExclude() {
        HashMap<String, String> expect = new HashMap<String, String>();
        expect.put("tags", "xyz");
        expect.put("name", "macaroon brigade");
        assertFilterMap(null, expect, NodeSet.parseFilter("!tags: xyz !name: 'macaroon brigade' "));
    }
    public void testParseFilterBasicExcludeNoSpace() {
        HashMap<String, String> expect = new HashMap<String, String>();
        expect.put("tags", "xyz");
        assertFilterMap(null, expect, NodeSet.parseFilter("!tags:xyz"));
    }

    public void testParseFilterMultiMixed() {
        HashMap<String, String> expect = new HashMap<String, String>();
        HashMap<String, String> expectX = new HashMap<String, String>();
        expect.put("tags", "xyz,vwz");
        expectX.put("name", "macaroon brigade");
        assertFilterMap(expect, expectX, NodeSet.parseFilter("tags: xyz,vwz !name: 'macaroon brigade' "));
    }

    public void testParseFilterMultiMixedIgnoreSpaces() {
        HashMap<String, String> expect = new HashMap<String, String>();
        HashMap<String, String> expectX = new HashMap<String, String>();
        expect.put("tags", "xyz,vwz");
        expectX.put("name", "macaroon brigade");
        assertFilterMap(expect, expectX, NodeSet.parseFilter("    tags:   xyz,vwz  !name:    \"macaroon brigade\""));
    }
    public void testParseFilterMultiMixedIgnoreTabs() {
        HashMap<String, String> expect = new HashMap<String, String>();
        HashMap<String, String> expectX = new HashMap<String, String>();
        expectX.put("cloud.display_name", "Rackspace Open Cloud - Chicago");
        expectX.put("cloud.xyz", "monkeyfill");
        assertFilterMap(expect, expectX, NodeSet.parseFilter("!cloud.display_name:\t 'Rackspace Open Cloud - Chicago' !cloud.xyz:\r\n 'monkeyfill'"));
    }
    public void testParseFilterQuotedKeyValue() {
        HashMap<String, String> expect = new HashMap<String, String>();
        HashMap<String, String> expectX = new HashMap<String, String>();
        expectX.put("test", "money pants");
        assertFilterMap(expect, expectX, NodeSet.parseFilter(" '!test:money pants'  "));
    }
    public void testParseFilterQuotedKeyValueMultipleColon() {
        HashMap<String, String> expect = new HashMap<String, String>();
        HashMap<String, String> expectX = new HashMap<String, String>();
        expectX.put("test", "xyz:money pants");
        assertFilterMap(expect, expectX, NodeSet.parseFilter(" '!test:xyz:money pants'  "));
    }

    public void testParseFilterColonInKey() {
        HashMap<String, String> expect = new HashMap<String, String>();
        expect.put("as:test", "elf");
        assertFilterMap(expect, null, NodeSet.parseFilter("as:test: elf"));
    }
    public void testNodeSet() {
        set = new NodeSet();
        NodeSet.Exclude ex = set.createExclude();
        NodeSet.Include inc = set.createInclude();
        assertEquals("incorrect exclude object returned", ex, set.getExclude());
        assertEquals("incorrect include object returned", inc, set.getInclude());

        //multiple include/exclude are not allowed
        try {
            set.createExclude();
            fail("createExclude should fail the second time");
        } catch (IllegalStateException e) {

        }
        try {
            set.createInclude();
            fail("createInclude should fail the second time");
        } catch (IllegalStateException e) {

        }

        assertEquals("default threadcount was incorrect: " + set.getThreadCount(), 1, set.getThreadCount());
        assertEquals("default keepgoing was incorrect: " + set.isKeepgoing(), false, set.isKeepgoing());

        set.setThreadCount(3);
        assertEquals("threadcount was incorrect", 3, set.getThreadCount());
        set.setKeepgoing(true);
        assertEquals("keepgoing was incorrect", true, set.isKeepgoing());
    }

    public void testSetSelectorValues() {
        {
            set = new NodeSet();
            NodeSet.SetSelector sel = set.createInclude();
            assertTrue("should be blank", sel.isBlank());

            sel.setName("blah");
            assertFalse("should not be blank", sel.isBlank());
            assertEquals("incorrect value", "blah", sel.getName());
            sel.setName(null);
            assertTrue("should be blank", sel.isBlank());
            assertNull("value should be null", sel.getName());

            sel.setHostname("blah");
            assertFalse("should not be blank", sel.isBlank());
            assertEquals("incorrect value", "blah", sel.getHostname());
            sel.setHostname(null);
            assertTrue("should be blank", sel.isBlank());
            assertNull("value should be null", sel.getHostname());

            sel.setOsarch("blah");
            assertFalse("should not be blank", sel.isBlank());
            assertEquals("incorrect value", "blah", sel.getOsarch());
            sel.setOsarch(null);
            assertTrue("should be blank", sel.isBlank());
            assertNull("value should be null", sel.getOsarch());

            sel.setOsfamily("blah");
            assertFalse("should not be blank", sel.isBlank());
            assertEquals("incorrect value", "blah", sel.getOsfamily());
            sel.setOsfamily(null);
            assertTrue("should be blank", sel.isBlank());
            assertNull("value should be null", sel.getOsfamily());

            sel.setOsname("blah");
            assertFalse("should not be blank", sel.isBlank());
            assertEquals("incorrect value", "blah", sel.getOsname());
            sel.setOsname(null);
            assertTrue("should be blank", sel.isBlank());
            assertNull("value should be null", sel.getOsname());

            sel.setOsversion("blah");
            assertFalse("should not be blank", sel.isBlank());
            assertEquals("incorrect value", "blah", sel.getOsversion());
            sel.setOsversion(null);
            assertTrue("should be blank", sel.isBlank());
            assertNull("value should be null", sel.getOsversion());

            sel.setTags("blah");
            assertFalse("should not be blank", sel.isBlank());
            assertEquals("incorrect value", "blah", sel.getTags());
            sel.setTags(null);
            assertTrue("should be blank", sel.isBlank());
            assertNull("value should be null", sel.getTags());

        }
        //test single attribute
        {
            set = new NodeSet();
            NodeSet.SetSelector sel = set.createInclude();
            sel.getAttributesMap().put("test1", "value1");
            assertFalse("should not be blank", sel.isBlank());
            final Map<String, String> attributesMap = sel.getAttributesMap();
            assertNotNull("incorrect value", attributesMap);
            assertFalse("should not be empty", attributesMap.isEmpty());
            assertEquals("incorrect size", 1, attributesMap.size());
            assertTrue("missing key", attributesMap.containsKey("test1"));
            assertEquals("wrong value", "value1", attributesMap.get("test1"));
            sel.setAttributesMap(null);
            assertTrue("should be blank", sel.isBlank());
        }
        //test setting attributesMap
        {
            set = new NodeSet();
            NodeSet.SetSelector sel = set.createInclude();
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("test1", "value1");
            assertTrue("should be blank", sel.isBlank());
            sel.setAttributesMap(hashMap);
            assertFalse("should not be blank", sel.isBlank());
            final Map<String, String> attributesMap = sel.getAttributesMap();
            assertNotNull("incorrect value", attributesMap);
            assertFalse("should not be empty", attributesMap.isEmpty());
            assertEquals("incorrect size", 1, attributesMap.size());
            assertTrue("missing key", attributesMap.containsKey("test1"));
            assertEquals("wrong value", "value1", attributesMap.get("test1"));
            sel.setAttributesMap(null);
            assertTrue("should be blank", sel.isBlank());
        }
        //test multiple attributes
        {
            set = new NodeSet();
            NodeSet.SetSelector sel = set.createInclude();
            sel.getAttributesMap().put("test1", "value1");
            sel.getAttributesMap().put("test2", "value2");
            sel.getAttributesMap().put("test3", "value3");
            assertFalse("should not be blank", sel.isBlank());
            final Map<String, String> attributesMap = sel.getAttributesMap();
            assertNotNull("incorrect value", attributesMap);
            assertFalse("should not be empty", attributesMap.isEmpty());
            assertEquals("incorrect size", 3, attributesMap.size());
            assertTrue("missing key", attributesMap.containsKey("test1"));
            assertEquals("wrong value", "value1", attributesMap.get("test1"));
            assertTrue("missing key", attributesMap.containsKey("test2"));
            assertEquals("wrong value", "value2", attributesMap.get("test2"));
            assertTrue("missing key", attributesMap.containsKey("test3"));
            assertEquals("wrong value", "value3", attributesMap.get("test3"));
            sel.setAttributesMap(null);
            assertTrue("should be blank", sel.isBlank());
        }

        //test included attributeSet with no matching imported attributes
        {
            set = new NodeSet();
            NodeSet.SetSelector sel = set.createInclude();
            assertTrue("should be blank", sel.isBlank());
            final Map<String, String> attributesMap = sel.getAttributesMap();
            assertNotNull("incorrect value", attributesMap);
            assertTrue("should not be empty", attributesMap.isEmpty());
            assertEquals("incorrect size", 0, attributesMap.size());
            assertFalse("missing key", attributesMap.containsKey("test1"));
            assertNull("wrong value", attributesMap.get("test1"));
        }


    }
    public void testPopulateSetSelector() throws Exception {
        set = new NodeSet();
        NodeSet.SetSelector sel = set.createInclude();
        Map map = new HashMap();
        map.put(NodeSet.NAME, "abc");
        map.put(NodeSet.HOSTNAME, "hostname1");
        map.put(NodeSet.OS_NAME, "osname1");
        map.put(NodeSet.OS_ARCH, "osarch1");
        map.put(NodeSet.OS_FAMILY, "osfam1");
        map.put(NodeSet.OS_VERSION, "osvers1");
        map.put(NodeSet.TAGS, "tags1");
        map.put("anything", "anything1");
        set.populateSetSelector(map, sel);
        Assert.assertEquals("abc", sel.getName());
        Assert.assertEquals("hostname1", sel.getHostname());
        Assert.assertEquals("osname1", sel.getOsname());
        Assert.assertEquals("osarch1", sel.getOsarch());
        Assert.assertEquals("osfam1", sel.getOsfamily());
        Assert.assertEquals("osvers1", sel.getOsversion());
        Assert.assertEquals("tags1", sel.getTags());
        Assert.assertEquals("anything1", sel.getAttributesMap().get("anything"));
    }
    public void testPopulateSetSelectorArrayValue() throws Exception {
        set = new NodeSet();
        NodeSet.SetSelector sel = set.createInclude();
        Map map = new HashMap();
        map.put(NodeSet.NAME, new String[]{"name1", "name2"});
        map.put(NodeSet.OS_NAME, new String[]{"osname1", "osname2"});
        map.put(NodeSet.TAGS, new String[]{"tag1", "tag2"});
        map.put("anything", new String[]{"anything1", "anything2"});
        set.populateSetSelector(map, sel);
        Assert.assertEquals("name1,name2", sel.getName());
        Assert.assertEquals("osname1,osname2", sel.getOsname());
        Assert.assertEquals("tag1,tag2", sel.getTags());
        Assert.assertEquals("anything1,anything2", sel.getAttributesMap().get("anything"));
    }
    public void testPopulateSetSelectorCollectionValue() throws Exception {
        set = new NodeSet();
        NodeSet.SetSelector sel = set.createInclude();
        Map map = new HashMap();
        map.put(NodeSet.NAME, 1L);
        map.put(NodeSet.OS_NAME, new Object(){
            @Override
            public String toString() {
                return "osname1";
            }
        });
        set.populateSetSelector(map, sel);
        Assert.assertEquals("1", sel.getName());
        Assert.assertEquals("osname1", sel.getOsname());
    }
    public void testPopulateSetSelectorObjectValue() throws Exception {
        set = new NodeSet();
        NodeSet.SetSelector sel = set.createInclude();
        Map map = new HashMap();
        map.put(NodeSet.NAME, Arrays.asList("name1", "name2"));
        map.put(NodeSet.OS_NAME, Arrays.asList("osname1","osname2"));
        map.put(NodeSet.TAGS, Arrays.asList("tag1", "tag2"));
        map.put("anything", Arrays.asList("anything1", "anything2"));
        set.populateSetSelector(map, sel);
        Assert.assertEquals("name1,name2", sel.getName());
        Assert.assertEquals("osname1,osname2", sel.getOsname());
        Assert.assertEquals("tag1,tag2", sel.getTags());
        Assert.assertEquals("anything1,anything2", sel.getAttributesMap().get("anything"));
    }

    public void testMatchesInputMap() throws Exception {
        set = new NodeSet();
        NodeSet.SetSelector sel = set.createInclude();
        //test blank
        HashMap<String, String> filterset = new HashMap<String, String>();
        HashMap<String, String> attributes = new HashMap<String, String>();

        assertTrue(sel.matchOrBlank(filterset, attributes));
        assertTrue(sel.isBlank(filterset));

        //test mismatch
        filterset.put("test1", "notvalue");
        attributes.put("test1", "somevalue");
        assertFalse("isBlank should not return true.", sel.isBlank(filterset));

        assertFalse(NodeSet.matchesInput(filterset, attributes, true));

        //test exact match
        filterset.put("test1", "somevalue");
        attributes.put("test1", "somevalue");
        assertTrue(NodeSet.matchesInput(filterset, attributes, true));

        //test regex match
        filterset.put("test1", "som.v.lue");
        attributes.put("test1", "somevalue");
        assertTrue(NodeSet.matchesInput(filterset, attributes, true));

        filterset.clear();
        attributes.clear();

        //test list match, fail
        filterset.put("test1", "nonvalue,nonsomevalue");
        attributes.put("test1", "somevalue");
        assertFalse(NodeSet.matchesInput(filterset, attributes, true));

        //test list match, success
        filterset.put("test1", "nonvalue,somevalue");
        attributes.put("test1", "somevalue");
        assertTrue(NodeSet.matchesInput(filterset, attributes, true));

        //test multiple matches, fail
        filterset.put("test1", "value1");
        filterset.put("test2", "value2");
        attributes.put("test1", "xvalue1");
        attributes.put("test2", "xvalue2");
        assertFalse(NodeSet.matchesInput(filterset, attributes, true));

        //test multiple matches, fail
        filterset.put("test1", "value1");
        filterset.put("test2", "value2");
        attributes.put("test1", "value1");
        attributes.put("test2", "xvalue2");
        assertFalse(NodeSet.matchesInput(filterset, attributes, true));

        //test multiple matches, succeed
        filterset.put("test1", "value1");
        filterset.put("test2", "value2");
        attributes.put("test1", "value1");
        attributes.put("test2", "value2");
        assertTrue(NodeSet.matchesInput(filterset, attributes, true));

        //test multiple optional matches, fail
        filterset.put("test1", "value1");
        filterset.put("test2", "value2");
        attributes.put("test1", "xvalue1");
        attributes.put("test2", "xvalue2");
        assertFalse(NodeSet.matchesInput(filterset, attributes, false));

        //test multiple optional matches, succeed
        filterset.put("test1", "value1");
        filterset.put("test2", "value2");
        attributes.put("test1", "value1");
        attributes.put("test2", "xvalue2");
        assertTrue(NodeSet.matchesInput(filterset, attributes, false));

        //test multiple optional matches, succeed
        filterset.put("test1", "value1");
        filterset.put("test2", "value2");
        attributes.put("test1", "xvalue1");
        attributes.put("test2", "value2");
        assertTrue(NodeSet.matchesInput(filterset, attributes, false));

    }

    public void testMatchesInput() throws Exception {

        assertTrue("should match", NodeSet.matchesInput("test1", "test1"));
        assertFalse("should not match", NodeSet.matchesInput("test2", "test1"));

        //list mode matching
        assertTrue("should match", NodeSet.matchesInput("test1,test2", "test1"));
        assertFalse("should match", NodeSet.matchesInput("test1 test2", "test1"));
        assertFalse("should match", NodeSet.matchesInput("test1 test2,test3", "test1"));
        assertTrue("should match", NodeSet.matchesInput("test1,test2,test3", "test1"));
        assertFalse("should not match", NodeSet.matchesInput("testz,test2,test3", "test1"));

        //regular expression matching
        assertTrue("should match", NodeSet.matchesInput("test.", "test1"));
        assertFalse("should not match", NodeSet.matchesInput("test..", "test1"));
        assertFalse("should not match", NodeSet.matchesInput("test[a-b]", "test1"));
        assertTrue("should match", NodeSet.matchesInput("test[1-9]", "test1"));
        assertTrue("should match", NodeSet.matchesInput("test.*", "test1"));
        assertTrue("should match", NodeSet.matchesInput("test.+", "test1"));
        assertTrue("should match", NodeSet.matchesInput(".*test1", "test1"));
        assertTrue("should match", NodeSet.matchesInput(".*test.*", "test1"));
        assertFalse("should not match", NodeSet.matchesInput(".+test.*", "test1"));

    }

    public void testMatchesInputSet() throws Exception {
        HashSet hashSet = new HashSet();
        hashSet.add("test1");
        hashSet.add("test2");
        hashSet.add("test3");

        //basic membership
        assertTrue("should match", NodeSet.matchesInputSet("test1", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test2", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test3", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test3", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("testz", hashSet));

        //boolean membership
        assertTrue("should match", NodeSet.matchesInputSet("test1,test2", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test1,testz", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("testz,test2", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test1+test2", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("testz+test2", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("testz,testx", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("testz,test1+test2", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("testz+test1,test1+test2", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("testz+test1,test1+testx", hashSet));

        //regex membership
        assertTrue("should match", NodeSet.matchesInputSet("test.", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test[1-3]", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("test[4-9]", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test.*", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test.+", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("testz+", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("testz*", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test[z1]*", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test[z1]+", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet(".*test1", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet(".+test1", hashSet));

        //combination
        assertTrue("should match", NodeSet.matchesInputSet("test.,test2", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test.,test[1-3]", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test.,testz", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("testz,test[1-3]", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test[a-z],test2", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test1+test2", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test.+test2", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test.+test[1-3]", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("testz+test2", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("test[a-z]+test2", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("test[a-z]+test[1-3]", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("testz,test[xl]", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("test[abz],testx", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("test[abz],test[xt]", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test[a-x],test1+test2", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("testx,test[1-2]+test3", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("testx,test1+test[34]", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("testx,test[12]+test[34]", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("test[x-z],test[12]+test[34]", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("test[p-r],test[xz]+test[34]", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("test[p-r],test..+test[34]", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("test[p-r],test1+test[45]", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("testz+test[1-3],test1+test2", hashSet));
        assertTrue("should match", NodeSet.matchesInputSet("testz+test[1-3],test.+test2", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("testz+test[1-3],test1+testx", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("testz+test[1-3],test.+testx", hashSet));
        assertFalse("should not match", NodeSet.matchesInputSet("test[x-z]+test1,test.+testx", hashSet));


    }

    public void testShouldExclude() throws Exception {
        //test node filtering via the shouldExclude method
        //configure exclude/include with different types of filters, and test node entries
        {
            //test hostname
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setHostname("test1.local");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            inc.setHostname(null);
            exc.setHostname("test1.local");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));
            inc.setHostname(null);
            exc.setHostname("testnode2");
            assertFalse(set.shouldExclude(nodeimp1));
            assertEquals(exc.getHostname(), nodeimp2.getHostname());
            assertTrue(NodeSet.matchesInput(exc.getHostname(), nodeimp2.getHostname()));
            assertTrue(exc.matches(nodeimp2));
            assertFalse(exc.isBlank());
            assertTrue(inc.isBlank());
            assertTrue(set.shouldExclude(nodeimp2));

            inc.setHostname("testnode2");
            exc.setHostname("test1.local");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            inc.setHostname("testnode2,test1.local");
            exc.setHostname(null);
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setHostname("testnode2,test1.local");
            inc.setHostname(null);
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //exclude is dominant by default, so when both match the node will not be included
            exc.setHostname("testnode2");
            inc.setHostname("test.*");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setHostname("test1.local");
            inc.setHostname("test.*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is not dominant, so testnode2 will be excluded
            exc.setHostname("test.*");
            inc.setHostname("testnode2");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setHostname("test.*");
            inc.setHostname("test1.local");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is dominant, so when both match the node will be included
            inc.setDominant(true);
            exc.setDominant(false);
            exc.setHostname("testnode2");
            inc.setHostname("test.*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setHostname("test1.local");
            inc.setHostname("test.*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            //exclude is not dominant, so testnode2 will not be excluded
            exc.setHostname("test.*");
            inc.setHostname("testnode2");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setHostname("test.*");
            inc.setHostname("test1.local");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));
        }
        {
            //test resource name
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setName("testnode1");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            inc.setName(null);
            exc.setName("testnode1");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));
            inc.setName(null);
            exc.setName("testnode2");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));

            inc.setName("testnode2");
            exc.setName("testnode1");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            inc.setName("testnode2,testnode1");
            exc.setName(null);
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setName("testnode2,testnode1");
            inc.setName(null);
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //exclude is dominant by default, so when both match the node will not be included
            exc.setName("testnode2");
            inc.setName("test.*");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setName("testnode1");
            inc.setName("test.*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is not dominant, so testnode2 will be excluded
            exc.setName("test.*");
            inc.setName("testnode2");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setName("test.*");
            inc.setName("testnode1");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is dominant, so when both match the node will be included
            exc.setDominant(false);
            inc.setDominant(true);
            exc.setName("testnode2");
            inc.setName("test.*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setName("testnode1");
            inc.setName("test.*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            //exclude is not dominant, so testnode2 will not be excluded
            exc.setName("test.*");
            inc.setName("testnode2");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setName("test.*");
            inc.setName("testnode1");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));
        }
        {
            //test OS architecture
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setOsarch("x86");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            inc.setOsarch(null);
            exc.setOsarch("x86");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));
            inc.setOsarch(null);
            exc.setOsarch("x386");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));

            inc.setOsarch("x386");
            exc.setOsarch("x86");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            inc.setOsarch("x386,x86");
            exc.setOsarch(null);
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsarch("x386,x86");
            inc.setOsarch(null);
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //exclude is dominant by default, so when both match the node will not be included
            exc.setOsarch("x386");
            inc.setOsarch("x.*86");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsarch("x86");
            inc.setOsarch("x.*86");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is not dominant, so x386 will be excluded
            exc.setOsarch("x.*86");
            inc.setOsarch("x386");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setOsarch("x.*86");
            inc.setOsarch("x86");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is dominant, so when both match the node will be included
            exc.setDominant(false);
            inc.setDominant(true);
            exc.setOsarch("x386");
            inc.setOsarch("x.*86");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsarch("x86");
            inc.setOsarch("x.*86");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            //exclude is not dominant, so x386 will not be excluded
            exc.setOsarch("x.*86");
            inc.setOsarch("x386");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setOsarch("x.*86");
            inc.setOsarch("x86");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));
        }
        {
            //test OS architecture
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setOsfamily("windows");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            inc.setOsfamily(null);
            exc.setOsfamily("windows");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));
            inc.setOsfamily(null);
            exc.setOsfamily("unix");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));

            inc.setOsfamily("unix");
            exc.setOsfamily("windows");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            inc.setOsfamily("unix,windows");
            exc.setOsfamily(null);
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsfamily("unix,windows");
            inc.setOsfamily(null);
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //exclude is dominant, so when both match the node will not be included
            exc.setOsfamily("unix");
            inc.setOsfamily(".*");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsfamily("windows");
            inc.setOsfamily(".*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is not dominant, so unix will be excluded
            exc.setOsfamily(".*");
            inc.setOsfamily("unix");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setOsfamily(".*");
            inc.setOsfamily("windows");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is dominant, so when both match the node will be included
            exc.setDominant(false);
            inc.setDominant(true);
            exc.setOsfamily("unix");
            inc.setOsfamily(".*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsfamily("windows");
            inc.setOsfamily(".*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            //exclude is not dominant, so unix will not be excluded
            exc.setOsfamily(".*");
            inc.setOsfamily("unix");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setOsfamily(".*");
            inc.setOsfamily("windows");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));
        }
        {
            //test OS architecture
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setOsname("Windows.*");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            inc.setOsname(null);
            exc.setOsname("Windows.*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));
            inc.setOsname(null);
            exc.setOsname("Mac.*");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));

            inc.setOsname("Mac.*");
            exc.setOsname("Windows.*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            inc.setOsname(".*");
            exc.setOsname(null);
            assertEquals("Windows NT", nodeimp1.getOsName());
            assertEquals("Mac OS X", nodeimp2.getOsName());
            assertTrue(set.getInclude().matches(nodeimp1));
            assertTrue(set.getInclude().matches(nodeimp2));
            assertFalse(set.getExclude().matches(nodeimp1));
            assertFalse(set.getExclude().matches(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsname(".*");
            inc.setOsname(null);
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //exclude is dominant, so when both match the node will not be included
            exc.setOsname("Mac.*");
            inc.setOsname(".*");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsname("Windows.*");
            inc.setOsname(".*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is not dominant, so Mac.* will be excluded
            exc.setOsname(".*");
            inc.setOsname("Mac.*");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setOsname(".*");
            inc.setOsname("Windows.*");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is dominant, so when both match the node will be included
            exc.setDominant(false);
            inc.setDominant(true);
            exc.setOsname("Mac.*");
            inc.setOsname(".*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsname("Windows.*");
            inc.setOsname(".*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            //exclude is not dominant, so Mac.* will not be excluded
            exc.setOsname(".*");
            inc.setOsname("Mac.*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setOsname(".*");
            inc.setOsname("Windows.*");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));
        }
        {
            //test OS version
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setOsversion("5.1");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            inc.setOsversion(null);
            exc.setOsversion("5.1");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));
            inc.setOsversion(null);
            exc.setOsversion("10.5.1");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));

            inc.setOsversion("10.5.1");
            exc.setOsversion("5.1");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            inc.setOsversion(".*");
            exc.setOsversion(null);
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsversion(".*");
            inc.setOsversion(null);
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //exclude is dominant, so when both match the node will not be included
            exc.setOsversion("10.5.1");
            inc.setOsversion(".*");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsversion("5.1");
            inc.setOsversion(".*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is not dominant, so 10.5.1 will be excluded
            exc.setOsversion(".*");
            inc.setOsversion("10.5.1");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setOsversion(".*");
            inc.setOsversion("5.1");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is dominant, so when both match the node will be included
            exc.setDominant(false);
            inc.setDominant(true);
            exc.setOsversion("10.5.1");
            inc.setOsversion(".*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setOsversion("5.1");
            inc.setOsversion(".*");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            //exclude is not dominant, so 10.5.1 will not be excluded
            exc.setOsversion(".*");
            inc.setOsversion("10.5.1");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setOsversion(".*");
            inc.setOsversion("5.1");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));
        }
        {
            //test tags
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setTags("priority1");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            inc.setTags(null);
            exc.setTags("priority1");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));
            inc.setTags(null);
            exc.setTags("priority2");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));

            inc.setTags("priority2");
            exc.setTags("priority1");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            inc.setTags("priority2,priority1");
            exc.setTags(null);
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setTags("priority2,priority1");
            inc.setTags(null);
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //exclude is dominant, so when both match the node will not be included
            exc.setTags("priority2");
            inc.setTags("devenv");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setTags("priority1");
            inc.setTags("devenv");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is not dominant, so priority2 will be excluded
            exc.setTags("devenv");
            inc.setTags("priority2");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setTags("devenv");
            inc.setTags("priority1");
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            //include is dominant, so when both match the node will be included
            exc.setDominant(false);
            inc.setDominant(true);
            exc.setTags("priority2");
            inc.setTags("devenv");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            exc.setTags("priority1");
            inc.setTags("devenv");
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));

            //exclude is not dominant, so priority2 will not be excluded
            exc.setTags("devenv");
            inc.setTags("priority2");
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp1));

            exc.setTags("devenv");
            inc.setTags("priority1");
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp1));
        }
        {
            //test attribute sets
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.getAttributesMap().put("testattribute1", "testvalue1");
            assertTrue(set.shouldExclude(nodeimp1));
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp3));

            inc.getAttributesMap().put("testattribute1", "testvalue2");
            assertTrue(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp3));

            inc.getAttributesMap().remove("testattribute1");
            inc.getAttributesMap().put("testattribute2", "testvalue2");
            assertTrue(set.shouldExclude(nodeimp1));
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp3));

            inc.getAttributesMap().put("testattribute2", "testvalue2redux");
            assertTrue(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp3));

            //use list
            inc.getAttributesMap().put("testattribute2", "testvalue2,testvalue2redux");
            assertTrue(set.shouldExclude(nodeimp1));
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp3));

            inc.getAttributesMap().remove("testattribute2");
            inc.getAttributesMap().put("testattribute3", "testvalue3");
            assertTrue(set.shouldExclude(nodeimp1));
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp3));

            inc.getAttributesMap().put("testattribute3", "testvalue4");
            assertTrue(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp3));

            inc.getAttributesMap().remove("testattribute3");
            inc.getAttributesMap().put("testattribute4", "testvalue5");
            assertTrue(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp3));

        }
    }

    public void testShouldExcludeMulti() throws Exception {
        //test node filtering via the shouldExclude method
        //configure exclude/include each with multiple filters, and test node entries

        {
            //test hostname
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setHostname(".*.local");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp3));
        }
        {
            //test hostname
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setHostname(".*.local");
            inc.setHostname("test1.local");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp3));
        }
        {
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setHostname(".*.local");
            inc.setTags("workstation");
            assertTrue(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp3));
        }
        {
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setHostname(".*.local");
            inc.setTags("workstation,priority2");
            assertTrue(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp3));
        }
        {
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setTags("workstation,priority2");
            assertTrue(set.shouldExclude(nodeimp1));
            assertFalse(set.shouldExclude(nodeimp2));
            assertFalse(set.shouldExclude(nodeimp3));
        }
        {
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setTags("devenv");
            assertFalse(set.shouldExclude(nodeimp1));
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp3));
        }
        {
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setTags("devenv+priority2");
            assertTrue(set.shouldExclude(nodeimp1));
            assertFalse(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp3));
        }
        {
            set = new NodeSet();
            NodeSet.SetSelector inc = set.createInclude();
            NodeSet.SetSelector exc = set.createExclude();
            inc.setTags("devenv");
            exc.getAttributesMap().put("testattribute1", "testvalue1");
//            attribute.setName("testattribute1");
//            attribute.setValue("testvalue1");
            assertFalse(set.shouldExclude(nodeimp1));
            assertTrue(set.shouldExclude(nodeimp2));
            assertTrue(set.shouldExclude(nodeimp3));
        }
    }

    public void testMatchRegexOrEquals() throws Exception {
        //test simple string
        assertTrue(NodeSet.matchRegexOrEquals("test1", "test1"));
        assertTrue(NodeSet.matchRegexOrEquals("/", "/"));
        assertTrue(NodeSet.matchRegexOrEquals("/123", "/123"));
        assertFalse(NodeSet.matchRegexOrEquals("test2", "test1"));
        assertFalse(NodeSet.matchRegexOrEquals("/", "//"));
        assertFalse(NodeSet.matchRegexOrEquals("/123", "/1234"));

        //test invalid regex input
        assertTrue(NodeSet.matchRegexOrEquals("test1[", "test1["));
        assertTrue(NodeSet.matchRegexOrEquals("test/1[", "test/1["));
        assertTrue(NodeSet.matchRegexOrEquals("test/1[${", "test/1[${"));

        assertFalse(NodeSet.matchRegexOrEquals("test1[", "test2["));
        assertFalse(NodeSet.matchRegexOrEquals("test/1[", "test1["));
        assertFalse(NodeSet.matchRegexOrEquals("test/2[${", "test/1[${"));
        assertFalse(NodeSet.matchRegexOrEquals("test//1[${", "test/1[${"));
        assertFalse(NodeSet.matchRegexOrEquals("test/1[${}", "test/1[${"));
        assertFalse(NodeSet.matchRegexOrEquals("test(/1[${})?", "test/1[${"));

        //test valid regex input
        assertTrue(NodeSet.matchRegexOrEquals("test[123]", "test1"));
        assertTrue(NodeSet.matchRegexOrEquals("test[123]", "test2"));
        assertTrue(NodeSet.matchRegexOrEquals("test(abc)?", "testabc"));
        assertTrue(NodeSet.matchRegexOrEquals("test(abc)?", "test"));

        assertFalse(NodeSet.matchRegexOrEquals("test(abc)?", "testz"));
        assertFalse(NodeSet.matchRegexOrEquals("test(abc)?", "testabx"));

        //test regex that fails, equals that succeeds
        assertTrue(NodeSet.matchRegexOrEquals("test[zyx]", "test[zyx]"));

        //test explicit regex input
        assertTrue(NodeSet.matchRegexOrEquals("/test[zyx]/", "testx"));
        assertTrue(NodeSet.matchRegexOrEquals("/test[zyx]/", "testy"));
        assertTrue(NodeSet.matchRegexOrEquals("//", ""));

        assertFalse(NodeSet.matchRegexOrEquals("/test[zyx]/", "testZ"));
        assertFalse(NodeSet.matchRegexOrEquals("/test[zyx]/", "test[zyx]"));
        assertFalse(NodeSet.matchRegexOrEquals("/test[zyx]/", "/test[zyx]/"));

        //test explicit regex syntax error
        try {
            assertFalse(NodeSet.matchRegexOrEquals("/test[zyx/", "testz"));
            fail("should not succeed");
        } catch (PatternSyntaxException e) {
            assertNotNull(e);
        }
        try {
            assertFalse(NodeSet.matchRegexOrEquals("/test[zyx]${/", "testz${"));
            fail("should not succeed");
        } catch (PatternSyntaxException e) {
            assertNotNull(e);
        }

        try {
            assertFalse(NodeSet.matchRegexOrEquals("/test(/1[${})?/", "test"));
            fail("should not succeed");
        } catch (PatternSyntaxException e) {
            assertNotNull(e);
        }
    }
}
