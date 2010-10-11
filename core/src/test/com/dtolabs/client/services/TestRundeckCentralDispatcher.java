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

package com.dtolabs.client.services;
/*
* TestRundeckCentralDispatcher.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 31, 2010 11:18:40 AM
* $Id$
*/

import com.dtolabs.rundeck.core.utils.NodeSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;

public class TestRundeckCentralDispatcher extends TestCase {
    RundeckCentralDispatcher rundeckCentralDispatcher;


    public TestRundeckCentralDispatcher(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestRundeckCentralDispatcher.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testAddNodeSetParams() throws Exception {
        // 19 parameters: 16 exclude/include params + nodexcludeprecedence + threadcount + donodedispatch
        final int PARAM_COUNT = 19;
        {
            //test null nodeset
            HashMap<String, String> params = new HashMap<String, String>();
            RundeckCentralDispatcher.addNodeSetParams(params, null, null, "test.");
            assertEquals(0, params.size());
        }
        {
            //test empty nodeset, only has threadcount & keepgoing
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            RundeckCentralDispatcher.addNodeSetParams(params, nodeset, false, "test.");
            assertEquals(2, params.size());
            assertFalse(params.containsKey("test.doNodedispatch"));
            assertTrue(params.containsKey("test.nodeThreadcount"));
            assertEquals("1", params.get("test.nodeThreadcount"));
            assertTrue(params.containsKey("test.nodeKeepgoing"));
            assertEquals("false", params.get("test.nodeKeepgoing"));

        }
        {
            //test empty nodeset, only has threadcount & keepgoing
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            nodeset.setThreadCount(2);
            RundeckCentralDispatcher.addNodeSetParams(params, nodeset, true, "test.");
            assertEquals(2, params.size());
            assertFalse(params.containsKey("test.doNodedispatch"));
            assertTrue(params.containsKey("test.nodeThreadcount"));
            assertEquals("2", params.get("test.nodeThreadcount"));
            assertTrue(params.containsKey("test.nodeKeepgoing"));
            assertEquals("true", params.get("test.nodeKeepgoing"));

        }
        {
            //test basic hostname
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            nodeset.setThreadCount(1);
            final NodeSet.Include include = nodeset.createInclude();
            include.setHostname("testhostname1");
            RundeckCentralDispatcher.addNodeSetParams(params, nodeset, false, "test.");
            assertEquals("incorrect size: " + params, PARAM_COUNT + 1 /* keepgoing==false */, params.size());
            assertTrue(params.containsKey("test.doNodedispatch"));
            assertEquals("true", params.get("test.doNodedispatch"));
            assertTrue(params.containsKey("test.nodeThreadcount"));
            assertEquals("1", params.get("test.nodeThreadcount"));
            assertTrue(params.containsKey("test.nodeKeepgoing"));
            assertEquals("false", params.get("test.nodeKeepgoing"));
            assertTrue(params.containsKey("test.nodeInclude"));
            assertEquals("testhostname1", params.get("test.nodeInclude"));
        }
        {
            //test threadcount/keepgoing changes
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Include include = nodeset.createInclude();
            include.setHostname("testhostname1");
            nodeset.setThreadCount(2);
            RundeckCentralDispatcher.addNodeSetParams(params, nodeset, true, "test.");
            assertEquals("incorrect size: " + params, PARAM_COUNT + 1 /* keepgoing==true */, params.size());
            assertTrue(params.containsKey("test.nodeThreadcount"));
            assertEquals("2", params.get("test.nodeThreadcount"));
            assertTrue(params.containsKey("test.nodeKeepgoing"));
            assertEquals("true", params.get("test.nodeKeepgoing"));
        }
        {
            //test include filters
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Include include = nodeset.createInclude();
            include.setHostname("testhostname1");
            include.setOsfamily("testosfamily");
            include.setOsname("testosname");
            include.setOsversion("test1234");
            include.setOsarch("testosarch");
            include.setName("testname");
            include.setTags("testtags");
            include.setType("testtype");

            //test other include filters
            RundeckCentralDispatcher.addNodeSetParams(params, nodeset, null, "test.");

            assertEquals("incorrect size: " + params, PARAM_COUNT, params.size());
            assertTrue(params.containsKey("test.doNodedispatch"));
            assertEquals("true", params.get("test.doNodedispatch"));
            assertEquals("testhostname1", params.get("test.nodeInclude"));
            assertEquals("testosfamily", params.get("test.nodeIncludeOsFamily"));
            assertEquals("testosname", params.get("test.nodeIncludeOsName"));
            assertEquals("test1234", params.get("test.nodeIncludeOsVersion"));
            assertEquals("testosarch", params.get("test.nodeIncludeOsArch"));
            assertEquals("testname", params.get("test.nodeIncludeName"));
            assertEquals("testtags", params.get("test.nodeIncludeTags"));
            assertEquals("testtype", params.get("test.nodeIncludeType"));
        }
        {
            //test exclude filters
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Exclude exclude = nodeset.createExclude();
            exclude.setHostname("testhostname1");
            exclude.setOsfamily("testosfamily");
            exclude.setOsname("testosname");
            exclude.setOsversion("test1234");
            exclude.setOsarch("testosarch");
            exclude.setName("testname");
            exclude.setTags("testtags");
            exclude.setType("testtype");

            //test other include filters
            RundeckCentralDispatcher.addNodeSetParams(params, nodeset, null, "test.");
            assertEquals("incorrect size: " + params, PARAM_COUNT, params.size());
            assertTrue(params.containsKey("test.doNodedispatch"));
            assertEquals("true", params.get("test.doNodedispatch"));
            assertEquals("testhostname1", params.get("test.nodeExclude"));
            assertEquals("testosfamily", params.get("test.nodeExcludeOsFamily"));
            assertEquals("testosname", params.get("test.nodeExcludeOsName"));
            assertEquals("test1234", params.get("test.nodeExcludeOsVersion"));
            assertEquals("testosarch", params.get("test.nodeExcludeOsArch"));
            assertEquals("testname", params.get("test.nodeExcludeName"));
            assertEquals("testtags", params.get("test.nodeExcludeTags"));
            assertEquals("testtype", params.get("test.nodeExcludeType"));
        }
        {
            //test precedence filters
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Exclude exclude = nodeset.createExclude();
            final NodeSet.Include include = nodeset.createInclude();
            exclude.setHostname("testhostname1");
            include.setHostname("testhostname2");
            exclude.setDominant(true);


            //test other include filters
            RundeckCentralDispatcher.addNodeSetParams(params, nodeset, null, "test.");
            assertEquals("incorrect size: " + params, PARAM_COUNT, params.size());
            assertTrue(params.containsKey("test.doNodedispatch"));
            assertEquals("true", params.get("test.doNodedispatch"));
            assertEquals("testhostname1", params.get("test.nodeExclude"));
            assertEquals("testhostname2", params.get("test.nodeInclude"));
            assertEquals("true", params.get("test.nodeExcludePrecedence"));
        }
        {
            //test precedence filters
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Exclude exclude = nodeset.createExclude();
            final NodeSet.Include include = nodeset.createInclude();
            exclude.setHostname("testhostname1");
            include.setHostname("testhostname2");
            include.setDominant(true);


            //test other include filters
            RundeckCentralDispatcher.addNodeSetParams(params, nodeset, null, "test.");
            assertEquals("incorrect size: " + params, PARAM_COUNT, params.size());
            assertTrue(params.containsKey("test.doNodedispatch"));
            assertEquals("true", params.get("test.doNodedispatch"));
            assertEquals("testhostname1", params.get("test.nodeExclude"));
            assertEquals("testhostname2", params.get("test.nodeInclude"));
            assertEquals("false", params.get("test.nodeExcludePrecedence"));
        }
        {
            //test precedence filters
            HashMap<String, String> params = new HashMap<String, String>();
            NodeSet nodeset = new NodeSet();
            final NodeSet.Exclude exclude = nodeset.createExclude();
            final NodeSet.Include include = nodeset.createInclude();
            exclude.setHostname("testhostname1");
            include.setHostname("testhostname2");
            exclude.setDominant(true);
            include.setDominant(true);


            //test other include filters
            RundeckCentralDispatcher.addNodeSetParams(params, nodeset, null, "test.");
            assertEquals("incorrect size: " + params, PARAM_COUNT, params.size());
            assertTrue(params.containsKey("test.doNodedispatch"));
            assertEquals("true", params.get("test.doNodedispatch"));
            assertEquals("testhostname1", params.get("test.nodeExclude"));
            assertEquals("testhostname2", params.get("test.nodeInclude"));
            assertEquals("true", params.get("test.nodeExcludePrecedence"));
        }
    }
}