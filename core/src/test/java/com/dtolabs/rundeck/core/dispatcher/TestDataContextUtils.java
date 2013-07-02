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

package com.dtolabs.rundeck.core.dispatcher;
/*
* TestDataContextUtils.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 19, 2010 12:38:47 PM
* $Id$
*/

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

public class TestDataContextUtils extends AbstractBaseTest {
    private static final String TEST_PROJECT = "TestDataContextUtils";

    File testfile1;

    public TestDataContextUtils(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestDataContextUtils.class);
    }

    protected void setUp() {
        super.setUp();
        FrameworkProject d = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(
            TEST_PROJECT);
        final File dir = new File(d.getBaseDir(), "var");
        assertTrue(dir.mkdirs());
        testfile1 = new File(dir, "test-file.sh");
        try {
            assertTrue(testfile1.createNewFile());
            final FileWriter writer = new FileWriter(testfile1);
            writer.write("test @test.data1@\n"
                         + "node test @node.test1@\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            fail("error: " + e.getMessage());
        }

    }

    protected void tearDown() throws Exception {
        testfile1.delete();
        FrameworkProject d = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(
            TEST_PROJECT);
        FileUtils.deleteDir(d.getBaseDir());
        getFrameworkInstance().getFrameworkProjectMgr().remove(TEST_PROJECT);

    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testReplaceDataReferences() throws Exception {

        //null data
        assertEquals("test ${test.key1}", DataContextUtils.replaceDataReferences("test ${test.key1}", null));
        assertEquals("${test2.key2}", DataContextUtils.replaceDataReferences("${test2.key2}", null));
        
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();

        //empty data
        assertEquals("test ${test.key1}", DataContextUtils.replaceDataReferences("test ${test.key1}", dataContext));
        assertEquals("${test2.key2}", DataContextUtils.replaceDataReferences("${test2.key2}", dataContext));

        //add context but no data for the keys
        dataContext.put("test", new HashMap<String, String>());

        assertEquals("test ${test.key1}", DataContextUtils.replaceDataReferences("test ${test.key1}", dataContext));
        assertEquals("${test2.key2}", DataContextUtils.replaceDataReferences("${test2.key2}", dataContext));

        //put in null value
        dataContext.get("test").put("key1", null);
        assertEquals("test ${test.key1}", DataContextUtils.replaceDataReferences("test ${test.key1}", dataContext));
        assertEquals("${test2.key2}", DataContextUtils.replaceDataReferences("${test2.key2}", dataContext));

        //put in some data
        dataContext.get("test").put("key1", "123");
        assertEquals("test 123", DataContextUtils.replaceDataReferences("test ${test.key1}", dataContext));

        //test null value for context
        dataContext.get("test").put("key1", "123");
        dataContext.put("test2", null);
        assertEquals("test ${test2.key1}", DataContextUtils.replaceDataReferences("test ${test2.key1}", dataContext));

        //test null value for data
        dataContext.get("test").put("key2", null);
        assertEquals("test ${test.key2}", DataContextUtils.replaceDataReferences("test ${test.key2}", dataContext));
    }

    public void testReplaceDataReferencesArray() throws Exception {

        {
            Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
            String[] arr1 = null;

            assertNull("should be null", DataContextUtils.replaceDataReferences(arr1, dataContext));
        }
        {
            Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
            String[] arr1 = {};
            assertNotNull("should not be null", DataContextUtils.replaceDataReferences(arr1, dataContext));
            assertEquals("wrong length", 0, DataContextUtils.replaceDataReferences(arr1, dataContext).length);
        }
        {
            Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
            String[] arr1 = {"a"};
            assertNotNull("should not be null", DataContextUtils.replaceDataReferences(arr1, dataContext));
            assertEquals("wrong length", 1, DataContextUtils.replaceDataReferences(arr1, dataContext).length);
            assertEquals("wrong value", "a", DataContextUtils.replaceDataReferences(arr1, dataContext)[0]);
        }
        {
            Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
            String[] arr1 = {"a", "test ${test.key1}"};
            assertNotNull("should not be null", DataContextUtils.replaceDataReferences(arr1, dataContext));
            assertEquals("wrong length", 2, DataContextUtils.replaceDataReferences(arr1, dataContext).length);
            assertEquals("wrong value", "a", DataContextUtils.replaceDataReferences(arr1, dataContext)[0]);
            assertEquals("wrong value", "test ${test.key1}", DataContextUtils.replaceDataReferences(arr1,
                dataContext)[1]);
        }
        {
            Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
            dataContext.put("test", new HashMap<String, String>());
            String[] arr1 = {"a", "test ${test.key1}"};
            assertNotNull("should not be null", DataContextUtils.replaceDataReferences(arr1, dataContext));
            assertEquals("wrong length", 2, DataContextUtils.replaceDataReferences(arr1, dataContext).length);
            assertEquals("wrong value", "a", DataContextUtils.replaceDataReferences(arr1, dataContext)[0]);
            assertEquals("wrong value", "test ${test.key1}", DataContextUtils.replaceDataReferences(arr1,
                dataContext)[1]);
        }
        {
            Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
            dataContext.put("test", new HashMap<String, String>());
            dataContext.get("test").put("key1", null);
            String[] arr1 = {"a", "test ${test.key1}"};
            assertNotNull("should not be null", DataContextUtils.replaceDataReferences(arr1, dataContext));
            assertEquals("wrong length", 2, DataContextUtils.replaceDataReferences(arr1, dataContext).length);
            assertEquals("wrong value", "a", DataContextUtils.replaceDataReferences(arr1, dataContext)[0]);
            assertEquals("wrong value", "test ${test.key1}", DataContextUtils.replaceDataReferences(arr1,
                dataContext)[1]);
        }
        {
            Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
            dataContext.put("test", new HashMap<String, String>());
            dataContext.get("test").put("key1", "123");
            String[] arr1 = {"a", "test ${test.key1}"};
            assertNotNull("should not be null", DataContextUtils.replaceDataReferences(arr1, dataContext));
            assertEquals("wrong length", 2, DataContextUtils.replaceDataReferences(arr1, dataContext).length);
            assertEquals("wrong value", "a", DataContextUtils.replaceDataReferences(arr1, dataContext)[0]);
            assertEquals("wrong value", "test 123", DataContextUtils.replaceDataReferences(arr1, dataContext)[1]);
        }

    }

    public void testReplaceTokensInScript() throws Exception {
        Framework fwk = getFrameworkInstance();
        try {
            DataContextUtils.replaceTokensInScript(null, null, null);
            fail("Should have thrown an Exception");
        }
        catch (NullPointerException ex) {
        }
    }
    public void testReplaceTokensInScript2() throws Exception {
        Framework fwk = getFrameworkInstance();
        try {
            DataContextUtils.replaceTokensInScript("test script", null, null);
            fail("Should have thrown an Exception");
        }
        catch (NullPointerException ex) {
        }
    }
    public void testReplaceTokensInScript3() throws Exception {
        Framework fwk = getFrameworkInstance();
        try {
            DataContextUtils.replaceTokensInScript("test script", new HashMap<String, Map<String, String>>(), null);
            fail("Should have thrown an Exception");
        }
        catch (NullPointerException ex) {
        }
    }

    public void testReplaceTokensInScript4() throws Exception {
        Framework fwk = getFrameworkInstance();
            //null data context is ok
            File temp = null;
            try {
                temp = DataContextUtils.replaceTokensInScript("test script", null, fwk);
            } catch (IOException e) {
                e.printStackTrace(System.out);
                fail("Unexpected IO Exception: " + e.getMessage());
            }
            assertNotNull(temp);
            assertTrue(temp.length() > 0);
            temp.delete();
    }

    public void testReplaceTokensInScript5() throws Exception {
        Framework fwk = getFrameworkInstance();

            //test empty data context
            File temp = DataContextUtils.replaceTokensInScript("test script",
                new HashMap<String, Map<String, String>>(),
                fwk);
            assertNotNull(temp);
            assertTrue(temp.length() > 0);
            temp.delete();
    }

    public void testReplaceTokensInScript6() throws Exception {
        Framework fwk = getFrameworkInstance();
            //test content
            File temp = DataContextUtils.replaceTokensInScript("test script some data @test.data@\n"
                                                               + "test line 2 some data @test.data2@\n",
                new HashMap<String, Map<String, String>>(),
                fwk);
            assertNotNull(temp);
            assertTrue(temp.length() > 0);
            //test content
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(temp)));
            assertTrue(br.ready());
            final String line0 = br.readLine();
            assertNotNull(line0);
            assertEquals("test script some data ", line0);
            assertTrue(br.ready());
            final String line1 = br.readLine();
            assertNotNull(line1);
            assertEquals("test line 2 some data ", line1);
            assertFalse(br.ready());
            assertNull(br.readLine());

    }

    public void testReplaceTokensInScript7() throws Exception {
        Framework fwk = getFrameworkInstance();
            //test content
            final HashMap<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();
            data.put("test", new HashMap<String, String>());
            File temp = DataContextUtils.replaceTokensInScript("test script some data @test.data@\n"
                                                               + "test line 2 some data @test.data2@\n",
                data,
                fwk);
            assertNotNull(temp);
            assertTrue(temp.length() > 0);
            //test content
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(temp)));
            assertTrue(br.ready());
            final String line0 = br.readLine();
            assertNotNull(line0);
            assertEquals("test script some data ", line0);
            assertTrue(br.ready());
            final String line1 = br.readLine();
            assertNotNull(line1);
            assertEquals("test line 2 some data ", line1);
            assertFalse(br.ready());
            assertNull(br.readLine());

    }

    public void testReplaceTokensInScript8() throws Exception {
        Framework fwk = getFrameworkInstance();
            //test content
            final HashMap<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testdata = new HashMap<String, String>();
            testdata.put("data", "this is a test");
            data.put("test", testdata);
            File temp = DataContextUtils.replaceTokensInScript("test script some data @test.data@\n"
                                                               + "test line 2 some data @test.data2@\n",
                data,
                fwk);
            assertNotNull(temp);
            assertTrue(temp.length() > 0);
            //test content
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(temp)));
            assertTrue(br.ready());
            final String line0 = br.readLine();
            assertNotNull(line0);
            assertEquals("test script some data this is a test", line0);
            assertTrue(br.ready());
            final String line1 = br.readLine();
            assertNotNull(line1);
            assertEquals("test line 2 some data ", line1);
            assertFalse(br.ready());
            assertNull(br.readLine());

    }

    public void testReplaceTokensInScript9() throws Exception {
        Framework fwk = getFrameworkInstance();
        //test content
        final HashMap<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();
        final HashMap<String, String> testdata = new HashMap<String, String>();
        testdata.put("data", "this is a test");
        testdata.put("data2", "2this is a test2");
        data.put("test", testdata);
        File temp = DataContextUtils.replaceTokensInScript("test script some data @test.data@\n"
                                                           + "test line 2 some data @test.data2@\n",
            data,
            fwk);
        assertNotNull(temp);
        assertTrue(temp.length() > 0);
        //test content
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(temp)));
        assertTrue(br.ready());
        final String line0 = br.readLine();
        assertNotNull(line0);
        assertEquals("test script some data this is a test", line0);
        assertTrue(br.ready());
        final String line1 = br.readLine();
        assertNotNull(line1);
        assertEquals("test line 2 some data 2this is a test2", line1);
        assertFalse(br.ready());
        assertNull(br.readLine());

    }

    public void testReplaceTokensInFile() throws Exception {
        Framework fwk = getFrameworkInstance();
        try {
            DataContextUtils.replaceTokensInFile((File) null, null, null);
            fail("Should have thrown an Exception");
        }
        catch (NullPointerException ex) {
        }
    }
    public void testReplaceTokensInFile2() throws Exception {
        Framework fwk = getFrameworkInstance();
        try {
            DataContextUtils.replaceTokensInFile(testfile1, null, null);
            fail("Should have thrown an Exception");
        }
        catch (NullPointerException ex) {
        }
    }

    public void testReplaceTokensInFile3() throws Exception {
        Framework fwk = getFrameworkInstance();
        try {
            DataContextUtils.replaceTokensInFile(testfile1, new HashMap<String, Map<String, String>>(), null);
            fail("Should have thrown an Exception");
        }
        catch (NullPointerException ex) {
        }
    }

    public void testReplaceTokensInFile4() throws Exception {
        Framework fwk = getFrameworkInstance();
            assertTrue(testfile1.length() > 0);
            //null data context is ok
            File temp = null;
            try {
                temp = DataContextUtils.replaceTokensInFile(testfile1, null, fwk);
            } catch (IOException e) {
                e.printStackTrace(System.out);
                fail("Unexpected IO Exception: " + e.getMessage());
            }
            assertNotNull(temp);
            assertTrue(temp.length() > 0);
            temp.delete();
    }

    public void testReplaceTokensInFile5() throws Exception {
        Framework fwk = getFrameworkInstance();
            //test empty data context
            File temp = DataContextUtils.replaceTokensInFile(testfile1,
                new HashMap<String, Map<String, String>>(),
                fwk);
            assertNotNull(temp);
            assertTrue(temp.length() > 0);
            temp.delete();
    }

    public void testReplaceTokensInFile6() throws Exception {
        Framework fwk = getFrameworkInstance();
            //test content
            File temp = DataContextUtils.replaceTokensInFile(testfile1,
                new HashMap<String, Map<String, String>>(),
                fwk);
            assertNotNull(temp);
            assertTrue(temp.length() > 0);
            //test content
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(temp)));
            assertTrue(br.ready());
            final String line0 = br.readLine();
            assertNotNull(line0);
            assertEquals("test ", line0);
            assertTrue(br.ready());
            final String line1 = br.readLine();
            assertNotNull(line1);
            assertEquals("node test ", line1);
            assertFalse(br.ready());
            assertNull(br.readLine());

    }

    public void testReplaceTokensInFile7() throws Exception {
        Framework fwk = getFrameworkInstance();
            //test content
            final HashMap<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();
            data.put("test", new HashMap<String, String>());
            File temp = DataContextUtils.replaceTokensInFile(testfile1,
                data,
                fwk);
            assertNotNull(temp);
            assertTrue(temp.length() > 0);
            //test content
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(temp)));
            assertTrue(br.ready());
            final String line0 = br.readLine();
            assertNotNull(line0);
            assertEquals("test ", line0);
            assertTrue(br.ready());
            final String line1 = br.readLine();
            assertNotNull(line1);
            assertEquals("node test ", line1);
            assertFalse(br.ready());
            assertNull(br.readLine());

    }

    public void testReplaceTokensInFile8() throws Exception {
        Framework fwk = getFrameworkInstance();
            //test content
            final HashMap<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> testdata = new HashMap<String, String>();
            testdata.put("data1", "this is a test");
            data.put("test", testdata);
            File temp = DataContextUtils.replaceTokensInFile(testfile1,
                data,
                fwk);
            assertNotNull(temp);
            assertTrue(temp.length() > 0);
            //test content
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(temp)));
            assertTrue(br.ready());
            final String line0 = br.readLine();
            assertNotNull(line0);
            assertEquals("test this is a test", line0);
            assertTrue(br.ready());
            final String line1 = br.readLine();
            assertNotNull(line1);
            assertEquals("node test ", line1);
            assertFalse(br.ready());
            assertNull(br.readLine());

    }

    public void testReplaceTokensInFile9() throws Exception {
        Framework fwk = getFrameworkInstance();
        //test content
        final HashMap<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();
        final HashMap<String, String> testdata = new HashMap<String, String>();
        testdata.put("data1", "this is a test");
        data.put("test", testdata);
        final HashMap<String, String> nodedata = new HashMap<String, String>();
        nodedata.put("test1", "node test data");
        data.put("node", nodedata);
        File temp = DataContextUtils.replaceTokensInFile(testfile1,
            data,
            fwk);
        assertNotNull(temp);
        assertTrue(temp.length() > 0);
        //test content
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(temp)));
        assertTrue(br.ready());
        final String line0 = br.readLine();
        assertNotNull(line0);
        assertEquals("test this is a test", line0);
        assertTrue(br.ready());
        final String line1 = br.readLine();
        assertNotNull(line1);
        assertEquals("node test node test data", line1);
        assertFalse(br.ready());
        assertNull(br.readLine());

    }

    public void testNodeData() throws Exception {
        {
            final NodeEntryImpl nodeentry = new NodeEntryImpl("testhost", "test1");
            final Map<String, String> stringMap = DataContextUtils.nodeData(nodeentry);
            assertNotNull(stringMap);
            assertEquals("wrong size: " + stringMap, 9, stringMap.size());
            assertEquals("test1", stringMap.get("name"));
            assertEquals("testhost", stringMap.get("hostname"));
            assertEquals("", stringMap.get("os-name"));
            assertEquals("", stringMap.get("os-family"));
            assertEquals("", stringMap.get("os-arch"));
            assertEquals("", stringMap.get("os-version"));
            assertEquals("", stringMap.get("tags"));
            assertEquals("", stringMap.get("username"));
            assertEquals("", stringMap.get("description"));
        }
        {
            final NodeEntryImpl nodeentry = new NodeEntryImpl("testhost", "test1");
            nodeentry.setDescription("testdesc");
            nodeentry.setUsername("username");
            nodeentry.setOsArch("osarch");
            nodeentry.setOsFamily("osfamily");
            nodeentry.setOsName("osname");
            nodeentry.setOsVersion("osversion");
            nodeentry.setTags(new HashSet());

            final Map<String, String> stringMap = DataContextUtils.nodeData(nodeentry);
            assertNotNull(stringMap);
            assertEquals(9, stringMap.size());
            assertEquals("test1", stringMap.get("name"));
            assertEquals("testhost", stringMap.get("hostname"));
            assertEquals("osname", stringMap.get("os-name"));
            assertEquals("osfamily", stringMap.get("os-family"));
            assertEquals("osarch", stringMap.get("os-arch"));
            assertEquals("osversion", stringMap.get("os-version"));
            assertEquals("", stringMap.get("tags"));
            assertEquals("username", stringMap.get("username"));
            assertEquals("testdesc", stringMap.get("description"));
        }
        {
            final NodeEntryImpl nodeentry = new NodeEntryImpl("testhost", "test1");
            final HashSet tags = new HashSet();
            tags.add("tag1");
            nodeentry.setTags(tags);

            final Map<String, String> stringMap = DataContextUtils.nodeData(nodeentry);
            assertNotNull(stringMap);
            assertEquals(9, stringMap.size());
            assertEquals("test1", stringMap.get("name"));
            assertEquals("testhost", stringMap.get("hostname"));
            assertEquals("tag1", stringMap.get("tags"));
        }
        {
            final NodeEntryImpl nodeentry = new NodeEntryImpl("testhost", "test1");
            final TreeSet tags = new TreeSet();
            tags.add("tag1");
            tags.add("tag2");
            tags.add("xyz");
            nodeentry.setTags(tags);

            final Map<String, String> stringMap = DataContextUtils.nodeData(nodeentry);
            assertNotNull(stringMap);
            assertEquals(9, stringMap.size());
            assertEquals("test1", stringMap.get("name"));
            assertEquals("testhost", stringMap.get("hostname"));
            assertEquals("tag1,tag2,xyz", stringMap.get("tags"));
        }
    }

    public void testAddContext() throws Exception {
        {
            final Map<String, Map<String, String>> map = DataContextUtils.addContext("test1",
                new HashMap<String, String>(),
                new HashMap<String, Map<String, String>>());

            assertNotNull(map);
            assertEquals(1, map.size());
            assertNotNull(map.get("test1"));
            assertEquals(0, map.get("test1").size());
        }
        {
            final HashMap<String, Map<String, String>> origContext = new HashMap<String, Map<String, String>>();
            origContext.put("test1", new HashMap<String, String>());
            final Map<String, Map<String, String>> map = DataContextUtils.addContext("test2",
                new HashMap<String, String>(),
                origContext);

            assertNotNull(map);
            assertEquals(2, map.size());
            assertNotNull(map.get("test1"));
            assertEquals(0, map.get("test1").size());
            assertNotNull(map.get("test2"));
            assertEquals(0, map.get("test2").size());
        }
        {
            final HashMap<String, Map<String, String>> origContext = new HashMap<String, Map<String, String>>();
            final HashMap<String, String> test1data = new HashMap<String, String>();
            test1data.put("key1", "value1");
            origContext.put("test1", test1data);
            final Map<String, Map<String, String>> map = DataContextUtils.addContext("test2",
                new HashMap<String, String>(),
                origContext);

            assertNotNull(map);
            assertEquals(2, map.size());
            assertNotNull(map.get("test1"));
            assertEquals(1, map.get("test1").size());
            assertNotNull(map.get("test1").get("key1"));
            assertEquals("value1", map.get("test1").get("key1"));
            assertNotNull(map.get("test2"));
            assertEquals(0, map.get("test2").size());
        }
    }

    public void testMerge(){
        final HashMap<String, Map<String, String>> origContext = new HashMap<String, Map<String, String>>();
        final HashMap<String, String> test1data = new HashMap<String, String>();
        test1data.put("key1", "value1");
        origContext.put("test1", test1data);
        final HashMap<String, String> test2data = new HashMap<String, String>();
        test2data.put("key2", "value2");
        origContext.put("test2", test2data);


        final HashMap<String, Map<String, String>> newContext = new HashMap<String, Map<String, String>>();
        final HashMap<String, String> test3data = new HashMap<String, String>();
        test3data.put("key1", "value2");
        newContext.put("test1", test3data);
        final Map<String, Map<String, String>> result = DataContextUtils.merge(origContext, newContext);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNotNull(result.get("test1"));
        assertEquals(1, result.get("test1").size());
        assertNotNull(result.get("test1").get("key1"));
        assertEquals("value2", result.get("test1").get("key1"));
        assertNotNull(result.get("test2"));
        assertEquals(1, result.get("test2").size());
        assertNotNull(result.get("test2").get("key2"));
        assertEquals("value2", result.get("test2").get("key2"));
    }
}
