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

package com.dtolabs.rundeck.core.cli.util;
/*
* TestMvnPomInfoTool.java
* 
* User: greg
* Created: Apr 14, 2008 11:39:34 AM
* $Id$
*/


import junit.framework.*;
import com.dtolabs.rundeck.core.cli.util.MvnPomInfoTool;

import java.io.File;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


public class TestMvnPomInfoTool extends TestCase {
    File testbase;
    File tempfile ;

    public void testRun() throws Exception {

        SAXReader reader = new SAXReader();
        {
            final TestTool tool1 = new TestTool();
            tool1.basedir = testbase;
            tool1.packaging = "rpm";
            tool1.destfile = tempfile.getAbsolutePath();
            tool1.run();
            assertTrue(tempfile.exists());


            Document dom = reader.read(tempfile);
            assertNotNull(dom);
            assertNotNull(dom.selectSingleNode("/packages"));
            assertNotNull(dom.selectSingleNode("/packages/@basedir"));
            assertEquals("incorrect basedir",
                         testbase.getAbsolutePath(),
                         dom.selectSingleNode("/packages/@basedir").getStringValue());

            assertNotNull(dom.selectSingleNode("/packages/package"));
            assertEquals("incorrect size", 2, dom.selectNodes("/packages/package").size());
            assertNotNull(dom.selectSingleNode("/packages/package[@name='my_APP-0.1.2.rpm']"));
            assertNotNull(dom.selectSingleNode("/packages/package[@name='ab-testda-SNAPSHOT.rpm']"));
            Element p1 = (Element) dom.selectSingleNode("/packages/package[@name='my_APP-0.1.2.rpm']");
            assertEquals("incorrect relative path", "my_APP-0.1.2.rpm", p1.attributeValue("name"));
            assertEquals("incorrect relative path", "pom.xml", p1.attributeValue("pom-path"));
            assertEquals("incorrect relative path", "ab/cd", p1.attributeValue("repo-path"));

            Element p2 = (Element) dom.selectSingleNode("/packages/package[@name='ab-testda-SNAPSHOT.rpm']");
            assertEquals("incorrect relative path", "ab-testda-SNAPSHOT.rpm", p2.attributeValue("name"));
            assertEquals("incorrect relative path", "a/pom.xml", p2.attributeValue("pom-path"));
            assertEquals("incorrect relative path", "ef/gh", p2.attributeValue("repo-path"));
        }
        {
            final TestTool tool1 = new TestTool();
            tool1.basedir = testbase;
            tool1.packaging = "pom";
            tool1.destfile = tempfile.getAbsolutePath();
            tool1.run();
            Document dom = reader.read(tempfile);
            assertNotNull(dom);
            assertNotNull(dom.selectSingleNode("/packages"));
            assertNotNull(dom.selectSingleNode("/packages/@basedir"));
            assertEquals("incorrect basedir",
                         testbase.getAbsolutePath(),
                         dom.selectSingleNode("/packages/@basedir").getStringValue());

            assertNotNull(dom.selectSingleNode("/packages/package"));
            assertEquals("incorrect size", 1, dom.selectNodes("/packages/package").size());
            assertNull(dom.selectSingleNode("/packages/package[@name='my_APP-0.1.2.rpm']"));
            assertNull(dom.selectSingleNode("/packages/package[@name='ab-testda-SNAPSHOT.rpm']"));
            assertNotNull(dom.selectSingleNode("/packages/package[@name='3p-stb-parent-aggregator-1.0-SNAPSHOT.pom']"));
            Element p1 = (Element) dom.selectSingleNode(
                "/packages/package[@name='3p-stb-parent-aggregator-1.0-SNAPSHOT.pom']");
            assertEquals("incorrect relative path",
                         "3p-stb-parent-aggregator-1.0-SNAPSHOT.pom",
                         p1.attributeValue("name"));
            assertEquals("incorrect relative path", "b/pom.xml", p1.attributeValue("pom-path"));
            assertEquals("incorrect relative path", "ab/cd/parents", p1.attributeValue("repo-path"));

        }
    }

    class TestTool extends MvnPomInfoTool {
        boolean test_exit = false;
        boolean test_help = false;
        int test_err_code = -1;

        public void help() {
            test_help = true;
        }

        protected void exit(int err) {
            test_exit = true;
            test_err_code = err;
        }
    }

    TestTool tool;

    public TestMvnPomInfoTool(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestMvnPomInfoTool.class);
    }

    protected void setUp() throws Exception {
        testbase = new File("src/test/com/dtolabs/rundeck/core/cli/util/mvntest");
        tempfile = File.createTempFile("testout.", ".xml");
    }

    protected void tearDown() throws Exception {
        if(tempfile.exists()) {
            tempfile.delete();
        }
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testParseArgs() throws Exception {
        try {
            TestTool testTool = new TestTool();
            assertFalse(testTool.test_exit);
            assertFalse(testTool.test_help);
            testTool.parseArgs(new String[0]);
            assertTrue("should exit with no args", testTool.test_exit);
            assertTrue("should print help with no args", testTool.test_help);
            assertEquals("should exit with error code", 2, testTool.test_err_code);
        } catch (IllegalArgumentException e) {
            fail("shouldn't throw error: " + e);
        }
        try {
            TestTool testTool = new TestTool();
            assertFalse(testTool.test_exit);
            assertFalse(testTool.test_help);
            testTool.parseArgs(new String[]{"-h"});
            assertTrue("should exit with -h", testTool.test_exit);
            assertTrue("should print help with -h", testTool.test_help);
            assertEquals("should exit with error code", 2, testTool.test_err_code);
        } catch (IllegalArgumentException e) {
            fail("shouldn't throw error: " + e);
        }
        try {
            new TestTool().parseArgs(new String[]{"-o", "/tmp"});
            fail("Should not succeed without all required arguments.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            new TestTool().parseArgs(new String[]{"-b", "/tmp"});
            fail("Should not succeed without all required arguments.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            new TestTool().parseArgs(new String[]{"-p", "/tmp"});
            fail("Should not succeed without all required arguments.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            new TestTool().parseArgs(new String[]{"-o", "/tmp", "-b", "/tmp"});
            fail("Should not succeed without all required arguments.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            new TestTool().parseArgs(new String[]{"-o", "/tmp", "-p", "/tmp"});
            fail("Should not succeed without all required arguments.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            new TestTool().parseArgs(new String[]{"-b", "/tmp", "-p", "/tmp"});
            fail("Should not succeed without all required arguments.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            new TestTool().parseArgs(new String[]{"-o", "/tmp", "-b", "/tmp", "-p", "pkg"});
        } catch (IllegalArgumentException e) {
            fail("should not throw exception with all required params: " + e);
        }
    }

    public void testValidate() throws Exception {
        try {
            TestTool tool1 = new TestTool();
            tool1.validate();
            fail("Should not validate without basedir required arguments.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            TestTool tool1 = new TestTool();
            tool1.basedir = new File("/DoesNotExist");
            tool1.validate();
            fail("Should not validate without basedir required arguments.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            TestTool tool1 = new TestTool();
            tool1.basedir = new File("/");
            tool1.validate();
        } catch (IllegalArgumentException e) {
            fail("should validate with valid basedir: " + e);
        }
    }

    public void testProcess() throws Exception {

        {
            final TestTool tool1 = new TestTool();
            tool1.basedir = testbase;
            tool1.packaging = "rpm";
            tool1.destfile = "-";
            tool1.validate();
            tool1.process();
            assertEquals("incorrect size", 2, tool1.data.size());
            {
                File t1 = new File(testbase, "pom.xml");
                assertNotNull(tool1.data.get(t1.getAbsolutePath()));
                Map data = (Map) tool1.data.get(t1.getAbsolutePath());
                assertEquals("incorrect size", 4, data.size());
                assertEquals("incorrect value", "ab.cd", data.get("groupId"));
                assertEquals("incorrect value", "my_APP", data.get("artifactId"));
                assertEquals("incorrect value", "0.1.2", data.get("version"));
                assertEquals("incorrect value", "rpm", data.get("packaging"));
            }
            {
                File t1 = new File(testbase, "a/pom.xml");
                assertNotNull(tool1.data.get(t1.getAbsolutePath()));
                Map data = (Map) tool1.data.get(t1.getAbsolutePath());
                assertEquals("incorrect size", 4, data.size());
                assertEquals("incorrect value", "ef.gh", data.get("groupId"));
                assertEquals("incorrect value", "ab-testda", data.get("artifactId"));
                assertEquals("incorrect value", "SNAPSHOT", data.get("version"));
                assertEquals("incorrect value", "rpm", data.get("packaging"));
            }

        }

        //test with different packaging value
        {
            TestTool tool1 = new TestTool();
            tool1.basedir = testbase;
            tool1.packaging = "pom";
            tool1.destfile = "-";
            tool1.validate();
            tool1.process();
            assertEquals("incorrect size", 1, tool1.data.size());
            {
                File t1 = new File(testbase, "b/pom.xml");
                assertNotNull(tool1.data.get(t1.getAbsolutePath()));
                Map data = (Map) tool1.data.get(t1.getAbsolutePath());
                assertEquals("incorrect size", 4, data.size());
                assertEquals("incorrect value", "ab.cd.parents", data.get("groupId"));
                assertEquals("incorrect value", "3p-stb-parent-aggregator", data.get("artifactId"));
                assertEquals("incorrect value", "1.0-SNAPSHOT", data.get("version"));
                assertEquals("incorrect value", "pom", data.get("packaging"));
            }
        }

        //test items without correct pom elements
        {
            TestTool tool1 = new TestTool();
            tool1.basedir = testbase;
            tool1.packaging = "elt";
            tool1.destfile = "-";
            tool1.validate();
            tool1.process();
            assertEquals("incorrect size", 0, tool1.data.size());
        }
    }

    public void testGenerateData() throws Exception {
        {
            final TestTool tool1 = new TestTool();
            tool1.basedir = testbase;
            tool1.packaging = "rpm";
            tool1.destfile = "-";
            tool1.validate();
            tool1.process();
            Document dom = tool1.generateData();
            assertNotNull(dom);
            assertNotNull(dom.selectSingleNode("/packages"));
            assertNotNull(dom.selectSingleNode("/packages/@basedir"));
            assertEquals("incorrect basedir",
                         testbase.getAbsolutePath(),
                         dom.selectSingleNode("/packages/@basedir").getStringValue());

            assertNotNull(dom.selectSingleNode("/packages/package"));
            assertEquals("incorrect size", 2, dom.selectNodes("/packages/package").size());
            assertNotNull(dom.selectSingleNode("/packages/package[@name='my_APP-0.1.2.rpm']"));
            assertNotNull(dom.selectSingleNode("/packages/package[@name='ab-testda-SNAPSHOT.rpm']"));
            Element p1 = (Element) dom.selectSingleNode("/packages/package[@name='my_APP-0.1.2.rpm']");
            assertEquals("incorrect relative path", "my_APP-0.1.2.rpm", p1.attributeValue("name"));
            assertEquals("incorrect relative path", "pom.xml", p1.attributeValue("pom-path"));
            assertEquals("incorrect relative path", "ab/cd", p1.attributeValue("repo-path"));

            Element p2 = (Element) dom.selectSingleNode("/packages/package[@name='ab-testda-SNAPSHOT.rpm']");
            assertEquals("incorrect relative path", "ab-testda-SNAPSHOT.rpm", p2.attributeValue("name"));
            assertEquals("incorrect relative path", "a/pom.xml", p2.attributeValue("pom-path"));
            assertEquals("incorrect relative path", "ef/gh", p2.attributeValue("repo-path"));
        }
        {
            final TestTool tool1 = new TestTool();
            tool1.basedir = testbase;
            tool1.packaging = "pom";
            tool1.destfile = "-";
            tool1.validate();
            tool1.process();
            Document dom = tool1.generateData();
            assertNotNull(dom);
            assertNotNull(dom.selectSingleNode("/packages"));
            assertNotNull(dom.selectSingleNode("/packages/@basedir"));
            assertEquals("incorrect basedir",
                         testbase.getAbsolutePath(),
                         dom.selectSingleNode("/packages/@basedir").getStringValue());

            assertNotNull(dom.selectSingleNode("/packages/package"));
            assertEquals("incorrect size", 1, dom.selectNodes("/packages/package").size());
            assertNull(dom.selectSingleNode("/packages/package[@name='my_APP-0.1.2.rpm']"));
            assertNull(dom.selectSingleNode("/packages/package[@name='ab-testda-SNAPSHOT.rpm']"));
            assertNotNull(dom.selectSingleNode("/packages/package[@name='3p-stb-parent-aggregator-1.0-SNAPSHOT.pom']"));
            Element p1 = (Element) dom.selectSingleNode(
                "/packages/package[@name='3p-stb-parent-aggregator-1.0-SNAPSHOT.pom']");
            assertEquals("incorrect relative path",
                         "3p-stb-parent-aggregator-1.0-SNAPSHOT.pom",
                         p1.attributeValue("name"));
            assertEquals("incorrect relative path", "b/pom.xml", p1.attributeValue("pom-path"));
            assertEquals("incorrect relative path", "ab/cd/parents", p1.attributeValue("repo-path"));

        }
    }
}